package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.network.ErasedAccounts
import com.artiuillab.tieryourlife.core.settings.AccountErasedNotice
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Another phone deleted the account: this one signs out of it, keeps its boards, and says why. */
@Singleton
class ErasedElsewhere @Inject constructor(
    private val erased: ErasedAccounts,
    private val merge: RoomBoardMerge,
    private val notice: AccountErasedNotice,
    private val preferences: AppPreferences,
) {

    suspend fun watch() {
        resume()
        erased.uids.collect { uid -> leaveOrSay(uid) }
    }

    /** A leave the process did not live to finish. */
    internal suspend fun resume() {
        preferences.erasedAccountToLeave()?.let { leaveOrSay(it) }
    }

    private suspend fun leaveOrSay(uid: String) {
        runSuspendCatching { leave(uid) }.onFailure { Timber.w(it, "Could not leave an erased account") }
    }

    internal suspend fun leave(uid: String) {
        preferences.setErasedAccountToLeave(uid)
        merge.leaveErased(uid) { fromHere ->
            Timber.i("The account was erased; signing out, boards kept here")
            notice.raise(if (fromHere) AccountErasedNotice.Where.ThisDevice else AccountErasedNotice.Where.AnotherDevice)
        }
        preferences.setErasedAccountToLeave(null)
    }
}
