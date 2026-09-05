package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the window has, in the three amounts this app treats
 * differently. Named after what a layout may do, not after a device: a phone
 * in split view is not a tablet, and a folding phone is both within a second.
 */
enum class WindowShape {

    /**
     * A folding phone's cover screen: roughly square, camera cutout in a
     * corner. A surface that can show a board and cannot be used to build one.
     */
    Cover,

    /**
     * One column, and the navigation along the bottom or the top. Everything
     * from a Flip's cover to a phone in landscape.
     */
    Narrow,

    /** Room for a second thing beside the first. The rail arrives here because it takes less width than a row of tabs. */
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

        /** Material's thresholds, kept rather than invented: a window that behaves differently here behaves differently from the rest of the phone. */
        val MediumFrom: Dp = 600.dp
        val WideFrom: Dp = 840.dp

        /**
         * Small in both directions and roughly square: width alone is an
         * ordinary narrow phone, shape alone a phone held sideways. A Z Flip 7
         * cover is 352 x 339dp; a phone in landscape is nearer 0.45.
         */
        val CoverMaxHeight: Dp = 480.dp
        const val COVER_MIN_RATIO = 0.8f
        const val COVER_MAX_RATIO = 1.5f

        /** [shareable]: 360 x 400dp is both a cover and half a phone in split view, and a shared window is never a cover. */
        fun of(
            width: Dp,
            height: Dp = Dp.Unspecified,
            shareable: Boolean = false,
        ): WindowShape = when {
            width >= WideFrom -> Wide
            width >= MediumFrom -> Medium
            !shareable && height != Dp.Unspecified && isCover(width, height) -> Cover
            else -> Narrow
        }

        private fun isCover(width: Dp, height: Dp): Boolean =
            height <= CoverMaxHeight &&
                height >= width * COVER_MIN_RATIO &&
                height <= width * COVER_MAX_RATIO
    }
}

/** Read rather than passed down: a screen four levels deep needs the same answer as the top. */
val LocalWindowShape = staticCompositionLocalOf { WindowShape.Narrow }

val currentWindowShape: WindowShape
    @Composable get() = LocalWindowShape.current
