package com.artiuillab.tieryourlife.feature.account.domain.repository

/**
 * Moving a guest's balance onto the account just signed into. Only needed on
 * one path: linking Google onto a guest keeps the uid, but an existing Google
 * account cannot be linked to, and Firebase leaves the guest uid behind
 * holding a balance nobody can reach.
 */
interface GuestCredits {

    /** [guestIdToken] must be taken before signing in; afterwards nothing can prove the guest was this person. */
    suspend fun carryOver(guestIdToken: String): Boolean
}
