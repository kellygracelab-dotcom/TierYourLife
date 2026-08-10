package com.artiuillab.tieryourlife.feature.tier.domain.export

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

data class TierListsExportStrings(
    val header: String,
    val exportedOn: String,
    val listCountText: (count: Int) -> String,
    val rankedCountText: (count: Int) -> String,
    val unrankedCountText: (count: Int) -> String,
    val tierWithCaptionFormat: String,
    val tierPlainFormat: String,
    val tierEmptyLabel: String,
    val unrankedHeading: String,
)

fun buildTierListsExport(lists: List<TierList>, strings: TierListsExportStrings): String {
    val lines = mutableListOf<String>()

    val totalRanked = lists.sumOf { it.rankedItemCount() }
    val totalUnranked = lists.sumOf { it.unrankedItemCount() }

    lines += strings.header
    lines += strings.exportedOn
    lines += listOf(
        strings.listCountText(lists.size),
        strings.rankedCountText(totalRanked),
        strings.unrankedCountText(totalUnranked),
    ).joinToString(" · ")
    lines += ""

    lists.forEachIndexed { index, list ->
        lines += list.title
        lines += "=".repeat(list.title.length)
        lines += listOf(
            strings.rankedCountText(list.rankedItemCount()),
            strings.unrankedCountText(list.unrankedItemCount()),
        ).joinToString(" · ")
        lines += ""

        list.tiers.filterNot { it.isPool }.forEach { tier ->
            val heading = tier.caption?.let { caption ->
                String.format(strings.tierWithCaptionFormat, tier.label, caption)
            } ?: String.format(strings.tierPlainFormat, tier.label)
            lines += heading

            if (tier.items.isEmpty()) {
                lines += "  ${strings.tierEmptyLabel}"
            } else {
                tier.items.forEachIndexed { itemIndex, item ->
                    lines += "  ${itemIndex + 1}. ${item.title}"
                }
            }
        }

        val pool = list.tiers.firstOrNull { it.isPool }
        if (pool != null && pool.items.isNotEmpty()) {
            lines += strings.unrankedHeading
            pool.items.forEach { item -> lines += "  ${item.title}" }
        }

        if (index != lists.lastIndex) {
            lines += ""
        }
    }

    return lines.joinToString("\n")
}

private fun TierList.rankedItemCount(): Int = tiers.filterNot { it.isPool }.sumOf { it.items.size }

private fun TierList.unrankedItemCount(): Int = tiers.firstOrNull { it.isPool }?.items?.size ?: 0
