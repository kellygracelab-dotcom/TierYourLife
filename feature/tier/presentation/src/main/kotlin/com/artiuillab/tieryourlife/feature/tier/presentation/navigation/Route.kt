package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object TierLists : Route

    @Serializable
    data class TierDetail(val tierListId: Long) : Route

    @Serializable
    data object MovieSearch : Route
}