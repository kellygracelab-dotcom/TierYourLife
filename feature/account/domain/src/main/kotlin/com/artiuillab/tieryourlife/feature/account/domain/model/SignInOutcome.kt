package com.artiuillab.tieryourlife.feature.account.domain.model

sealed interface SignInOutcome {

    data object Success : SignInOutcome

    data object Cancelled : SignInOutcome

    data object NoGoogleAccount : SignInOutcome

    data class SignedInToExistingAccount(val creditsCarriedOver: Boolean) : SignInOutcome

    data object Failed : SignInOutcome
}
