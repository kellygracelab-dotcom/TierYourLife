package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    /** [community] rides in the route: the rail navigates here to switch tabs, and a rail that could not say which would work from one of them only. */
    @Serializable
    /** [makeBoard] rides in the route so exactly one screen sees it; a counter above the graph was read by the screen being left as well. */
    data class TierLists(val community: Boolean = false, val makeBoard: Boolean = false) : Route

    @Serializable
    data class TierDetail(val tierListId: Long) : Route

    @Serializable
    data class CommunityList(val publishedId: String) : Route

    /** Name and face travel so the header is right before the lists arrive. */
    @Serializable
    data class Author(
        val authorUid: String,
        val authorName: String,
        val authorPhotoUrl: String? = null,
    ) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Trash : Route

    @Serializable
    data object Hidden : Route

    @Serializable
    data object Moderation : Route

    @Serializable
    data object MyPublished : Route
}
