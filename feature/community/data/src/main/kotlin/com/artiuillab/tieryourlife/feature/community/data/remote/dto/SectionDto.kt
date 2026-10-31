package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One section as `GET lists/sections` sends it (docs/sections-api.md §2).
 * Everything past the counts is new: a server that predates sections as data
 * sends none of it, and a built-in falls back to the app's own.
 */
@Serializable
data class SectionDto(
    val id: String,
    val name: String? = null,
    val names: Map<String, String> = emptyMap(),
    val icon: String? = null,
    val pictureUrl: String? = null,
    val tint: ThemePairDto? = null,
    val topicBg: ThemePairDto? = null,
    val topicIcon: ThemePairDto? = null,
    val status: String? = null,
    val hidden: Boolean = false,
    val builtIn: Boolean? = null,
    val rank: Int? = null,
    val approvedAt: Long? = null,
    val listCount: Int = 0,
    val lists30d: Int = 0,
    val following: Boolean = false,
    val collage: List<String> = emptyList(),
    val fileUnder: String? = null,
    /** Sent on a proposal to its proposer and to moderators. */
    val proposerUid: String? = null,
    val declineNote: String? = null,
    val mergedInto: String? = null,
    val waitingLists: Int = 0,
)

@Serializable
data class ThemePairDto(val light: String, val dark: String)

@Serializable
data class SectionsDto(val sections: List<SectionDto> = emptyList(), val updatedAt: Long? = null)

@Serializable
data class SectionNameCheckDto(val free: Boolean = false, val id: String? = null, val code: String? = null)

@Serializable
data class SectionProposalDto(val name: String, val icon: String, val pictureId: String, val fileUnder: String)

/** The body of `410 MERGED`. */
@Serializable
data class SectionMergedDto(val mergedInto: String? = null)

@Serializable
data class FollowedSectionDto(val id: String)

@Serializable
data class SectionFollowDto(val following: Boolean = false)
