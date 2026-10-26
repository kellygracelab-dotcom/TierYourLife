package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/**
 * A section as the server counts it. The nine sections are fixed
 * ([ListCategory]); what moves is how many lists there are, how many are new
 * this week, who follows, and which covers are good enough for a header.
 */
data class SectionStats(
    val section: ListCategory,
    val listCount: Int = 0,
    val followerCount: Int = 0,
    /** Lists published or changed in the last seven days. */
    val newThisWeek: Int = 0,
    val following: Boolean = false,
    /** Up to four covers for the section page; empty while too few lists qualify, and the art stands in. */
    val collage: List<String> = emptyList(),
)

/** A section this person follows, with its count for the row that offers to stop. */
data class FollowedSection(val section: ListCategory, val followerCount: Int = 0)
