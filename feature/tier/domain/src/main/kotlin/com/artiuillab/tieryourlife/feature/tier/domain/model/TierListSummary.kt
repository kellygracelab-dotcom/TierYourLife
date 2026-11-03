package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * One of somebody's boards as a list of boards draws it: its counts, its
 * colours and its first pictures, and none of its cards. The board itself is
 * read when it is opened.
 */
data class TierListSummary(
    val id: Long,
    val title: String,
    /** In board order, the pool among them. */
    val tiers: List<TierSummary>,
    /** The first pictures among its cards, in board order, at most [PREVIEW_IMAGES]; left out under a collage, which is drawn instead. */
    val previewImages: List<String> = emptyList(),
    val publishedId: String? = null,
    val rankingOf: String? = null,
    val category: String? = null,
    /** Set on a copy taken from someone else's published list. */
    val authorName: String? = null,
    val coverImageUrl: String? = null,
    val coverImageUrls: List<String> = emptyList(),
    val arrivedFrom: String? = null,
    val editedAt: Long? = null,
    val favouritedAt: Long? = null,
    /** True while this board and one beside it are two versions of the same thing. */
    val hasTwin: Boolean = false,
) {
    val isPublic: Boolean get() = publishedId != null || rankingOf != null

    val rankedCount: Int get() = tiers.filterNot { it.isPool }.sumOf { it.cards }

    val poolCount: Int get() = tiers.filter { it.isPool }.sumOf { it.cards }

    companion object {
        const val PREVIEW_IMAGES = 6
    }
}

data class TierSummary(
    val id: Long,
    val colorLight: String,
    val colorDark: String,
    val isPool: Boolean,
    val cards: Int,
)

/** The same summary the database would give, for a board already read whole. */
fun TierList.summary(): TierListSummary = TierListSummary(
    id = id,
    title = title,
    tiers = tiers.map { TierSummary(it.id, it.colorLight, it.colorDark, it.isPool, it.items.size) },
    previewImages = if (coverImageUrls.isNotEmpty()) {
        emptyList()
    } else {
        tiers.asSequence().flatMap { it.items }.mapNotNull { it.imageUrl }.take(TierListSummary.PREVIEW_IMAGES).toList()
    },
    publishedId = publishedId,
    rankingOf = rankingOf,
    category = category,
    authorName = authorName,
    coverImageUrl = coverImageUrl,
    coverImageUrls = coverImageUrls,
    arrivedFrom = arrivedFrom,
    editedAt = editedAt,
    favouritedAt = favouritedAt,
    hasTwin = hasTwin,
)
