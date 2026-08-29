package com.artiuillab.tieryourlife.feature.tier.domain.model

data class PublishedListSummary(
    val id: String,
    val title: String,
    val authorUid: String,
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val category: ListCategory,
    val itemCount: Int,
    val coverImageUrl: String? = null,
    /** Card art the feed draws a mosaic from when there is no cover. */
    val previewImages: List<String> = emptyList(),
    /** The author's palette, for a card with neither a cover nor card art. */
    val tierColors: List<String> = emptyList(),
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
