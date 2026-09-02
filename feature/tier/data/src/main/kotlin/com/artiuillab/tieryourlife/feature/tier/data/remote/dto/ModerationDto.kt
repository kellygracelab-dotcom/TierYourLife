package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModerationReportDto(
    val listId: String,
    val listTitle: String = "",
    val authorName: String = "",
    val authorUid: String? = null,
    val authorPhotoUrl: String? = null,
    val coverImageUrl: String? = null,
    /** Newest first. One per person who complained. */
    val reasons: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val reportCount: Int = 0,
    val newestAtMs: Long = 0,
    /** Out of the feed while it waits to be looked at. */
    val hidden: Boolean = false,
    /** Looked at once and kept, so later complaints no longer hide it. */
    val reviewed: Boolean = false,
)

@Serializable
data class ModerationQueueDto(
    val reports: List<ModerationReportDto> = emptyList(),
)

/** Where a picture of somebody's own ended up once it was made a face. */
@Serializable
data class FaceDto(val url: String)

/**
 * What a takedown asks for. Null bans nobody, which is what taking down a
 * list without judging its author means.
 */
@Serializable
data class TakeDownRequestDto(val ban: String? = null)
