package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/** One colour in the light theme and the dark one, as "#RRGGBB". */
data class ThemePair(val light: String, val dark: String)

/** Where a section stands. Only approved ones are drawn for now; the rest are its proposer's and the moderators' business. */
enum class SectionStatus(val id: String) {
    Approved("approved"),
    Pending("pending"),
    Declined("declined"),
    Merged("merged"),
    ;

    companion object {
        fun fromId(id: String?): SectionStatus = entries.firstOrNull { it.id == id } ?: Approved
    }
}

/**
 * A section as the server describes it (docs/sections-api.md §1). The nine of
 * [ListCategory] are sections like any other here, with no picture: the app
 * draws their art itself. Counts are as of the last answer.
 */
data class Section(
    /** Never changes, even when the section is renamed. */
    val id: String,
    /** As typed by its proposer or edited by a moderator; empty while the server has given none. */
    val name: String = "",
    /** Translations, by locale tag: `uk`, `pt-BR`. */
    val names: Map<String, String> = emptyMap(),
    /** A Material Symbols name, one of [SectionSymbols.ALL]. */
    val icon: String = SectionSymbols.DEFAULT,
    val pictureUrl: String? = null,
    val tint: ThemePair? = null,
    val topicBg: ThemePair? = null,
    val topicIcon: ThemePair? = null,
    val status: SectionStatus = SectionStatus.Approved,
    /** Left out for everybody but moderators. */
    val hidden: Boolean = false,
    val builtIn: Boolean = false,
    /** A moderator's pin: pinned sections come first, lowest first. */
    val rank: Int? = null,
    /** Zero for the built-ins, which keeps them in their old order among themselves. */
    val approvedAt: Long = 0,
    val listCount: Int = 0,
    /** Lists published in the last thirty days: what orders the unpinned. */
    val lists30d: Int = 0,
    val following: Boolean = false,
    /** Up to four covers for the section page; empty while too few lists qualify, and the art stands in. */
    val collage: List<String> = emptyList(),
    /** On a waiting proposal: where its proposer's lists live meanwhile. */
    val fileUnder: String? = null,
    /** This person's own proposal: waiting, or turned down or merged and not yet dismissed. Moderators get everybody's. */
    val mine: Boolean = false,
    /** A moderator's word on a declined proposal, for its proposer. */
    val declineNote: String? = null,
    /** Where a merged proposal went. */
    val mergedInto: String? = null,
    /** How many of the proposer's lists wait for this one. */
    val waitingLists: Int = 0,
) {
    /** The built-in this section is, whose art and words the app has of its own; null for everybody else's. */
    val art: ListCategory? get() = ListCategory.fromId(id)

    /** What ordinary screens draw: approved, and not hidden by a moderator. */
    val shown: Boolean get() = status == SectionStatus.Approved && !hidden

    val isOther: Boolean get() = id == ListCategory.Other.id

    /** This person's own proposal still waiting for a moderator. */
    val waiting: Boolean get() = mine && status == SectionStatus.Pending

    /** This person's own proposal decided against: a notice until they dismiss it. */
    val settledAgainst: Boolean get() = mine && (status == SectionStatus.Declined || status == SectionStatus.Merged)

    /** `names[locale] ?? name`: the exact tag first, then its language, so `pt-BR` and a plain `uk` both find theirs. Null when the server named nothing. */
    fun nameIn(languageTag: String): String? {
        val chosen = names[languageTag] ?: names[languageTag.substringBefore('-')] ?: name
        return chosen.takeIf { it.isNotBlank() }
    }

    /** The button changes in the same frame. */
    fun followed(now: Boolean): Section = if (following == now) this else copy(following = now)

    companion object {
        /** One of the nine as the app knows it before any answer: no name of the server's, no counts. */
        fun builtIn(category: ListCategory) = Section(id = category.id, icon = category.symbol, builtIn = true)

        /** All nine, in their own order, Other last. */
        val BUILT_IN: List<Section> = ListCategory.entries.map(::builtIn)
    }
}

/** A section this person follows. */
data class FollowedSection(val id: String)

/** What the server says of one id asked for alone (`GET lists/sections/{id}`). */
sealed interface SectionLookup {
    data class Found(val section: Section) : SectionLookup

    /** `410 MERGED`: the id lives on as another. */
    data class Merged(val into: String) : SectionLookup

    /** `404`: no such section, or somebody else's proposal. */
    data object Unknown : SectionLookup
}

/** The icons a section may carry: the nine built-ins' first, then the fifteen a proposer picks from. */
object SectionSymbols {
    const val DEFAULT = "category"

    val PROPOSABLE: List<String> = listOf(
        "pets", "flight", "directions_car", "palette", "checkroom", "science", "computer", "local_cafe",
        "park", "theater_comedy", "mic", "photo_camera", "sports_basketball", "celebration", "star",
    )

    val ALL: List<String> = listOf(
        "category", "movie", "animation", "sports_esports", "music_note", "menu_book", "restaurant", "sports_soccer", "person",
    ) + PROPOSABLE
}
