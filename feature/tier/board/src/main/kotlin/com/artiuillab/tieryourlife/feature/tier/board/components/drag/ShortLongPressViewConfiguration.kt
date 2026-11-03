package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.platform.ViewConfiguration

/** A finger held still this long lifts a card on a board that scrolls up and down; one moving sideways lifts it sooner. */
const val DRAG_LONG_PRESS_TIMEOUT_MILLIS = 100L

class ShortLongPressViewConfiguration(
    base: ViewConfiguration,
    override val longPressTimeoutMillis: Long,
) : ViewConfiguration by base
