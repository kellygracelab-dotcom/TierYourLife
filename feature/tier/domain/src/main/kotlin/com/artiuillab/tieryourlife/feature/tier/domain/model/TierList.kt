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
    /** The id of the section this list sits in: one of [ListCategory]'s or any approved one. Required before publishing. */
    val category: String? = null,
    /** Published with no name or face; kept on the board, so every publish from any phone says the same. */
    val anonymous: Boolean = false,
    /** The author's own cover. A local pick never reaches the community. */
    val coverImageUrl: String? = null,
    /** Two to six web pictures the author chose as a collage; empty while the cover is one picture or left to the cards. */
    val coverImageUrls: List<String> = emptyList(),
    /** The community topic this list is filed under, once the server knows it. */
    val topicId: String? = null,
    val topicName: String? = null,
    /** A topic the author named that the server has not seen yet; sent with the next publish and replaced by what comes back. */
    val newTopicName: String? = null,
    /** The published list this board was taken from: publishing the same cards answers that list rather than making a second. */
    val takenFrom: String? = null,
    /** Set while this board is out as this person's ranking of [takenFrom]; [publishedId] stays null then. */
    val rankingOf: String? = null,
    /** On the copy kept after the same board changed on two phones: the other phone's name. */
    val arrivedFrom: String? = null,
    /** When anything about this board last changed. */
    val editedAt: Long? = null,
    /** Starred boards come first however the rest are sorted, most recent first. */
    val favouritedAt: Long? = null,
    /** True while this board and one beside it are two versions of the same thing. */
    val hasTwin: Boolean = false,
    /** The saved Play game in the JSON both clients write, kept on the board so the account carries it; read by Play alone. */
    val play: String? = null,
) {
    /** Out in the community one way or the other: as a list of its own, or as a ranking of somebody's. */
    val isPublic: Boolean get() = publishedId != null || rankingOf != null
}
