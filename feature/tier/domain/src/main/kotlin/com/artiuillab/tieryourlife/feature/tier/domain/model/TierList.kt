package com.artiuillab.tieryourlife.feature.tier.domain.model

data class TierList(
    val id: Long,
    val title: String,
    val tiers: List<Tier>,
    val displayMode: TierListDisplayMode = TierListDisplayMode.WRAP,
    /** Set once this list has been published; the id the server keeps it under. */
    val publishedId: String? = null,
    /**
     * What was sent the last time this board was published, so that the board
     * can tell whether it has moved on since.
     *
     * Opaque: nothing reads it, only compares it. Null on a board published
     * before this was recorded, which reads as "we do not know" -- and not
     * knowing must never be shown as "behind", or somebody is sent to republish
     * a list that was already right.
     */
    val publishedFingerprint: String? = null,
    /** Set on a copy taken from someone else's published list. */
    val authorName: String? = null,
    /** Where this list sits in the community feed. Required before publishing. */
    val category: ListCategory? = null,
    /** The author's own cover. A local pick never reaches the community. */
    val coverImageUrl: String? = null,
    /**
     * Set only on the copy kept after the same board was changed on two
     * phones. Names the phone the other version came from, when that phone
     * had a name worth showing.
     */
    val arrivedFrom: String? = null,
    /** When anything about this board last changed. */
    val editedAt: Long? = null,
    /** True while this board and one beside it are two versions of the same thing. */
    val hasTwin: Boolean = false,
)
