package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.platform.ViewConfiguration

const val DRAG_LONG_PRESS_TIMEOUT_MILLIS = 150L

class ShortLongPressViewConfiguration(
    base: ViewConfiguration,
    override val longPressTimeoutMillis: Long,
) : ViewConfiguration by base
