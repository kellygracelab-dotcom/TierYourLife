package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The pulse used to be a value read by the tab that drew the skeletons, so
 * both feed tabs composed again on every frame, sixty times a second, for as
 * long as they were on screen; a first swipe to Following paid for both.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SkeletonPulseTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aPulsingSkeletonRedrawsWithoutComposingItsScreenAgain() {
        var compositions = 0
        val seen = mutableSetOf<Float>()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val alpha = rememberSkeletonAlpha(running = true)
            SideEffect { compositions++ }
            Box(
                Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        this.alpha = alpha()
                        seen += this.alpha
                    },
            )
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()

        assertEquals(1, compositions)
        assertTrue("the pulse still moves: $seen", seen.size > 1)
    }

    @Test
    fun withNothingLoadingThereIsNoPulse() {
        var alpha: () -> Float = { 0f }
        compose.mainClock.autoAdvance = false
        compose.setContent { alpha = rememberSkeletonAlpha(running = false) }
        compose.mainClock.advanceTimeBy(700)

        assertEquals(1f, alpha())
    }
}
