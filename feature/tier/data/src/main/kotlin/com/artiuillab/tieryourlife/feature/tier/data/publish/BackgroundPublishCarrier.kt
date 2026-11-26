package com.artiuillab.tieryourlife.feature.tier.data.publish

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.repository.Carried
import com.artiuillab.tieryourlife.feature.community.domain.repository.CarryRequest
import com.artiuillab.tieryourlife.feature.community.domain.repository.Carrying
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.PublishCarrier
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.community.domain.repository.RecordPublished
import com.artiuillab.tieryourlife.feature.tier.data.sync.Connection
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PublishFingerprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publish and Update, carried in the app's own scope rather than a screen's:
 * at once, while the board stays open and editable and after it is left. A
 * lost connection is tried again after 2, 5 and 10 seconds, then whenever one
 * comes back. [CarryBackup] holds each one until it ends, so a phone that
 * closes the app finishes it the next time the system runs us.
 */
@Singleton
class BackgroundPublishCarrier internal constructor(
    private val boards: TierRepository,
    private val community: CommunityRepository,
    private val accounts: AccountRepository,
    private val preferences: AppPreferences,
    private val pictures: OwnPictures,
    private val connection: Connection,
    private val backup: CarryBackup,
    private val scope: CoroutineScope,
    private val pauses: Pauses,
) : PublishCarrier {

    @Inject constructor(
        boards: TierRepository,
        community: CommunityRepository,
        accounts: AccountRepository,
        preferences: AppPreferences,
        pictures: OwnPictures,
        connection: Connection,
        backup: CarryBackup,
    ) : this(boards, community, accounts, preferences, pictures, connection, backup, CoroutineScope(SupervisorJob() + Dispatchers.IO), Pauses())

    /** How long it waits; shorter in tests. */
    data class Pauses(
        val retries: List<Long> = RETRIES_MS,
        val afterReconnect: Long = AFTER_RECONNECT_MS,
        val slow: Long = SLOW_MS,
    )

    private val mutable = MutableStateFlow(emptyMap<Long, Carrying>())
    override val going: StateFlow<Map<Long, Carrying>> = mutable.asStateFlow()
    private val events = MutableSharedFlow<Carried>(extraBufferCapacity = EVENTS)
    override val ended: SharedFlow<Carried> = events.asSharedFlow()

    private class Trip(val request: CarryRequest) {
        lateinit var job: Job

        /** The request has left: stopping now waits for its answer rather than cutting it off. */
        @Volatile var onTheWire = false
    }

    /** Guarded by itself: a Take it down a moment after the tap must find the trip already there. */
    private val trips = mutableMapOf<Long, Trip>()

    private val record = RecordPublished(boards, community) { id, waiting ->
        val ids = preferences.awaitingApproval()
        preferences.setAwaitingApproval(if (waiting) ids + id else ids - id)
    }

    override fun send(request: CarryRequest) {
        synchronized(trips) {
            if (request.board.id !in trips) start(request)
        }
    }

    private fun start(request: CarryRequest): Trip {
        val id = request.board.id
        val trip = Trip(request)
        trips[id] = trip
        mutable.update { it + (id to Carrying(update = request.board.publishedId != null)) }
        // Atomic: one stopped before it began still clears what it set.
        trip.job = scope.launch(start = CoroutineStart.ATOMIC) { carry(trip) }
        return trip
    }

    /**
     * From [CarryBackup], when the system runs us again. Here, the trip still
     * on its way is waited for. In a new process the board as the tap found it
     * is gone with the old one: the board on this phone goes instead, but only
     * if it has not changed since, for a later edit is never published by itself.
     */
    suspend fun resume(note: BackupNote) {
        synchronized(trips) { trips[note.boardId] }?.let {
            it.job.join()
            return
        }
        val board = boards.getTierListById(note.boardId)
        if (board == null || fingerprintOf(board) != note.fingerprint || !signedInAs(note.owner)) {
            Timber.i("Not resuming the publish of board %d: changed or gone since", note.boardId)
            backup.drop(note.boardId)
            return
        }
        val trip = synchronized(trips) { trips[note.boardId] ?: start(CarryRequest(board, note.owner, note.originVersion, note.categoryIfNone)) }
        trip.job.join()
    }

    private suspend fun carry(trip: Trip) {
        val request = trip.request
        val id = request.board.id
        val slow = scope.launch {
            delay(pauses.slow)
            mutable.update { going -> going[id]?.let { going + (id to it.copy(slow = true)) } ?: going }
        }
        var ending: Carried? = null
        try {
            backup.keep(BackupNote(id, request.owner, fingerprintOf(request.board), request.originVersion, request.categoryIfNone))
            ending = attempts(trip)
        } finally {
            slow.cancel()
            synchronized(trips) { trips.remove(id) }
            backup.drop(id)
            // Said before it leaves [going]: a screen sees one or the other, never neither.
            ending?.let { events.tryEmit(it) }
            mutable.update { it - id }
        }
    }

    private suspend fun attempts(trip: Trip): Carried? {
        val request = trip.request
        var tried = 0
        var ending: Carried? = null
        while (ending == null && signedInAs(request.owner)) {
            trip.onTheWire = true
            val answer = community.publishOwned(request.sending, request.owner)
            trip.onTheWire = false
            ending = endingOf(request, answer)
            if (ending == null) waitToTryAgain(request.board.id, tried++)
        }
        return ending
    }

    /** Null for a lost connection, which is tried again. */
    private suspend fun endingOf(request: CarryRequest, answer: Result<Published>): Carried? {
        val id = request.board.id
        answer.getOrNull()?.let { published ->
            runSuspendCatching { record(request.sending, published, request.originVersion) }
                .onFailure { Timber.w(it, "Recording a carried publish failed") }
            return Carried.Out(id, request.sending, published)
        }
        val reason = (answer.exceptionOrNull() as? PublishRefused)?.error ?: PublishError.Unknown
        return if (reason == PublishError.Offline) null else Carried.Refused(id, request.board.publishedId != null, reason)
    }

    private suspend fun waitToTryAgain(id: Long, tried: Int) {
        val pause = pauses.retries.getOrNull(tried)
        if (pause != null) {
            delay(pause)
        } else {
            mutable.update { going -> going[id]?.let { going + (id to it.copy(offline = true)) } ?: going }
            connection.available.first()
            delay(pauses.afterReconnect)
        }
    }

    override suspend fun stop(boardId: Long) {
        val trip = synchronized(trips) { trips[boardId] } ?: return
        if (trip.onTheWire) {
            trip.job.join()
        } else {
            trip.job.cancel()
            trip.job.join()
        }
    }

    private fun signedInAs(owner: String): Boolean = (accounts.current() as? Account.SignedIn)?.recordKey == owner

    private fun fingerprintOf(board: TierList): String = PublishFingerprint.of(board, pictures::pictureIdOf)

    private companion object {
        const val EVENTS = 8
        val RETRIES_MS = listOf(2_000L, 5_000L, 10_000L)
        const val AFTER_RECONNECT_MS = 10_000L
        const val SLOW_MS = 60_000L
    }
}

/** What [CarryBackup] keeps of a trip: enough to finish it from the board on this phone, if that has not changed since. */
data class BackupNote(
    val boardId: Long,
    val owner: String,
    val fingerprint: String,
    val originVersion: Long?,
    val categoryIfNone: String?,
)

/** Keeps a trip until it ends, so the system finishes it if the app is closed meanwhile. */
interface CarryBackup {
    fun keep(note: BackupNote)
    fun drop(boardId: Long)
}
