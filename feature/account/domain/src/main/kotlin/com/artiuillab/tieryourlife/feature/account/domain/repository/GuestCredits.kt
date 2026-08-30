package com.artiuillab.tieryourlife.feature.account.domain.repository

/**
 * Moving what is left on a guest identity onto the account someone just signed
 * into.
 *
 * Only ever needed on one path. Linking a Google credential onto a guest keeps
 * the uid, so the credits never move; but a Google account that already exists
 * cannot be linked to, and Firebase signs the person in and leaves the guest
 * uid behind holding a balance nobody can ever reach again.
 */
interface GuestCredits {

    /**
     * [guestIdToken] has to be taken before signing in -- afterwards there is
     * nothing left that can prove the guest was this person. Returns true if
     * the account ended up with more than it had.
     */
    suspend fun carryOver(guestIdToken: String): Boolean
}
