package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

data class PublishedListSummary(
    val id: String,
    val title: String,
    val authorUid: String,
    val authorName: String,
    val authorPhotoUrl: String? = null,
    /** A section id; Other's when the server named none. */
    val category: String,
    val itemCount: Int,
    val coverImageUrl: String? = null,
    /** Card art the feed draws a mosaic from when there is no cover. */
    val previewImages: List<String> = emptyList(),
    /** The author's palette, for a card with neither a cover nor card art. */
    val tierColors: List<String> = emptyList(),
    val updatedAtMillis: Long,
    /** How many people have taken this list: the number the popular ordering sorts by, so what a reader sees and what put it there agree. */
    val takeCount: Int = 0,
    /** Set by the server once it marks lists that stand without an author; absent before. */
    val anonymous: Boolean = false,
    /** The topic the author filed it under, if any; the name travels too, so a card needs no second read. */
    val topicId: String? = null,
    val topicName: String? = null,
    /** How many readers have placed these cards their own way. */
    val rankingCount: Int = 0,
    /** The list this one was made from, when a reader changed the cards rather than only placing them. */
    val basedOn: BasedOn? = null,
    /** On the author's own list: the waiting proposal it moves to once approved, and its name. */
    val proposedSection: String? = null,
    val proposedSectionName: String? = null,
) {
    /** Shown without its author, and leading to no profile. */
    val editorial: Boolean get() = EditorialAuthors.isEditorial(authorUid, anonymous)

    /** Somebody's list out without their name, rather than one of the app's own: its author reads "Anonymous". */
    val byNobody: Boolean get() = editorial && !EditorialAuthors.isTheAppsOwn(authorUid)
}

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

/** A list a copy was changed from: named on the copy's page, and a tap away. Its author is empty when the original is anonymous. */
data class BasedOn(val id: String, val title: String, val authorName: String, val authorUid: String = "")
