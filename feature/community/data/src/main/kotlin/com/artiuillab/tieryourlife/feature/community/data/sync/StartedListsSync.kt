package com.artiuillab.tieryourlife.feature.community.data.sync

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.data.cache.DraftRecord
import com.artiuillab.tieryourlife.feature.community.data.cache.FileRankingDrafts
import com.artiuillab.tieryourlife.feature.community.data.remote.KeepStartedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.KeptStartedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedApi
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedConflictDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedRemoval
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedSync
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedSyncOutcome
import com.artiuillab.tieryourlife.feature.community.domain.repository.startedGameKey
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import com.artiuillab.tieryourlife.feature.play.domain.SharedGameJson
import com.artiuillab.tieryourlife.feature.play.domain.isExpired
import com.artiuillab.tieryourlife.feature.play.domain.local
import com.artiuillab.tieryourlife.feature.play.domain.shared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this device's lists begun and the account's in step, by the boards function's started routes.
 * Each round sends what changed here (a list emptied, saved or finished goes as its removal), then takes
 * what changed elsewhere since the last round. Two devices changing one list: the later change wins, and
 * the device whose copy gave way says so. Offline, what changed here waits, marked, for the next round.
 */
@Singleton
class StartedListsSync internal constructor(
    private val api: StartedApi,
    private val drafts: FileRankingDrafts,
    private val games: SavedGames,
    private val accounts: AccountRepository,
    triggers: Flow<Unit>,
    scope: CoroutineScope,
) : StartedSync {

    @Inject
    constructor(
        api: StartedApi,
        drafts: FileRankingDrafts,
        games: SavedGames,
        accounts: AccountRepository,
        network: CommunityNetwork,
    ) : this(
        api,
        drafts,
        games,
        accounts,
        merge(network.available, accounts.account.filterIsInstance<Account.SignedIn>().map { }),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private val mutex = Mutex()

    // Kept until taken: a request made before the rounds are listened for still makes one.
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val outcomes = MutableSharedFlow<StartedSyncOutcome>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val synced: Flow<StartedSyncOutcome> = outcomes

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            merge(requests.receiveAsFlow(), drafts.changes, triggers).debounce(QUIET_MILLIS).collect {
                runSuspendCatching { sync() }.onFailure { Timber.w(it, "Syncing the lists begun failed") }
            }
        }
    }

    override fun request() {
        requests.trySend(Unit)
    }

    override suspend fun sync(): StartedSyncOutcome = mutex.withLock {
        val owner = (accounts.current() as? Account.SignedIn)?.recordKey ?: return@withLock StartedSyncOutcome()
        val round = Round()
        try {
            adoptGuests(owner, round)
            for (record in drafts.records(owner)) push(owner, record, round)
            pull(owner, round)
        } catch (e: IOException) {
            Timber.i(e, "Lists begun wait for the network")
        } catch (e: HttpException) {
            // A guest's 403, or a server without the routes yet: what is here stays as it is.
            Timber.w(e, "The account did not take the lists begun")
        }
        round.outcome().also { if (it.changed) outcomes.emit(it) }
    }

    /**
     * A guest's lists join the account's. Sent as new (no version), so the account keeps whichever of the two
     * copies changed later and hands the other back: nothing it holds is overwritten by an older copy.
     */
    private suspend fun adoptGuests(owner: String, round: Round) {
        for (guest in drafts.records(null)) {
            val listId = guest.listId ?: continue
            val mine = drafts.record(owner, listId)
            if (mine == null || guest.lastChange > mine.lastChange) {
                drafts.write(guest.copy(owner = owner, updatedAt = null, dirty = true, removed = null))
                round.changed = true
            }
            drafts.forget(null, listId)
        }
    }

    private suspend fun push(owner: String, record: DraftRecord, round: Round) {
        val listId = record.listId ?: return
        val key = startedGameKey(listId, record.version)
        val game = games.load(key)?.takeUnless { it.isExpired(System.currentTimeMillis()) }
        // A game moves on without the arrangement being kept again: a newer game is a change too.
        val gameAt = game?.touchedAt ?: 0
        if (!record.dirty && (record.removed != null || gameAt <= record.lastChange)) return
        val changedAt = maxOf(record.lastChange, gameAt)
        val reason = removalOf(record)
        if (reason != null) {
            if (record.updatedAt == null) {
                // The account never had it: nothing there to take out.
                drafts.write(record.copy(dirty = false, removed = reason))
                return
            }
            val answer = api.remove(listId, reason, record.updatedAt, changedAt)
            settle(owner, record, answer, round) { kept -> record.copy(updatedAt = kept.updatedAt, dirty = false, removed = reason) }
            return
        }
        val body = KeepStartedDto(
            basedOn = record.updatedAt,
            changedAt = changedAt,
            listVersion = record.version,
            itemCount = record.rows.sumOf { it.size } + record.pool.size,
            tierCount = record.rows.size,
            title = record.title,
            picture = null,
            pictures = emptyList(),
            category = record.section,
            rows = record.rows,
            poolOrder = record.pool,
            play = game?.shared(itemUid = { it.toString() }, tierUid = ::tierUid, now = System.currentTimeMillis())
                ?.let { networkJson.parseToJsonElement(SharedGameJson.encode(it)) },
        )
        val answer = api.keep(listId, body)
        settle(owner, record, answer, round) { kept -> record.copy(updatedAt = kept.updatedAt, changedAt = changedAt, dirty = false, removed = null) }
    }

    /** A write taken, or refused with what the account holds, which then takes this device's place. */
    private suspend fun settle(owner: String, record: DraftRecord, answer: Response<KeptStartedDto>, round: Round, taken: (KeptStartedDto) -> DraftRecord) {
        val kept = answer.body()
        when {
            answer.isSuccessful && kept != null -> {
                val now = drafts.record(owner, checkNotNull(record.listId))
                // Changed again while this went: the newer change stays to go next, from the version just made.
                drafts.write(if (now == null || now == record) taken(kept) else now.copy(updatedAt = kept.updatedAt))
                round.see(kept.updatedAt)
            }
            answer.code() == CONFLICT -> {
                val theirs = answer.errorBody()?.string()
                    ?.let { runCatching { networkJson.decodeFromString<StartedConflictDto>(it) }.getOrNull() }
                    ?.started
                if (theirs != null) {
                    take(owner, record, theirs)
                    round.changed = true
                    round.replaced++
                    round.see(theirs.updatedAt)
                }
            }
            // Refused for its size or its fields: sending it again would be refused again.
            answer.code() == TOO_LARGE || answer.code() == INVALID -> {
                Timber.w("The account refused a list begun: %d", answer.code())
                drafts.write(record.copy(dirty = false))
            }
            else -> throw HttpException(answer)
        }
    }

    private suspend fun pull(owner: String, round: Round) {
        val since = drafts.cursor(owner)
        val index = api.index(since)
        for (theirs in index.started + index.removed) {
            round.see(theirs.updatedAt)
            val mine = drafts.record(owner, theirs.listId)
            if (!arrives(mine, theirs)) continue
            take(owner, mine, theirs)
            round.changed = true
            if (replaces(mine, theirs)) round.replaced++
        }
        round.seen?.let { drafts.setCursor(owner, maxOf(it, since ?: 0)) }
    }

    /** The account's copy in this device's place: the arrangement and the game, or the removal of both. */
    private suspend fun take(owner: String, mine: DraftRecord?, theirs: StartedListDto) {
        val listId = theirs.listId
        val removed = theirs.removed
        if (removed != null) {
            mine ?: return
            val emptied = if (removed == StartedRemoval.DISCARD) mine.copy(rows = mine.rows.map { emptyList() }, pool = mine.rows.flatten() + mine.pool) else mine
            drafts.write(emptied.copy(updatedAt = theirs.updatedAt, changedAt = theirs.changedAt, dirty = false, removed = removed))
            if (removed == StartedRemoval.DISCARD) games.discard(startedGameKey(listId, mine.version))
            return
        }
        val placed = theirs.rows.flatten().toSet()
        drafts.write(
            DraftRecord(
                version = theirs.listVersion,
                rows = theirs.rows,
                pool = theirs.poolOrder ?: (0 until theirs.itemCount).filterNot { it in placed },
                choice = mine?.choice,
                listId = listId,
                owner = owner,
                title = theirs.title,
                keptAt = System.currentTimeMillis(),
                section = theirs.category,
                updatedAt = theirs.updatedAt,
                changedAt = theirs.changedAt,
                dirty = false,
            ),
        )
        val key = startedGameKey(listId, theirs.listVersion)
        val game = theirs.play?.let { SharedGameJson.decode(it.toString()) }
        if (game == null) {
            games.discard(key)
        } else {
            val here = game.local(itemId = { it.toLongOrNull() }, tierId = { if (it == POOL) POOL_TIER else it.toLongOrNull() })
            // Kept in the file, as a game of someone else's list always is, and touched when the account last was.
            games.save(key, here.copy(onBoard = false, touchedAt = maxOf(here.startedAt, theirs.changedAt)))
        }
    }

    private class Round {
        var changed = false
        var replaced = 0
        var seen: Long? = null

        fun see(updatedAt: Long) {
            seen = maxOf(seen ?: updatedAt, updatedAt)
        }

        fun outcome() = StartedSyncOutcome(changed = changed, replaced = replaced)
    }

    private companion object {
        const val QUIET_MILLIS = 3_000L
        const val CONFLICT = 409
        const val TOO_LARGE = 413
        const val INVALID = 400
    }
}

/** In a game of someone else's list a card is its index in the list and a tier its index, the deck -1. */
private const val POOL_TIER = -1L
private const val POOL = "pool"

private fun tierUid(tierId: Long): String = if (tierId == POOL_TIER) POOL else tierId.toString()

/** Saved, emptied or finished: it leaves Continue on every device, and the account keeps why. */
private fun removalOf(record: DraftRecord): String? {
    val draft = record.draft()
    return when {
        draft.savedBoardUid != null -> StartedRemoval.SAVED
        draft.placed == 0 -> StartedRemoval.DISCARD
        draft.placed == draft.total -> StartedRemoval.DONE
        else -> null
    }
}

/** The account's copy is newer than this device's, and this device has no later change of its own waiting. */
private fun arrives(mine: DraftRecord?, theirs: StartedListDto): Boolean = when {
    mine == null -> theirs.removed == null
    (mine.updatedAt ?: Long.MIN_VALUE) >= theirs.updatedAt -> false
    else -> !mine.dirty || mine.lastChange < theirs.changedAt
}

/** A list taken out elsewhere is gone, not replaced: only a later arrangement over one shown here is told. */
private fun replaces(mine: DraftRecord?, theirs: StartedListDto): Boolean =
    theirs.removed == null && mine != null && mine.removed == null && (mine.dirty || !sameArrangement(mine, theirs))

private fun sameArrangement(mine: DraftRecord, theirs: StartedListDto): Boolean =
    mine.version == theirs.listVersion && mine.rows == theirs.rows && (theirs.poolOrder == null || mine.pool == theirs.poolOrder)
