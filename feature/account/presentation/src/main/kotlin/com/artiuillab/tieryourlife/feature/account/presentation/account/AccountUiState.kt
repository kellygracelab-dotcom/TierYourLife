package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice

data class AccountUiState(
    val account: Account = Account.Unknown,
    val signingIn: Boolean = false,
    val savingName: Boolean = false,
    /** Generations left, or null where nothing is counted. */
    val credits: Int? = null,
    val publicListCount: Int = 0,
    /** Boards on this phone, so the offer can say what it would be keeping. */
    val boardCount: Int = 0,
    val backUpBoards: Boolean = true,
    /** Card pictures the reader can wear as a face. */
    val faceChoices: List<String> = emptyList(),
    val googlePhotoUrl: String? = null,
    val notice: AccountNotice? = null,
    /**
     * Set only while the question is open: somebody has signed into an account
     * that already holds boards, and nothing has been written either way yet.
     */
    val merge: MergeChoice? = null,
    val mergeKeep: MergeKeep = MergeKeep.Everything,
)

/** Which set of boards is in use afterwards. Neither answer deletes anything. */
enum class MergeKeep { Everything, AccountOnly }

enum class AccountNotice {
    SignInFailed,
    NoGoogleAccount,
    SignInUnavailable,
    SignedInToExistingAccount,
    NameNotSaved,
}
