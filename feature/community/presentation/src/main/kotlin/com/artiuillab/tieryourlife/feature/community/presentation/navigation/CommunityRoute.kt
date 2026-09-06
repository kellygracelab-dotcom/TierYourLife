package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface CommunityRoute {
    @Serializable
    data class CommunityList(val publishedId: String) : CommunityRoute

    /** Name and face travel so the header is right before the lists arrive. */
    @Serializable
    data class Author(
        val authorUid: String,
        val authorName: String,
        val authorPhotoUrl: String? = null,
    ) : CommunityRoute

    @Serializable
    data object Moderation : CommunityRoute

    @Serializable
    data object MyPublished : CommunityRoute
}
