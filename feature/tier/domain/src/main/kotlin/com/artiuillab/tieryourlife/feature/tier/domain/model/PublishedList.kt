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
    /** How many people have taken this list: the number the popular ordering sorts by, so what a reader sees and what put it there agree. */
    val takeCount: Int = 0,
)

/** Their cards and tier definitions, with no ranking; where it goes is the reader's business. */
data class PublishedList(
    val summary: PublishedListSummary,
    val tiers: List<Tier>,
    val items: List<TierItem>,
    /**
     * Where the author put each card, aligned with [items]: a tier's position
     * in [tiers], or null for unranked. Empty on a snapshot published before
     * this was recorded -- not the same as all-null: nobody knows, versus they
     * ranked nothing.
     */
    val arrangement: List<Int?> = emptyList(),
)
