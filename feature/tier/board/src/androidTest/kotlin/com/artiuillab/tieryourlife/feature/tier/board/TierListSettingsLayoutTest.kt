package com.artiuillab.tieryourlife.feature.tier.board

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Where the list settings page puts its rows: what people came for at the top, and nothing under the system bar at the bottom. */
@RunWith(AndroidJUnit4::class)
class TierListSettingsLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** [height]: a window short enough that the page has to scroll, as on a phone held sideways. */
    private fun open(height: Dp? = null) {
        // Drawn behind the system bars, as the app is, so the page has to keep clear of them itself.
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
        composeRule.setContent {
            TierYourLifeTheme {
                Box(Modifier.let { if (height != null) it.height(height) else it }.testTag(FRAME)) {
                    TierListSettingsScreenContent(
                        list = previewTierList,
                        onBack = {},
                        onAddTier = { _, _, _, _ -> },
                        onSetDisplayMode = {},
                    )
                }
            }
        }
    }

    @Test
    fun newTier_isTheFirstRow_andShowsWithoutScrolling() {
        open()

        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).assertIsDisplayed()
        val newTierTop = composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).getBoundsInRoot().top
        val publishTop = composeRule.onNodeWithTag(TierDetailTestTags.PUBLIC_SWITCH).getBoundsInRoot().top
        assertTrue("New tier stands above Publish", newTierTop < publishTop)
    }

    @Test
    fun theLastRow_endsAboveTheNavigationBar() {
        open(height = 360.dp)

        // To the very end, as a thumb would: scrolling a row into view stops as soon as it shows.
        repeat(SWIPES) { composeRule.onNodeWithTag(FRAME).performTouchInput { swipeUp() } }
        composeRule.waitForIdle()

        val insets = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
        val navigationBarPx = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val density = Density(composeRule.activity)
        val navigationBar = with(density) { navigationBarPx.toDp() }
        val rootBottom = composeRule.onNodeWithTag(FRAME).getBoundsInRoot().bottom
        val lastBottom = composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_RANKED).getBoundsInRoot().bottom
        // A device without a navigation bar has nothing to keep clear of; the faked-bar screenshots cover it there.
        assumeTrue("this device shows a navigation bar", navigationBarPx > 0)
        assertTrue(
            "the last row ($lastBottom) ends above the navigation bar (${rootBottom - navigationBar})",
            lastBottom <= rootBottom - navigationBar,
        )
    }

    private companion object {
        const val FRAME = "settings_frame"
        const val SWIPES = 6
    }
}
