package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.list.ListPreview
import kotlinx.serialization.Serializable
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic as TopicModel

sealed interface CommunityRoute {
    /**
     * The id, and what the card that was tapped already showed: the screen
     * draws the title, the author, the size and the palette at once and fills
     * the cards in when the answer lands. Optional, since moderation opens a
     * list by its id alone.
     */
    @Serializable
    data class CommunityList(
        val publishedId: String,
        val title: String? = null,
        val authorName: String? = null,
        val itemCount: Int = 0,
        /** Hex colours joined with commas: one string travels through a route and a handle alike. */
        val tierColors: String? = null,
    ) : CommunityRoute {

        fun preview(): ListPreview? = title?.let { known ->
            ListPreview(
                title = known,
                authorName = authorName,
                itemCount = itemCount,
                tierColors = tierColors?.split(',')?.filter(String::isNotEmpty).orEmpty(),
            )
        }

        companion object {
            fun of(summary: PublishedListSummary) = CommunityList(
                publishedId = summary.id,
                title = summary.title,
                // A list that stands without an author has no name to show.
                authorName = summary.authorName.takeUnless { summary.editorial },
                itemCount = summary.itemCount,
                tierColors = summary.tierColors.joinToString(",").takeIf { it.isNotEmpty() },
            )
        }
    }

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

    /**
     * One section's page (§3), with a topic chosen when [topicId] is set.
     * [sectionId] is a [com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory] id.
     */
    @Serializable
    data class Section(val sectionId: String, val topicId: String? = null) : CommunityRoute

    /** Everyone this person follows, with a way to stop. */
    @Serializable
    data object Following : CommunityRoute

    /**
     * One topic's page. The name and section travel so the header is right
     * before the server answers; a chip that knows only the id sends the id.
     */
    @Serializable
    data class Topic(
        val topicId: String,
        val name: String? = null,
        /** A [com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory] id; null while the topic waits. */
        val section: String? = null,
    ) : CommunityRoute {
        companion object {
            fun of(topic: TopicModel) = Topic(topicId = topic.id, name = topic.name, section = topic.section?.id)
        }
    }
}

/**
 * The route read straight from the handle, by property name, as Navigation
 * stores it. toRoute() would do the same through Android's Bundle, which
 * keeps a view model off the JVM. RouteFromHandleTest holds the two equal.
 */
internal fun SavedStateHandle.communityListRoute() = CommunityRoute.CommunityList(
    publishedId = required(CommunityRoute.CommunityList::publishedId.name),
    title = get<String>(CommunityRoute.CommunityList::title.name),
    authorName = get<String>(CommunityRoute.CommunityList::authorName.name),
    itemCount = get<Int>(CommunityRoute.CommunityList::itemCount.name) ?: 0,
    tierColors = get<String>(CommunityRoute.CommunityList::tierColors.name),
)

internal fun SavedStateHandle.authorRoute() = CommunityRoute.Author(
    authorUid = required(CommunityRoute.Author::authorUid.name),
    authorName = required(CommunityRoute.Author::authorName.name),
    authorPhotoUrl = get<String>(CommunityRoute.Author::authorPhotoUrl.name),
)

internal fun SavedStateHandle.topicRoute() = CommunityRoute.Topic(
    topicId = required(CommunityRoute.Topic::topicId.name),
    name = get<String>(CommunityRoute.Topic::name.name),
    section = get<String>(CommunityRoute.Topic::section.name),
)

internal fun SavedStateHandle.sectionRoute() = CommunityRoute.Section(
    sectionId = required(CommunityRoute.Section::sectionId.name),
    topicId = get<String>(CommunityRoute.Section::topicId.name),
)

private fun <T : Any> SavedStateHandle.required(key: String): T =
    checkNotNull(get<T>(key)) { "Route argument $key is missing" }
