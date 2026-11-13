package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

data class RankingOffer(val placed: Int, val total: Int)

/** Strictly more than 30%; there is no additional minimum number of cards. */
fun rankingOffer(board: TierList, choice: RankingChoice?): RankingOffer? {
    if (choice != null) return null
    val all = board.tiers.flatMap { it.items }.map { it.id }.toSet()
    val placed = board.tiers.filterNot { it.isPool }.flatMap { it.items }.map { it.id }.toSet().size
    val overThreshold = all.isNotEmpty() && placed.toLong() * PROGRESS_PARTS > all.size.toLong() * OFFER_PARTS
    return if (overThreshold) RankingOffer(placed, all.size) else null
}

private const val PROGRESS_PARTS = 10
private const val OFFER_PARTS = 3
