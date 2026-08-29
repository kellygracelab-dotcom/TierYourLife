package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object TierLists : Route

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
}
