package com.artiuillab.tieryourlife.feature.account.domain.model

sealed interface Account {

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
