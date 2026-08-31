package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModerationReportDto(
    val listId: String,
    val listTitle: String = "",
    val authorName: String = "",
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
