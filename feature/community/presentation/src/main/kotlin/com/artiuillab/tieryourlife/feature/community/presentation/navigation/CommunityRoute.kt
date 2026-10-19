package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.lifecycle.SavedStateHandle
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

/**
 * The route read straight from the handle, by property name, as Navigation
 * stores it. toRoute() would do the same through Android's Bundle, which
 * keeps a view model off the JVM. RouteFromHandleTest holds the two equal.
 */
internal fun SavedStateHandle.communityListRoute() =
    CommunityRoute.CommunityList(publishedId = required(CommunityRoute.CommunityList::publishedId.name))

internal fun SavedStateHandle.authorRoute() = CommunityRoute.Author(
    authorUid = required(CommunityRoute.Author::authorUid.name),
    authorName = required(CommunityRoute.Author::authorName.name),
    authorPhotoUrl = get<String>(CommunityRoute.Author::authorPhotoUrl.name),
)

private fun <T : Any> SavedStateHandle.required(key: String): T =
    checkNotNull(get<T>(key)) { "Route argument $key is missing" }
