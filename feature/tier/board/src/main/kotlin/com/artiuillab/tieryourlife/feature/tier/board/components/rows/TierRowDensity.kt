package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape

/**
 * How big a board's rows are drawn. Height decides it: a window held
 * sideways gets lower rows, four tiers where three fitted; a tablet held as
 * one gets bigger cards, because it has the room and the reach.
 */
@Immutable
data class TierRowDensity(
    val bandWidth: Dp,
    /** A row with nothing in it. */
    val emptyHeight: Dp,
    val tileWidth: Dp,
    val tileHeight: Dp,
    val padding: Dp,
    val gap: Dp,
    /** A row a card hovers over says so with a ring rather than its dashes: in a short window the dashes were lost among the cards. */
    val ringOnHover: Boolean = false,
) {
    /** A row with cards in one line. */
    val occupiedHeight: Dp get() = tileHeight + padding * 2

    companion object {
        val Regular = TierRowDensity(bandWidth = 64.dp, emptyHeight = 84.dp, tileWidth = 44.dp, tileHeight = 64.dp, padding = 10.dp, gap = 8.dp)
        val Compact = TierRowDensity(
            bandWidth = 64.dp,
            emptyHeight = 56.dp,
            tileWidth = 40.dp,
            tileHeight = 58.dp,
            padding = 7.dp,
            gap = 6.dp,
            ringOnHover = true,
        )
        val Roomy = TierRowDensity(bandWidth = 80.dp, emptyHeight = 72.dp, tileWidth = 52.dp, tileHeight = 76.dp, padding = 10.dp, gap = 8.dp)

        /** The board's rule, shared with the skeleton that stands in for it: a tablet's width in either orientation gets the roomy cards. */
        fun of(short: Boolean, poolBeside: Boolean, width: Dp = Dp.Unspecified): TierRowDensity = when {
            short -> Compact
            poolBeside -> Roomy
            width != Dp.Unspecified && width >= WindowShape.MediumFrom -> Roomy
            else -> Regular
        }
    }
}

/** Read rather than passed: every row of one board is the same size, and the rows sit several calls below the screen that decides. */
val LocalTierRowDensity = staticCompositionLocalOf { TierRowDensity.Regular }
