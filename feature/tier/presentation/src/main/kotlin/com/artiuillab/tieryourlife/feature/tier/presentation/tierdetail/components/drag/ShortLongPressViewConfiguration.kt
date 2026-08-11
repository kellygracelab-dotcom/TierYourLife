package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag

import androidx.compose.ui.platform.ViewConfiguration

internal const val DRAG_LONG_PRESS_TIMEOUT_MILLIS = 150L

internal class ShortLongPressViewConfiguration(
    base: ViewConfiguration,
    override val longPressTimeoutMillis: Long,
) : ViewConfiguration by base
