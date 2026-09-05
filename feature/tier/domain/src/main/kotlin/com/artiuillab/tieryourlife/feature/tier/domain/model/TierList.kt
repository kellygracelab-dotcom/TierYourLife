package com.artiuillab.tieryourlife.feature.tier.domain.model

data class TierList(
    val id: Long,
    val title: String,
    val tiers: List<Tier>,
    val displayMode: TierListDisplayMode = TierListDisplayMode.WRAP,
    /** Set once this list has been published; the id the server keeps it under. */
    val publishedId: String? = null,
    /**
     * What was sent when last published; opaque, only compared. Null on a board
     * published before this was recorded: "we do not know", never to be shown
     * as "behind".
     */
    val publishedFingerprint: String? = null,
    /** Set on a copy taken from someone else's published list. */
    val authorName: String? = null,
    /** Where this list sits in the community feed. Required before publishing. */
    val category: ListCategory? = null,
    /** The author's own cover. A local pick never reaches the community. */
    val coverImageUrl: String? = null,
    /** On the copy kept after the same board changed on two phones: the other phone's name. */
    val arrivedFrom: String? = null,
    /** When anything about this board last changed. */
    val editedAt: Long? = null,
    /** Starred boards come first however the rest are sorted, most recent first. */
    val favouritedAt: Long? = null,
    /** True while this board and one beside it are two versions of the same thing. */
    val hasTwin: Boolean = false,
)
