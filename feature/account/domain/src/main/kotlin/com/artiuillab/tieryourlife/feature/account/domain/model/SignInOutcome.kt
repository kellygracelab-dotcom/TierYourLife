package com.artiuillab.tieryourlife.feature.account.domain.model

sealed interface SignInOutcome {

    data object Success : SignInOutcome

    /** The person closed the picker. Not a failure, and not worth a message. */
    data object Cancelled : SignInOutcome

    /** No Google account on the device, so there is nothing to choose from. */
    data object NoGoogleAccount : SignInOutcome

    /**
     * The chosen Google account already belongs to another identity, and the
     * guest credits could not be carried over to it.
     */
    data class SignedInToExistingAccount(val creditsCarriedOver: Boolean) : SignInOutcome

    data object Failed : SignInOutcome
}
