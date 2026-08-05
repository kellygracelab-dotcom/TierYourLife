package com.artiuillab.tieryourlife.feature.tier.domain.export

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

// Every already-resolved piece of text the formatter needs, supplied by the caller
// (presentation, via stringResource/pluralStringResource) so this function stays pure
// Kotlin and never touches an Android resource itself (docs/design-spec-home.md,
// section 8 — "Getting the strings into a pure domain function without dragging Android
// resources into the domain module"). The two counting lambdas exist because plurals
// can only be resolved once the count is known, and the counts themselves (totals across
// every list, and per list) are only known once this function has already walked the
// data — the caller can't precompute them.
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

// docs/design-spec-home.md, section 8. A plain, readable listing — not JSON, not CSV —
// in the same order Home shows the lists, tiers in position order (S first, worst
// last), items numbered within their tier, the pool last and unnumbered.
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
        // A rule of "=" the width of the name — re-measured per list, not a fixed width.
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

            // Included even when empty, with a placeholder instead of silently vanishing —
            // a tier the user made and left empty is information the file shouldn't drop.
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
            // Unnumbered and last — the pool has no order worth exporting.
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
