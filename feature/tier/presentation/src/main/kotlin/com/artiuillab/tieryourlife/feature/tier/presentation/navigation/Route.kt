package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object TierLists : Route

    @Serializable
    data class TierDetail(val tierListId: Long, val startInTitleEdit: Boolean = false) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Trash : Route
}