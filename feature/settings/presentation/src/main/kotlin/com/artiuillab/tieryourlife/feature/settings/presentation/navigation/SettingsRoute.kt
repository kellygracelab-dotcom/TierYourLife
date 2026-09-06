package com.artiuillab.tieryourlife.feature.settings.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface SettingsRoute {
    @Serializable
    data object Settings : SettingsRoute

    @Serializable
    data object Hidden : SettingsRoute
}
