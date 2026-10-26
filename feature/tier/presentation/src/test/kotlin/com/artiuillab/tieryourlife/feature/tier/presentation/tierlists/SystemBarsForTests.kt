package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/** A status bar and a three-button navigation bar, as a phone reports them. */
internal const val STATUS_BAR_DP = 24
internal const val THREE_BUTTON_BAR_DP = 48

/** Gesture navigation's slim handle. */
internal const val GESTURE_BAR_DP = 16

/**
 * Hands the screen the insets a phone would, without a phone: Robolectric
 * draws no system bars, so a screenshot would otherwise never show what
 * sits under them.
 */
internal fun ComponentActivity.fakeSystemBars(statusDp: Int, navigationDp: Int) {
    val content = findViewById<ViewGroup>(android.R.id.content)
    val density = content.resources.displayMetrics.density
    val insets = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, (statusDp * density).roundToInt(), 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, (navigationDp * density).roundToInt()))
        .build()
    ViewCompat.dispatchApplyWindowInsets(content, insets)
}

/** The two bars drawn over the screen as tinted bands: anything under a band is under the bar. */
@Composable
internal fun SystemBarBands(statusDp: Int, navigationDp: Int) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(statusDp.dp).background(STATUS_TINT))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(navigationDp.dp).background(NAVIGATION_TINT))
    }
}

private val STATUS_TINT = Color(0x66B03A32)
private val NAVIGATION_TINT = Color(0x663C6E99)
