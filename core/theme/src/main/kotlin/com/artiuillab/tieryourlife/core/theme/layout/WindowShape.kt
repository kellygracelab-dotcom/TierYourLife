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

    val hasRail: Boolean get() = this != Narrow

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

        fun of(width: Dp): WindowShape = when {
            width >= WideFrom -> Wide
            width >= MediumFrom -> Medium
            else -> Narrow
        }
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
