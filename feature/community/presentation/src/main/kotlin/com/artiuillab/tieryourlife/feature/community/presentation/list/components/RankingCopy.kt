package com.artiuillab.tieryourlife.feature.community.presentation.list.components

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.readIn
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Local ids never identify source cards; duplicate-looking cards keep their source positions. */
internal fun TierList.isCopyOf(source: PublishedList, empty: TierList): Boolean {
    if (takenFrom != source.summary.id || originVersion != source.summary.updatedAtMillis) return false
    val cards = tiers.flatMap { it.items }
    if (cards.size != source.items.size || cards.map { it.sourceIndex }.toSet() != source.items.indices.toSet()) return false
    val variants = listOf(empty) + source.translations.byLocale.values.map { empty.readIn(it) }
    return variants.any { variant ->
        val rows = tiers.filterNot { it.isPool }
        val reference = variant.tiers.filterNot { it.isPool }
        val texts = variant.tiers.flatMap { it.items }.associateBy { it.id }
        title == variant.title &&
            rows.map { it.label to it.caption } == reference.map { it.label to it.caption } &&
            cards.all { card ->
                val original = source.items[requireNotNull(card.sourceIndex)]
                val expected = texts[original.id] ?: original
                card.title == expected.title && card.imageUrl == expected.imageUrl
            }
    }
}

internal fun TierList.sourceDraft(source: PublishedList) = RankingDraft(
    version = source.summary.updatedAtMillis,
    rows = tiers.filterNot { it.isPool }.map { row -> row.items.map { requireNotNull(it.sourceIndex) } },
    pool = tiers.filter { it.isPool }.flatMap { it.items }.map { requireNotNull(it.sourceIndex) },
    choice = rankingChoice ?: RankingChoice.Ranking.takeIf { rankingOf != null },
    savedBoardUid = uid,
)

/** Map the private editor's source ids onto the saved copy's actual Room ids. */
internal fun TierList.layoutOf(source: PublishedList, mine: TierList): Map<Long, List<Long>> {
    val bySource = tiers.flatMap { it.items }.associate { it.sourceIndex to it.id }
    val indices = source.items.withIndex().associate { it.value.id to it.index }
    val rows = mine.tiers.filterNot { it.isPool }
    var row = 0
    return tiers.associate { target ->
        val cards = if (target.isPool) mine.tiers.filter { it.isPool }.flatMap { it.items } else rows[row++].items
        target.id to cards.map { requireNotNull(bySource[indices[it.id]]) }
    }
}
