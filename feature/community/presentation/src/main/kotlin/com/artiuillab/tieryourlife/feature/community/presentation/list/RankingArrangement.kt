package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/**
 * The author's cards where [ranking] put them, under the words it gave the
 * tiers: in its order within each tier when it kept one, in the list's order
 * when it kept only which tier.
 */
internal fun placedBy(answer: PublishedList, ranking: Ranking): TierList {
    val rows = rowsPlaced(answer, ranking.rows) ?: answer.tiers.indices.map { index ->
        answer.items.filterIndexed { at, _ -> ranking.arrangement.getOrNull(at) == index }
    }
    val placed = rows.flatten().mapTo(mutableSetOf()) { it.id }
    val ranked = answer.tiers.mapIndexed { index, tier ->
        tier.copy(label = ranking.tierLabels.getOrNull(index) ?: tier.label, items = rows.getOrNull(index).orEmpty())
    }
    return TierList(id = 0, title = answer.summary.title, tiers = ranked + pool(answer.items.filterNot { it.id in placed }), authorName = ranking.authorName)
}

/** Each tier's cards from [rows], each card once; null when there are none to go by. */
private fun rowsPlaced(answer: PublishedList, rows: List<List<Int>>): List<List<TierItem>>? {
    if (rows.isEmpty()) return null
    val seen = mutableSetOf<Int>()
    return answer.tiers.indices.map { index ->
        rows.getOrNull(index).orEmpty().filter { seen.add(it) }.mapNotNull(answer.items::getOrNull)
    }
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

/** [board]'s tiers in the list's order, each its cards left to right as positions among the list's items. */
internal fun rowsOf(answer: PublishedList, board: TierList): List<List<Int>> {
    val at = answer.items.withIndex().associate { (index, item) -> item.id to index }
    val tiers = board.tiers.associateBy { it.id }
    return answer.tiers.map { tier -> tiers[tier.id]?.items.orEmpty().mapNotNull { at[it.id] } }
}

/** What [board] calls each of the list's tiers, in the list's order. */
internal fun tierLabelsOf(answer: PublishedList, board: TierList): List<String> {
    val labels = board.tiers.associate { it.id to it.label }
    return answer.tiers.map { labels[it.id] ?: it.label }
}
