package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.network.ErasedAccounts
import com.artiuillab.tieryourlife.core.settings.AccountErasedNotice
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Another phone deleted the account: this one signs out of it, keeps its boards, and says why. */
@Singleton
class ErasedElsewhere @Inject constructor(
    private val erased: ErasedAccounts,
    private val merge: RoomBoardMerge,
    private val notice: AccountErasedNotice,
) {

    suspend fun watch() {
        erased.uids.collect { uid -> runSuspendCatching { leave(uid) }.onFailure { Timber.w(it, "Could not leave an erased account") } }
    }

    internal suspend fun leave(uid: String) {
        if (merge.leaveErased(uid)) {
            Timber.i("The account was erased elsewhere; signed out, boards kept here")
            notice.raise()
        }
    }
}
