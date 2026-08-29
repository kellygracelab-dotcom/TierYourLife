package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModerationReportDto(
    val listId: String,
    val listTitle: String = "",
    val authorName: String = "",
    val reason: String = "other",
    val note: String? = null,
    val createdAt: Long = 0,
)

@Serializable
data class ModerationQueueDto(
    val reports: List<ModerationReportDto> = emptyList(),
)
