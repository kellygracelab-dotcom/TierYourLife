package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsUiState

internal const val COVER_CARD_WIDTH_DP = 36
internal const val COVER_CARD_GAP_DP = 4
internal const val COVER_BAND_DP = 32
internal const val COVER_ROW_PADDING_DP = 5

/** How many 36dp cards stand on one line of a row [rowWidthDp] wide, after the band and the padding. */
internal fun coverCardsPerLine(rowWidthDp: Int): Int =
    ((rowWidthDp - COVER_BAND_DP - COVER_ROW_PADDING_DP * 2 + COVER_CARD_GAP_DP) / (COVER_CARD_WIDTH_DP + COVER_CARD_GAP_DP))
        .coerceAtLeast(1)

/** [shown] cards, then a "+[more]" tile when [more] is above zero. */
internal data class CoverRowPlan(val shown: Int, val more: Int)

/**
 * A folded row keeps to one line: when the cards do not fit, the last place
 * goes to a tile saying how many more there are. An open row shows them all.
 */
internal fun coverRowPlan(cards: Int, perLine: Int, open: Boolean): CoverRowPlan = when {
    open || cards <= perLine -> CoverRowPlan(shown = cards, more = 0)
    else -> CoverRowPlan(shown = perLine - 1, more = cards - (perLine - 1))
}

/** One row open at a time: tapping the open one folds it, tapping another moves the opening there. */
internal fun toggledRow(open: Long?, tapped: Long): Long? = if (open == tapped) null else tapped

/** The list after [currentId] in the order the cover shows them, or null at the end or when it is gone. */
internal fun nextListAfter(lists: List<TierList>, currentId: Long): TierList? {
    val at = lists.indexOfFirst { it.id == currentId }
    return if (at < 0) null else lists.getOrNull(at + 1)
}

/** Your lists in the main screen's order: the starred ones first when they stand apart, then the rest. */
internal fun coverOrder(state: TierListsUiState): List<TierList> {
    val success = state as? TierListsUiState.Success ?: return emptyList()
    return (success.favourites + success.lists).distinctBy { it.id }
}
