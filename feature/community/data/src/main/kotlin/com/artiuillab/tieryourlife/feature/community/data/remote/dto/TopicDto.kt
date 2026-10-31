package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TopicDto(
    val id: String,
    val name: String = "",
    val section: String? = null,
    val status: String = "pending",
    val followerCount: Int = 0,
    val following: Boolean = false,
    val createdBy: String = "",
    val mergedInto: String? = null,
    val pictureUrl: String? = null,
)

@Serializable
data class TopicsDto(
    val topics: List<TopicDto> = emptyList(),
)

/** A topic in the queue: the topic's own fields, flattened, plus what a decision needs. */
@Serializable
data class PendingTopicDto(
    val id: String,
    val name: String = "",
    val section: String? = null,
    val status: String = "pending",
    val followerCount: Int = 0,
    val following: Boolean = false,
    val createdBy: String = "",
    val mergedInto: String? = null,
    val pictureUrl: String? = null,
    val proposerName: String = "",
    val requestedSection: String? = null,
    val similar: List<TopicDto> = emptyList(),
)

@Serializable
data class PendingTopicsDto(
    val topics: List<PendingTopicDto> = emptyList(),
)

@Serializable
data class TopicSectionRequestDto(val section: String)

@Serializable
data class TopicNameRequestDto(val name: String)

@Serializable
data class TopicMergeRequestDto(val into: String)

@Serializable
/** The topic travels even when null: an absent one would keep the old topic, a null one drops it. */
data class MoveListRequestDto(val category: String, val topicId: String?)

@Serializable
data class ModeratorDto(
    val uid: String,
    val name: String = "",
    val photoUrl: String? = null,
    val appointedAt: Long = 0,
)

@Serializable
data class ModeratorsDto(
    val moderators: List<ModeratorDto> = emptyList(),
)

@Serializable
data class AppointModeratorRequestDto(val name: String? = null, val photoUrl: String? = null)
