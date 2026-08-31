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
    /**
     * [makeBoard] is the rail's new-board button arriving. It rides in the
     * route rather than in a counter above the graph so that exactly one
     * screen sees it: the one this navigation opened. A number kept above the
     * graph is read by the lists screen being left as well as the one
     * arriving, and both of them answered it.
     */
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
