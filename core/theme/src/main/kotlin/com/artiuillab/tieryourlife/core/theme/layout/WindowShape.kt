package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the window has, in the only three amounts this app treats
 * differently.
 *
 * Named after what a screen may do rather than after a device. There is no
 * "tablet" here on purpose: a phone in split view is not a tablet and a folding
 * phone is both within a second, and every layout that asked "is this a tablet"
 * got one of those wrong.
 */
enum class WindowShape {

    /**
     * A folding phone's cover screen. Barely taller than it is wide, which no
     * phone is, and with a camera cutout in one corner -- so it is not "a very
     * small phone", it is a surface that can show a board and cannot be used
     * to build one.
     */
    Cover,

    /**
     * One column, and the navigation along the bottom or the top. Everything
     * from a Flip's cover to a phone in landscape.
     */
    Narrow,

    /**
     * Room for a second thing beside the first, but not much of it. A small
     * tablet, or half of a large one. The rail arrives here because it takes
     * less width than a row of tabs, not because the window is grand.
     */
    Medium,

    /** A tablet held as a tablet. A list beside its detail fits without either being cramped. */
    Wide,

    ;

    val hasRail: Boolean get() = this == Medium || this == Wide

    /**
     * Whether anything here can be dragged, typed into or decided. The cover
     * shows; everything else waits for the phone to be opened.
     */
    val isGlanceable: Boolean get() = this == Cover

    /** Whether a list of boards can stand beside the board that is open. */
    val holdsTwoPanes: Boolean get() = this == Wide

    companion object {

        /**
         * Material's own thresholds, and worth keeping rather than inventing:
         * they are what every other Android app is measured against, so a
         * window that behaves differently here behaves differently from the
         * rest of the phone.
         */
        val MediumFrom: Dp = 600.dp
        val WideFrom: Dp = 840.dp

        /**
         * A cover is recognised by being small in both directions and roughly
         * square. Width alone cannot find it -- 352dp is an ordinary narrow
         * phone -- and shape alone would catch a small phone held sideways,
         * which is a phone and can be typed on.
         *
         * Measured off a Z Flip 7: 352 x 339dp, a ratio of 0.96. A phone in
         * landscape is nearer 0.45, and upright nearer 2.2.
         */
        val CoverMaxHeight: Dp = 480.dp
        const val COVER_MIN_RATIO = 0.8f
        const val COVER_MAX_RATIO = 1.5f

        fun of(width: Dp, height: Dp = Dp.Unspecified): WindowShape = when {
            width >= WideFrom -> Wide
            width >= MediumFrom -> Medium
            height != Dp.Unspecified && isCover(width, height) -> Cover
            else -> Narrow
        }

        private fun isCover(width: Dp, height: Dp): Boolean =
            height <= CoverMaxHeight &&
                height >= width * COVER_MIN_RATIO &&
                height <= width * COVER_MAX_RATIO
    }
}

/**
 * Read rather than passed down. A screen four levels deep needs the same answer
 * as the one at the top, and threading it through every signature in between
 * makes every one of them about layout.
 */
val LocalWindowShape = staticCompositionLocalOf { WindowShape.Narrow }

val currentWindowShape: WindowShape
    @Composable get() = LocalWindowShape.current
