package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** The author's cards where [ranking] put them, under the words it gave the tiers. */
internal fun placedBy(answer: PublishedList, ranking: Ranking): TierList {
    val where = ranking.arrangement
    val ranked = answer.tiers.mapIndexed { index, tier ->
        tier.copy(
            label = ranking.tierLabels.getOrNull(index) ?: tier.label,
            items = answer.items.filterIndexed { at, _ -> where.getOrNull(at) == index },
        )
    }
    val unranked = answer.items.filterIndexed { at, _ -> where.getOrNull(at) == null }
    return TierList(id = 0, title = answer.summary.title, tiers = ranked + pool(unranked), authorName = ranking.authorName)
}

/**
 * The other way round: where [board] put each of the list's cards, in the
 * list's own item order, as its tier's position in the list or null. Cards and
 * tiers are matched by id, which a drag and a game's result both keep.
 */
internal fun arrangementOf(answer: PublishedList, board: TierList): List<Int?> {
    val position = answer.tiers.withIndex().associate { (index, tier) -> tier.id to index }
    val tierOf = board.tiers.filterNot { it.isPool }.flatMap { tier -> tier.items.map { it.id to tier.id } }.toMap()
    return answer.items.map { item -> tierOf[item.id]?.let(position::get) }
}

/** What [board] calls each of the list's tiers, in the list's order. */
internal fun tierLabelsOf(answer: PublishedList, board: TierList): List<String> {
    val labels = board.tiers.associate { it.id to it.label }
    return answer.tiers.map { labels[it.id] ?: it.label }
}
