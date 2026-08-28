package com.artiuillab.tieryourlife.feature.account.domain.model

/**
 * Who the app is acting as. There is always an identity — generation is metered
 * against one — but only a signed-in identity survives losing the device.
 */
sealed interface Account {

    /**
     * Anonymous, or not signed in at all. The distinction is invisible to the
     * user and deliberately so: nothing was ever asked of them.
     */
    data object Guest : Account

    data class SignedIn(
        val email: String?,
        val photoUrl: String?,
    ) : Account
}
