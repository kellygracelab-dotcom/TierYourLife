package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.ThemeColour
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A board in the ready colours (artboard 58): the tiers keep their own colours, the rest is the theme's. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ColourBoardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, hue: Int, dark: Boolean) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark, colour = ThemeColour.FromHue(hue)) {
                TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList))
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun matcha_light() = capture("board_colour_matcha", hue = 125, dark = false)

    @Test
    fun peach_light() = capture("board_colour_peach", hue = 42, dark = false)

    @Test
    fun sky_dark() = capture("board_colour_sky_dark", hue = 212, dark = true)

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
