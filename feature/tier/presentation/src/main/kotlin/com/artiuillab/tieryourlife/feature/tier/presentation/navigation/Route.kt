package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.Serializable

sealed interface Route {
    /** [community] rides in the route: the rail navigates here to switch tabs, and a rail that could not say which would work from one of them only. */
    @Serializable
    /** [makeBoard] rides in the route so exactly one screen sees it; a counter above the graph was read by the screen being left as well. */
    data class TierLists(val community: Boolean = false, val makeBoard: Boolean = false) : Route

    @Serializable
    data class TierDetail(val tierListId: Long) : Route

    @Serializable
    data object Trash : Route
}

/**
 * The route read straight from the handle, by property name, as Navigation
 * stores it. toRoute() would do the same through Android's Bundle, which
 * keeps a view model off the JVM. RouteFromHandleTest holds the two equal.
 */
internal fun SavedStateHandle.tierDetailRoute() = Route.TierDetail(tierListId = required(Route.TierDetail::tierListId.name))

private fun <T : Any> SavedStateHandle.required(key: String): T =
    checkNotNull(get<T>(key)) { "Route argument $key is missing" }
