package com.artiuillab.tieryourlife.feature.account.domain.model

sealed interface Account {

    /**
     * Before Firebase has answered. Distinct from [Guest] on purpose: treating
     * the two as one made every screen paint the sign-in offer for a frame and
     * then replace it.
     */
    data object Unknown : Account

    data object Guest : Account

    data class SignedIn(
        val email: String?,
        val photoUrl: String?,
        val displayName: String? = null,
    ) : Account {

        /** What the community shows as the author. */
        val authorName: String? get() = displayName?.takeIf { it.isNotBlank() }
    }
}
