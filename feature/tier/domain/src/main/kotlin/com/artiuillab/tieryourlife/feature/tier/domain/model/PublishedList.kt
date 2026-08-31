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
    /**
     * How many people have taken this list to rank for themselves. The card
     * says the number the popular ordering sorts by, so that what a reader
     * sees and what put it there are the same thing.
     */
    val takeCount: Int = 0,
)

/**
 * Someone else's list as it arrives: their cards and their tier definitions,
 * with no ranking. Where it goes is the reader's business.
 */
data class PublishedList(
    val summary: PublishedListSummary,
    val tiers: List<Tier>,
    val items: List<TierItem>,
    /**
     * Where the author put each card, aligned with [items]: the position of a
     * tier in [tiers], or null for one they left unranked.
     *
     * Empty on a snapshot published before this was recorded. Empty is not the
     * same as all-null: the first means nobody knows what the author thought,
     * the second means they thought nothing, and the screen says different
     * things about them.
     */
    val arrangement: List<Int?> = emptyList(),
)
