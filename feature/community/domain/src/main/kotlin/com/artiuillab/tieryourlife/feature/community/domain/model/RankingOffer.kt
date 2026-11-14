package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

data class RankingOffer(val placed: Int, val total: Int)

/** Once, never after an answer: with every card placed, or straight [afterGame] with anything placed. */
fun rankingOffer(board: TierList, choice: RankingChoice?, afterGame: Boolean = false): RankingOffer? {
    if (choice != null) return null
    val all = board.tiers.flatMap { it.items }.map { it.id }.toSet()
    val placed = board.tiers.filterNot { it.isPool }.flatMap { it.items }.map { it.id }.toSet().size
    val ready = placed > 0 && (placed == all.size || afterGame)
    return if (ready) RankingOffer(placed, all.size) else null
}
