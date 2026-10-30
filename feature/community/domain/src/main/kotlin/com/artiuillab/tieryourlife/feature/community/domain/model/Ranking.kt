package com.artiuillab.tieryourlife.feature.community.domain.model

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
    /** This reader's own, which the server lists first. */
    val mine: Boolean = false,
    /** Each tier's cards left to right, by position among the list's items; empty on a placing kept without its order. */
    val rows: List<List<Int>> = emptyList(),
)

data class RankingsPage(val rankings: List<Ranking>, val nextCursor: String?)

/** This reader's placing as the server now keeps it; [created] when it was not shown before. */
data class ShownRanking(val ranking: Ranking, val created: Boolean)

/** Why a reader's placing was not shown. */
enum class RankingRefusal { NotSignedIn, Banned, OwnList, Failed }

class RankingRefused(val reason: RankingRefusal) : Exception(reason.name)

/** The list is not there any more: taken down, or never published. */
class ListGone : Exception("The list is gone")
