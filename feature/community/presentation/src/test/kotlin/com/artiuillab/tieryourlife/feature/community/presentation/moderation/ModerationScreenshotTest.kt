package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.presentation.explore.STATUS_BAR_DP
import com.artiuillab.tieryourlife.feature.community.presentation.explore.SystemBarBands
import com.artiuillab.tieryourlife.feature.community.presentation.explore.THREE_BUTTON_BAR_DP
import com.artiuillab.tieryourlife.feature.community.presentation.explore.fakeSystemBars
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reports in each window, under a status bar and a three-button bar: one pane
 * upright, the queue beside the reported list sideways and on a tablet, and the
 * chips in the slim bar where a short window is one pane wide.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ModerationScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, dark: Boolean = false) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) {
                    Box {
                        ModerationScreenContent(
                            state = ModerationUiState.Ready(previewReports, looking = previewReports.first().listId),
                            looking = previewLooking,
                            onBack = {},
                        )
                        SystemBarBands(STATUS_BAR_DP, THREE_BUTTON_BAR_DP)
                    }
                }
            }
        }
        composeRule.runOnUiThread { composeRule.activity.fakeSystemBars(STATUS_BAR_DP, THREE_BUTTON_BAR_DP) }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun portrait() = capture("moderation_portrait")

    @Test
    @Config(qualifiers = PORTRAIT)
    fun portrait_dark() = capture("moderation_portrait_dark", dark = true)

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun landscape_queueBesideTheList() = capture("moderation_landscape")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun landscape_dark() = capture("moderation_landscape_dark", dark = true)

    @Test
    @Config(qualifiers = TABLET)
    fun tablet_queueBesideTheList() = capture("moderation_tablet")

    @Test
    @Config(qualifiers = TABLET)
    fun tablet_dark() = capture("moderation_tablet_dark", dark = true)

    @Test
    @Config(qualifiers = SHORT_ONE_PANE)
    fun shortAndOnePaneWide_theChipsRideInTheBar() = capture("moderation_short_one_pane")

    private companion object {
        const val PORTRAIT = "w411dp-h891dp-xhdpi"
        const val LANDSCAPE = "w891dp-h411dp-land-xhdpi"
        const val TABLET = "w1280dp-h800dp-land-xhdpi"
        const val SHORT_ONE_PANE = "w720dp-h400dp-land-xhdpi"
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
