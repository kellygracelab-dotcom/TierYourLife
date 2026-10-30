package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.list.ListPreview
import kotlinx.serialization.Serializable
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic as TopicModel

sealed interface CommunityRoute {
    /**
     * The id, and what the card that was tapped already showed: the screen
     * draws the header and the rows, with a blank card for every card, at
     * once and fills the cards in when the answer lands. Optional, since
     * moderation opens a list by its id alone.
     */
    @Serializable
    data class CommunityList(
        val publishedId: String,
        val title: String? = null,
        val authorName: String? = null,
        val itemCount: Int = 0,
        /** Hex colours joined with commas: one string travels through a route and a handle alike. */
        val tierColors: String? = null,
        /** Cards per tier, the pool last, joined the same way. */
        val tierCounts: String? = null,
        val authorUid: String? = null,
        val authorPhotoUrl: String? = null,
        val byNobody: Boolean = false,
        val mine: Boolean = false,
        val anonymous: Boolean = false,
        val section: String? = null,
        val rankingCount: Int = 0,
        /** Whether the reader follows the author is known, and then [following] says it. */
        val followKnown: Boolean = false,
        val following: Boolean = false,
        val basedOnId: String? = null,
        val basedOnTitle: String? = null,
        val basedOnAuthor: String? = null,
    ) : CommunityRoute {

        fun preview(): ListPreview? = title?.let { known ->
            ListPreview(
                title = known,
                authorName = authorName,
                itemCount = itemCount,
                tierColors = tierColors?.split(',')?.filter(String::isNotEmpty).orEmpty(),
                tierCounts = tierCounts?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty(),
                authorUid = authorUid.orEmpty(),
                authorPhotoUrl = authorPhotoUrl,
                byNobody = byNobody,
                mine = mine,
                anonymous = anonymous,
                section = section,
                rankingCount = rankingCount,
                viewerFollowsAuthor = following.takeIf { followKnown },
                basedOn = basedOnId?.let { BasedOn(id = it, title = basedOnTitle.orEmpty(), authorName = basedOnAuthor.orEmpty()) },
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
                tierCounts = summary.tierCounts.joinToString(",").takeIf { it.isNotEmpty() },
                authorUid = summary.authorUid.takeUnless { summary.editorial },
                authorPhotoUrl = summary.authorPhotoUrl.takeUnless { summary.editorial },
                byNobody = summary.byNobody,
                mine = summary.mine,
                anonymous = summary.anonymous,
                section = summary.category,
                rankingCount = summary.rankingCount,
                followKnown = summary.viewerFollowsAuthor != null,
                following = summary.viewerFollowsAuthor == true,
                basedOnId = summary.basedOn?.id,
                basedOnTitle = summary.basedOn?.title,
                basedOnAuthor = summary.basedOn?.authorName,
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
     * [sectionId] is any section's id; one the phone does not know yet is asked about.
     */
    @Serializable
    data class Section(val sectionId: String, val topicId: String? = null) : CommunityRoute

    /** Everyone this person follows, with a way to stop. */
    @Serializable
    data object Following : CommunityRoute

    /** Every section, where Home has no room for them all. */
    @Serializable
    data object AllSections : CommunityRoute

    /** The proposal form; [fileUnder] is the section of the list it was opened from, if any. */
    @Serializable
    data class ProposeSection(val fileUnder: String? = null) : CommunityRoute

    /**
     * One topic's page. The name and section travel so the header is right
     * before the server answers; a chip that knows only the id sends the id.
     */
    @Serializable
    data class Topic(
        val topicId: String,
        val name: String? = null,
        /** A section id; null while the topic waits. */
        val section: String? = null,
    ) : CommunityRoute {
        companion object {
            fun of(topic: TopicModel) = Topic(topicId = topic.id, name = topic.name, section = topic.section)
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
    tierCounts = get<String>(CommunityRoute.CommunityList::tierCounts.name),
    authorUid = get<String>(CommunityRoute.CommunityList::authorUid.name),
    authorPhotoUrl = get<String>(CommunityRoute.CommunityList::authorPhotoUrl.name),
    byNobody = get<Boolean>(CommunityRoute.CommunityList::byNobody.name) ?: false,
    mine = get<Boolean>(CommunityRoute.CommunityList::mine.name) ?: false,
    anonymous = get<Boolean>(CommunityRoute.CommunityList::anonymous.name) ?: false,
    section = get<String>(CommunityRoute.CommunityList::section.name),
    rankingCount = get<Int>(CommunityRoute.CommunityList::rankingCount.name) ?: 0,
    followKnown = get<Boolean>(CommunityRoute.CommunityList::followKnown.name) ?: false,
    following = get<Boolean>(CommunityRoute.CommunityList::following.name) ?: false,
    basedOnId = get<String>(CommunityRoute.CommunityList::basedOnId.name),
    basedOnTitle = get<String>(CommunityRoute.CommunityList::basedOnTitle.name),
    basedOnAuthor = get<String>(CommunityRoute.CommunityList::basedOnAuthor.name),
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

internal fun SavedStateHandle.proposeSectionRoute() = CommunityRoute.ProposeSection(
    fileUnder = get<String>(CommunityRoute.ProposeSection::fileUnder.name),
)

internal fun SavedStateHandle.sectionRoute() = CommunityRoute.Section(
    sectionId = required(CommunityRoute.Section::sectionId.name),
    topicId = get<String>(CommunityRoute.Section::topicId.name),
)

private fun <T : Any> SavedStateHandle.required(key: String): T =
    checkNotNull(get<T>(key)) { "Route argument $key is missing" }
