package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/**
 * One reader's placing of a list's cards: where each card went, by its
 * position among the list's items, and what they called the tiers.
 */
data class Ranking(
    val uid: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val arrangement: List<Int?>,
    val tierLabels: List<String>,
    val updatedAtMillis: Long,
    /** This reader's own, in its place among the rest. */
    val mine: Boolean = false,
    /** Each tier's cards left to right, by position among the list's items; empty on a placing kept without its order. */
    val rows: List<List<Int>> = emptyList(),
    /** Whether this reader liked it. How many did is never told. */
    val likedByMe: Boolean = false,
    /** Cards the reader added to their version; position N+k in [allRows] for a list of N cards. Their ids mean nothing. */
    val addedCards: List<TierItem> = emptyList(),
    /** [rows] with the added cards in them; empty when nothing was added. */
    val allRows: List<List<Int>> = emptyList(),
) {
    /** Somebody else's: the only kind there is to like. */
    val likeable: Boolean get() = !mine
}

data class RankingsPage(val rankings: List<Ranking>, val nextCursor: String?)

/** This reader's placing as the server now keeps it; [created] when it was not shown before. */
data class ShownRanking(val ranking: Ranking, val created: Boolean)

/**
 * Why a reader's placing was not shown. A `400 INVALID` is [BadTierNames] for
 * its tier names, [CardsDontMatch] for its cards, and [NotAccepted] when it
 * did not say which.
 */
enum class RankingRefusal {
    NotSignedIn,
    Banned,
    OwnList,
    ListGone,
    ListChanged,
    BadTierNames,
    CardsDontMatch,
    NotAccepted,
    TooManyWebPictures,
    WebPicturesToday,
    Failed,
}

class RankingRefused(val reason: RankingRefusal) : Exception(reason.name)

/** Why a like did not go through (SPEC list-flow К2). */
enum class LikeRefusal {
    NotSignedIn,
    Banned,
    Gone,
    CannotLike,
    TooMany,
    Failed,
}

class LikeRefused(val reason: LikeRefusal) : Exception(reason.name)

/** The list is not there any more: taken down, or never published. */
class ListGone : Exception("The list is gone")
