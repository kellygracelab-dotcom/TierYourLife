package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.previewTierLists
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Your lists scrolled to the end, under a three-button bar and under gesture navigation's handle. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class SystemBarsScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * The screen under a status bar of [STATUS_BAR_DP] and a navigation bar of
     * [navigationDp], scrolled to its end by swiping [scrollTag] when given.
     */
    private fun capture(name: String, navigationDp: Int, scrollTag: String?, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) {
                    Box {
                        content()
                        SystemBarBands(STATUS_BAR_DP, navigationDp)
                    }
                }
            }
        }
        composeRule.runOnUiThread { composeRule.activity.fakeSystemBars(STATUS_BAR_DP, navigationDp) }
        composeRule.waitForIdle()
        if (scrollTag != null) {
            repeat(SWIPES) { composeRule.onNodeWithTag(scrollTag).performTouchInput { swipeUp() } }
            composeRule.waitForIdle()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private val boards = TierListsUiState.Success(previewTierLists, previewTierLists.size, 24)

    @Test
    fun lists_bottom_threeButton() = capture("bars_lists_bottom_three_button", THREE_BUTTON_BAR_DP, TierListsTestTags.LISTS) {
        TierListsScreenContent(boards, onTierListClick = {})
    }

    @Test
    fun lists_bottom_gesture() = capture("bars_lists_bottom_gesture", GESTURE_BAR_DP, TierListsTestTags.LISTS) {
        TierListsScreenContent(boards, onTierListClick = {})
    }

    private companion object {
        const val SWIPES = 8
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
