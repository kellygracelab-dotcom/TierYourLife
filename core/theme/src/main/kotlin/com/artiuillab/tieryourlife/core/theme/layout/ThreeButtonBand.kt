package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Three-button navigation gets a solid bar: tiles and rows no longer show
 * through the buttons. Screens already end their content above the bar, so
 * the band only hides what scrolls under it. Gesture navigation keeps the edge.
 */
@Composable
fun BoxScope.ThreeButtonBand() {
    val buttons = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()
    val bar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    if (buttons == 0.dp || bar == 0.dp) return
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(bar)
            .background(MaterialTheme.colorScheme.background)
            .testTag(THREE_BUTTON_BAND),
    )
}

const val THREE_BUTTON_BAND = "three_button_band"
