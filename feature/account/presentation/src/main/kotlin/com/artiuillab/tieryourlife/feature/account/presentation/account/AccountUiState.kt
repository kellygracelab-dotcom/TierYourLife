package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.feature.account.domain.model.Account

data class AccountUiState(
    val account: Account = Account.Guest,
    val signingIn: Boolean = false,
    /** Generations left, or null where nothing is counted. */
    val credits: Int? = null,
    val notice: AccountNotice? = null,
)

/** Something worth saying out loud. A cancelled picker is not on this list. */
enum class AccountNotice {
    SignInFailed,
    NoGoogleAccount,
    SignInUnavailable,
    SignedInToExistingAccount,
}
