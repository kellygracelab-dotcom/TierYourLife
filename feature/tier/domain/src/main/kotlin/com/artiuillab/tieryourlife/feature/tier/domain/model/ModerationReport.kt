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
    /** Every reason given, newest first. As many as there were complaints. */
    val reasons: List<ReportReason>,
    /** What the people who complained wrote, verbatim. Newest first. */
    val notes: List<String>,
    val reportCount: Int,
    val newestAtMillis: Long,
    /** Nobody can see this list until it is kept or taken down. */
    val hidden: Boolean,
    /** Kept once already, so further complaints no longer take it out. */
    val reviewed: Boolean,
) {
    /** The one to lead with. There is always at least one. */
    val reason: ReportReason get() = reasons.firstOrNull() ?: ReportReason.Other
}
