package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    /**
     * [community] is the tab to open on. Carried by the route rather than kept
     * in the screen because the rail on a wide window navigates here to switch
     * tabs, and a rail that could not say which tab would be a rail that only
     * works from one of them.
     */
    @Serializable
    data class TierLists(val community: Boolean = false) : Route

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
