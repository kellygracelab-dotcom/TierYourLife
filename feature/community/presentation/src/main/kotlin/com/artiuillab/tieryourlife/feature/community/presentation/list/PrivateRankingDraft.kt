package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal fun draftOf(source: PublishedList, board: TierList): RankingDraft {
    val indices = source.items.withIndex().associate { it.value.id to it.index }
    return RankingDraft(
        version = source.summary.updatedAtMillis,
        rows = rowsOf(source, board),
        pool = board.tiers.filter { it.isPool }.flatMap { it.items }.mapNotNull { indices[it.id] },
        choice = board.rankingChoice,
        savedBoardUid = board.uid.takeIf { it.isNotBlank() },
    )
}

/** Never apply positions from a different publication or a damaged/incomplete draft. */
internal fun RankingDraft.on(source: PublishedList, empty: TierList): TierList? {
    val indices = rows.flatten() + pool
    val sameVersion = version == source.summary.updatedAtMillis && rows.size == source.tiers.size
    val complete = indices.size == source.items.size && indices.toSet() == source.items.indices.toSet()
    if (!sameVersion || !complete) return null
    val rowsById = source.tiers.withIndex().associate { it.value.id to rows[it.index].map(source.items::get) }
    return empty.copy(tiers = empty.tiers.map { tier -> tier.copy(items = if (tier.isPool) pool.map(source.items::get) else rowsById[tier.id].orEmpty()) })
}
