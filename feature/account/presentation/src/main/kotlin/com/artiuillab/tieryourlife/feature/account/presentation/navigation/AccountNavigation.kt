package com.artiuillab.tieryourlife.feature.account.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountScreen
import kotlinx.serialization.Serializable

@Serializable
data object AccountRoute

fun NavController.navigateToAccount() {
    navigate(AccountRoute)
}

fun NavGraphBuilder.accountScreen(onClose: () -> Unit, onOpenPublished: () -> Unit) {
    composable<AccountRoute> {
        AccountScreen(onClose = onClose, onOpenPublished = onOpenPublished)
    }
}
