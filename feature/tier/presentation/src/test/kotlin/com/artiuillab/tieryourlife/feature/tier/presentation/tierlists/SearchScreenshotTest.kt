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

/**
 * Searching Your lists (§2.3), each swiped up once the results are in: with the
 * keyboard up the slim bar holds the field in place; with it put away the bar
 * leaves with the results, which stay.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = LANDSCAPE)
class SearchScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, keyboardDp: Int = 0, swipe: Boolean = true, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) {
                    Box {
                        content()
                        SystemBarBands(STATUS_BAR_DP, GESTURE_BAR_DP, keyboardDp)
                    }
                }
            }
        }
        composeRule.runOnUiThread { composeRule.activity.fakeSystemBars(STATUS_BAR_DP, GESTURE_BAR_DP, keyboardDp) }
        composeRule.waitForIdle()
        if (swipe) {
            composeRule.onNodeWithTag(TierListsTestTags.LISTS).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private fun searching(query: String, found: Int = FOUND) = TierListsUiState.Success(
        lists = (1..found).map { n -> previewTierLists[n % previewTierLists.size].copy(id = n.toLong(), title = "Films of ${1990 + n}") },
        totalListCount = found + 6,
        rankedCount = 24,
        mode = HomeMode.Searching(query),
    )

    @Test
    fun landscape_keyboardUp_theBarHolds() = capture("search_landscape_keyboard_up", keyboardDp = KEYBOARD_DP) {
        TierListsScreenContent(searching("films"), onTierListClick = {})
    }

    @Test
    fun landscape_keyboardDown_theBarLeavesAndTheResultsStay() = capture("search_landscape_keyboard_down") {
        TierListsScreenContent(searching("films"), onTierListClick = {})
    }

    @Test
    fun landscape_keyboardDown_dark() = capture("search_landscape_keyboard_down_dark", dark = true) {
        TierListsScreenContent(searching("films"), onTierListClick = {})
    }

    @Test
    fun landscape_nothingCalledThat() = capture("search_landscape_empty", swipe = false) {
        TierListsScreenContent(searching("sushi", found = 0), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun portrait() = capture("search_portrait", swipe = false) {
        TierListsScreenContent(searching("films"), onTierListClick = {})
    }

    private companion object {
        const val FOUND = 14
        const val KEYBOARD_DP = 200
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}

private const val LANDSCAPE = "w891dp-h411dp-land-xhdpi"
private const val PORTRAIT = "w411dp-h891dp-xhdpi"
