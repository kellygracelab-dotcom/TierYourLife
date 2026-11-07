package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.OwnerDao
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A session Firebase ended with no sign-out by choice: an expired token, a
 * password changed elsewhere. The boards stay on the phone as its own and the
 * person is forgotten, as after signing out; but each keeps its record with
 * the account, so signing back in finds it beside its copy rather than as a
 * new board to ask about.
 */
@Singleton
class LostSession @Inject constructor(
    private val accounts: AccountRepository,
    private val owners: OwnerDao,
    private val ownership: BoardOwnership,
    private val lock: SyncLock,
    private val preferences: AppPreferences,
    private val thePerson: ThePerson,
) {

    suspend fun watch() {
        accounts.account.collect { notice() }
    }

    /** Reads the account again under the lock: one seen before a sign-out by choice took it would be stale. */
    internal suspend fun notice() = lock.withLock {
        val now = accounts.current()
        if (now is Account.Unknown) return@withLock
        val signedIn = (now as? Account.SignedIn)?.recordKey
        val last = preferences.lastSignedIn()
        if (last != null && last != signedIn) lose(last)
        if (signedIn != null) {
            preferences.setLastSignedIn(signedIn)
            ownership.reclaim(signedIn)
        }
    }

    private suspend fun lose(account: String) {
        if (erasureMarkOf(preferences.erasureUnderWay())?.account == account) {
            // As settling the erase at the next start would: the account going away is what ended it.
            ownership.reclaim(account)
            ownership.forgetCopiesInto(account)
            val kept = owners.keepBoardsOfDeleted(account)
            preferences.setErasureUnderWay(null)
            Timber.i("Signed out with an erase unanswered; kept %d board(s) of the account", kept)
        } else if (!ownership.holdFor(account)) {
            // Nothing let go: the boards stay the account's, and the next look tries again.
            Timber.w("Could not write down whose the boards were; holding them waits")
            thePerson.forget()
            return
        }
        preferences.setLastSignedIn(null)
        thePerson.forget()
        ownership.announceMove()
    }
}
