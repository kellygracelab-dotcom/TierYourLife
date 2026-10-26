package com.artiuillab.tieryourlife.feature.tier.board

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
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A board's last tier and the list settings' last row under a three-button
 * navigation bar, scrolled to the end: the tinted bands mark the bars, and
 * nothing may sit under them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
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

    @Test
    @Config(qualifiers = "w914dp-h411dp-land-xhdpi")
    fun board_lastTier_threeButton_landscape() = capture("bars_board_last_tier_landscape", THREE_BUTTON_BAR_DP, TierDetailTestTags.TIER_LIST) {
        TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList))
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    fun board_lastTier_threeButton_portrait() = capture("bars_board_last_tier_portrait", THREE_BUTTON_BAR_DP, TierDetailTestTags.TIER_LIST) {
        TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList), readOnly = true)
    }

    @Test
    @Config(qualifiers = "w914dp-h411dp-land-xhdpi")
    fun listSettings_lastRow_threeButton() = capture("bars_list_settings_bottom", THREE_BUTTON_BAR_DP, TierDetailTestTags.LIST_SETTINGS_SCREEN) {
        TierListSettingsScreenContent(list = previewTierList, onBack = {}, onAddTier = { _, _, _, _ -> }, onSetDisplayMode = {})
    }

    private companion object {
        const val SWIPES = 8
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
