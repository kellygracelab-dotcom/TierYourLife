package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.feature.account.domain.model.Account

data class AccountUiState(
    val account: Account = Account.Unknown,
    val signingIn: Boolean = false,
    val savingName: Boolean = false,
    /** Generations left, or null where nothing is counted. */
    val credits: Int? = null,
    val publicListCount: Int = 0,
    val notice: AccountNotice? = null,
)

enum class AccountNotice {
    SignInFailed,
    NoGoogleAccount,
    SignInUnavailable,
    SignedInToExistingAccount,
    NameNotSaved,
}
