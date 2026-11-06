package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.GuestBoardsDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.OwnerDao
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
            val count = guests.moveGuestBoardsTo(owner)
            Timber.i("Moved %d board(s) made without an account into the account", count)
        }
        holding = false
        moved.tryEmit(Unit)
    }

    /** Boards changed hands outside a sign-in. */
    fun announceMove() {
        moved.tryEmit(Unit)
    }

    /** The boards [account] left behind when its session was ended for it are its own again. */
    suspend fun reclaim(account: String) {
        val count = guests.reclaimHeldBoards(account)
        if (count > 0) {
            Timber.i("%d board(s) are back with the account they were synced with", count)
            moved.tryEmit(Unit)
        }
    }

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
