package com.artiuillab.tieryourlife.feature.account.domain.model

sealed interface Account {

    data object Guest : Account

    data class SignedIn(
        val email: String?,
        val photoUrl: String?,
    ) : Account
}
