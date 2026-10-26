package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * The app draws behind the system bars, so every screen keeps clear of them
 * itself, once, by one rule:
 *
 * - The status bar belongs to the screen, never to a bar that scrolls away.
 *   A header that leaves with the content leaves its inset behind
 *   ([BarredScreen] pads the container, not the bar), or whatever is pinned
 *   under the header slides up beneath the clock.
 * - The navigation bar belongs to the end of what scrolls: a list, grid,
 *   board or column ends with [systemBarsContentPadding], so its last item can
 *   scroll clear of the bar and nothing sits under it at rest.
 * - The sides are the app root's: it pads for the cutout and a sideways
 *   navigation bar once, so nothing below adds them again.
 *
 * Where a bar is absent the inset is zero, so none of this leaves a gap.
 */

/**
 * The padding a scrolling list needs at its end: [bottom] for whatever floats
 * over it (a button, a strip), plus the navigation bar under it. The keyboard
 * already covers the bar when it is up, so the bar is not added twice then.
 */
@Composable
fun systemBarsContentPadding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): PaddingValues = PaddingValues(start = start, top = top, end = end, bottom = bottom + navigationBarBottom())

/** How tall the navigation bar is at the bottom of the window, or zero: gesture navigation, a sideways phone, a cover screen. */
@Composable
fun navigationBarBottom(): Dp = WindowInsets.navigationBars.exclude(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
