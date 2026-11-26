package com.artiuillab.tieryourlife.core.theme.layout

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** On a real window: a solid band exactly over three buttons, and nothing over gesture navigation. */
@RunWith(AndroidJUnit4::class)
class ThreeButtonBandTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theBand_coversThreeButtons_andNothingElse() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
        composeRule.setContent {
            TierYourLifeTheme {
                Box(Modifier.fillMaxSize().testTag(FRAME)) { ThreeButtonBand() }
            }
        }
        composeRule.waitForIdle()

        val insets = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
        val buttons = (insets?.getInsets(WindowInsetsCompat.Type.tappableElement())?.bottom ?: 0) > 0
        val barPx = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val bands = composeRule.onAllNodesWithTag(THREE_BUTTON_BAND).fetchSemanticsNodes().size

        if (!buttons || barPx == 0) {
            assertEquals("gesture navigation keeps the edge", 0, bands)
            return
        }
        assertEquals(1, bands)
        val bar = with(Density(composeRule.activity)) { barPx.toDp() }
        val frame = composeRule.onNodeWithTag(FRAME).getBoundsInRoot()
        val band = composeRule.onNodeWithTag(THREE_BUTTON_BAND).getBoundsInRoot()
        assertClose("the band is as tall as the bar", bar, band.bottom - band.top)
        assertClose("the band ends at the window's bottom", frame.bottom, band.bottom)
        assertClose("the band spans the width", frame.right - frame.left, band.right - band.left)
    }

    private fun assertClose(message: String, expected: Dp, actual: Dp) {
        assertTrue("$message: expected $expected, was $actual", abs((expected - actual).value) <= 1f)
    }

    private companion object {
        const val FRAME = "band_frame"
    }
}
