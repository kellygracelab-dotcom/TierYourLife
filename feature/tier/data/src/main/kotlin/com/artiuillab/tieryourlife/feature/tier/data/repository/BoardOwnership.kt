package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.GuestBoardsDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.OwnerDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.domain.repository.BoardOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardOwnership @Inject constructor(
    private val accounts: AccountRepository,
    private val dao: OwnerDao,
    private val guests: GuestBoardsDao,
    private val preferences: AppPreferences,
) : BoardOwner {

    private val moved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val settling = Mutex()
    private val heldChanges = Mutex()

    @Volatile
    private var settled = false

    /**
     * True from just before a sign-in until the question about this phone's
     * own boards is answered. No sync runs meanwhile: what goes up for the
     * account depends on the answer.
     */
    @Volatile
    var holding: Boolean = false
        private set

    override val changes: Flow<Unit> = merge(
        accounts.account.map(::ownerOf).distinctUntilChanged().drop(1).map { },
        moved,
    )

    /**
     * Whose boards are read and written now. The first call after the upgrade
     * that gave boards owners waits until each older board has one, so no
     * board shows under the wrong identity, not even for a frame.
     */
    suspend fun current(): String? {
        if (!settled) {
            settling.withLock {
                if (!settled) {
                    settle()
                    settled = true
                }
            }
        }
        return ownerOf(accounts.current())
    }

    fun hold() {
        holding = true
    }

    /** [moveGuestBoardsIn]: the boards made without an account become the signed-in account's. */
    suspend fun release(moveGuestBoardsIn: Boolean) {
        val owner = current()
        if (moveGuestBoardsIn && owner != null) {
            val count = heldChanges.withLock { guests.moveGuestBoardsTo(owner, heldUids()) }
            Timber.i("Moved %d board(s) made without an account into the account", count)
        }
        holding = false
        moved.tryEmit(Unit)
    }

    /** Boards changed hands outside a sign-in. */
    fun announceMove() {
        moved.tryEmit(Unit)
    }

    /** Made without an account, trash left out; not the ones held for an account, which only it takes back. */
    suspend fun guestBoardsInUse(): List<TierListEntity> {
        val held = heldUids()
        return guests.guestBoardsInUse().filterNot { it.uid in held }
    }

    /**
     * [account]'s session ended without a sign-out: its boards become this
     * phone's to show, but stay [account]'s to take back, with their records.
     */
    suspend fun holdFor(account: String): Boolean = heldChanges.withLock {
        val boards = guests.boardsOwnedBy(account)
        if (boards.isEmpty()) return@withLock true
        val held = preferences.heldBoards()
        // Written first: let go with nothing saying whose they were, they would be anyone's.
        if (!preferences.setHeldBoards(held + (account to held[account].orEmpty() + boards))) return@withLock false
        guests.releaseBoardsOf(account)
        Timber.i("The session ended without a sign-out; %d board(s) stay on this phone", boards.size)
        true
    }

    /**
     * Held for an account other than the signed-in one, trash left out, and
     * not copied into the signed-in one before: what the question about them
     * counts, and what a yes copies.
     */
    suspend fun heldForOthersInUse(): List<TierListEntity> {
        val owner = current()
        val held = preferences.heldBoards().filterKeys { it != owner }.values.flatten().toSet()
        val copied = owner?.let { preferences.heldBoardsCopied()[it] }.orEmpty()
        return guests.guestBoardsInUse().filter { it.uid in held && it.uid !in copied }
    }

    /**
     * Before the copies are made: a crash in between leaves them unmade
     * rather than made twice. Originals deleted for good drop out. False when
     * it could not be written, and then no copy is made.
     */
    suspend fun markCopied(into: String, uids: Collection<String>): Boolean = heldChanges.withLock {
        val here = guests.everyBoardUid().toSet()
        val copied = preferences.heldBoardsCopied().mapValues { (_, ofIt) -> ofIt.filterTo(mutableSetOf()) { it in here } }
        preferences.setHeldBoardsCopied(copied + (into to copied[into].orEmpty() + uids))
    }

    /** The copies were not made after all. */
    suspend fun unmarkCopied(into: String, uids: Collection<String>) = heldChanges.withLock {
        val copied = preferences.heldBoardsCopied()
        if (!preferences.setHeldBoardsCopied(copied + (into to copied[into].orEmpty() - uids.toSet()))) {
            Timber.w("Could not take back the mark of copies never made")
        }
    }

    /** [account] is gone: nothing is held back from it any more. */
    suspend fun forgetCopiesInto(account: String) = heldChanges.withLock {
        val copied = preferences.heldBoardsCopied()
        if (account in copied) preferences.setHeldBoardsCopied(copied - account)
    }

    /** The boards held for [account] that are still here are its own again. */
    suspend fun reclaim(account: String) {
        val count = heldChanges.withLock {
            val held = preferences.heldBoards()
            val boards = held[account] ?: return
            val given = guests.giveBack(account, boards)
            preferences.setHeldBoards(held - account)
            given
        }
        if (count > 0) {
            Timber.i("%d board(s) are back with the account they were held for", count)
            moved.tryEmit(Unit)
        }
    }

    private fun heldUids(): Set<String> = preferences.heldBoards().values.flatten().toSet()

    private suspend fun settle() {
        val account = accounts.current().takeUnless { it is Account.Unknown }
            ?: accounts.account.first { it !is Account.Unknown }
        val signedIn = ownerOf(account)
        preferences.settleBackUpChoice(signedIn)

        val unsettled = dao.unsettledBoards()
        if (unsettled.isEmpty()) return
        val records = dao.claimedSyncRecords().groupBy { it.listUid }
        dao.settleOwners(unsettled.associateWith { uid -> legacyOwner(records[uid].orEmpty(), signedIn) })
        Timber.i("Gave %d board(s) from before owners an owner", unsettled.size)
    }

    private fun ownerOf(account: Account): String? = (account as? Account.SignedIn)?.recordKey
}
