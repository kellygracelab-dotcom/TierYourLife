package com.artiuillab.tieryourlife.feature.tier.domain.model

data class PublishedListSummary(
    val id: String,
    val title: String,
    val authorName: String,
    val itemCount: Int,
    val updatedAtMillis: Long,
)

/**
 * Someone else's list as it arrives: their cards and their tier definitions,
 * with no ranking. Where it goes is the reader's business.
 */
data class PublishedList(
    val summary: PublishedListSummary,
    val tiers: List<Tier>,
    val items: List<TierItem>,
)
