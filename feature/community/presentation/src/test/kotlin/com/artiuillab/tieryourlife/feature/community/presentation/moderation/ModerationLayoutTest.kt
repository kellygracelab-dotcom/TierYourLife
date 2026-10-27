package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Where the chips live: the first thing the queue scrolls, never a pinned row; in the bar where a short window has room. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ModerationLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var refreshed = 0

    private fun setScreen() {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) {
                    ModerationScreenContent(
                        state = ModerationUiState.Ready(manyReports),
                        onBack = {},
                        onRetry = { refreshed++ },
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    fun upright_theChipsScrollAwayWithTheQueue() {
        setScreen()
        composeRule.onNodeWithTag(ModerationTestTags.tab(ModerationTab.Reports)).assertIsDisplayed()

        repeat(2) { composeRule.onNodeWithTag(ModerationTestTags.SCREEN).performTouchInput { swipeUp() } }

        composeRule.onNodeWithTag(ModerationTestTags.tab(ModerationTab.Reports)).assertIsNotDisplayed()
        composeRule.onNodeWithTag(ModerationTestTags.BACK).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w720dp-h400dp-land-xhdpi")
    fun shortAndOnePaneWide_theChipsAreInTheBar() {
        setScreen()
        val chip = composeRule.onNodeWithTag(ModerationTestTags.tab(ModerationTab.Reports)).getBoundsInRoot()
        val back = composeRule.onNodeWithTag(ModerationTestTags.BACK).getBoundsInRoot()

        assertTrue("the chip shares the bar's row", chip.top < back.bottom && chip.bottom > back.top)
        assertTrue("the bar is the slim one", back.bottom <= 48.dp)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    fun theBarSaysHowManyAreOpen_andItsOverflowAsksAgain() {
        setScreen()
        val open = InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(R.plurals.moderation_open_count, manyReports.size, manyReports.size)
        composeRule.onNodeWithText(open).assertIsDisplayed()
        composeRule.onNodeWithTag(ModerationTestTags.MORE).performClick()
        composeRule.onNodeWithTag(ModerationTestTags.REFRESH).performClick()

        composeRule.runOnIdle { assertEquals(1, refreshed) }
    }

    private val manyReports = (1..8).map { n -> previewReports[n % previewReports.size].copy(listId = "$n", listTitle = "Reported list $n") }
}
