package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * One complaint, as the person who reads them sees it. Several people
 * complaining about one list arrive as several of these on purpose: how many
 * there were is the useful part, and grouping would hide it.
 */
data class ModerationReport(
    val listId: String,
    val listTitle: String,
    val authorName: String,
    val reason: ReportReason,
    val note: String?,
    val createdAtMillis: Long,
)
