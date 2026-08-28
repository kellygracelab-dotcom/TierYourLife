package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountUiState

internal val previewAccountGuestState = AccountUiState()

internal val previewAccountSigningInState = AccountUiState(signingIn = true)

internal val previewAccountSignedInState = AccountUiState(
    account = Account.SignedIn(email = "danylo@example.com", photoUrl = null),
    credits = 12,
)
