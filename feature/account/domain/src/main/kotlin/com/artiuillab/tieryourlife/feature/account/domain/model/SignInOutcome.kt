package com.artiuillab.tieryourlife.feature.account.domain.model

sealed interface SignInOutcome {

    data object Success : SignInOutcome

    data object Cancelled : SignInOutcome

    data object NoGoogleAccount : SignInOutcome

    /** [guestCreditsLost]: a guest had credits, and they stayed with the guest. */
    data class SignedInToExistingAccount(val guestCreditsLost: Boolean) : SignInOutcome

    data object Failed : SignInOutcome
}
