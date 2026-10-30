package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape

/**
 * How big a board's rows are drawn. Height decides it: a window held
 * sideways gets lower rows, four tiers where three fitted; a tablet gets
 * bigger cards, because it has the room and the reach. A phone held upright
 * keeps the measures it always had.
 */
@Immutable
data class TierRowDensity(
    val bandWidth: Dp,
    val tileWidth: Dp,
    val tileHeight: Dp,
    val padding: Dp,
    val gap: Dp,
    /** Between one row and the next. */
    val rowGap: Dp,
    /** The band's letter and caption; unspecified keeps the theme's own. */
    val letterSize: TextUnit = TextUnit.Unspecified,
    val letterLineHeight: TextUnit = TextUnit.Unspecified,
    val captionSize: TextUnit = TextUnit.Unspecified,
    val captionLineHeight: TextUnit = TextUnit.Unspecified,
    /** "Drop items here" in an empty row while the pool has cards to drop. */
    val emptyHint: Boolean = false,
) {
    /** A row with cards in one line, and an empty row too: it is as tall as one holding a card. */
    val occupiedHeight: Dp get() = tileHeight + padding * 2

    companion object {
        val Regular = TierRowDensity(
            bandWidth = 64.dp,
            tileWidth = 44.dp,
            tileHeight = 64.dp,
            padding = 10.dp,
            gap = 8.dp,
            rowGap = 8.dp,
        )
        val Compact = TierRowDensity(
            bandWidth = 64.dp,
            tileWidth = 40.dp,
            tileHeight = 58.dp,
            padding = 7.dp,
            gap = 6.dp,
            rowGap = 6.dp,
            letterSize = 20.sp,
            letterLineHeight = 24.sp,
            captionSize = 9.sp,
            captionLineHeight = 11.sp,
            emptyHint = true,
        )
        val Roomy = TierRowDensity(
            bandWidth = 80.dp,
            tileWidth = 52.dp,
            tileHeight = 76.dp,
            padding = 10.dp,
            gap = 8.dp,
            rowGap = 8.dp,
            letterSize = 26.sp,
            letterLineHeight = 30.sp,
            captionSize = 10.sp,
            captionLineHeight = 13.sp,
            emptyHint = true,
        )

        /** Beside the pane of boards the band gives back a little of the width the pane took. */
        private val RoomyBesideIndex = Roomy.copy(bandWidth = 72.dp)

        /** The board's rule, shared with the skeleton that stands in for it. */
        fun of(short: Boolean, poolBeside: Boolean, width: Dp = Dp.Unspecified, indexBeside: Boolean = false): TierRowDensity {
            val tablet = poolBeside || (width != Dp.Unspecified && width >= WindowShape.MediumFrom)
            return when {
                short -> Compact
                tablet && indexBeside -> RoomyBesideIndex
                tablet -> Roomy
                else -> Regular
            }
        }
    }
}

/** Read rather than passed: every row of one board is the same size, and the rows sit several calls below the screen that decides. */
val LocalTierRowDensity = staticCompositionLocalOf { TierRowDensity.Regular }
