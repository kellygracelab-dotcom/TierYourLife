package com.artiuillab.tieryourlife.feature.community.presentation.explore

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
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Community scrolled far enough that its bar has left: the tabs pinned under
 * it must stop at the status bar, not slide beneath it, and the feed's end
 * must clear the navigation bar.
 */
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

    private val lists = (1..12).map { n ->
        PublishedListSummary(
            id = "$n",
            title = "List number $n",
            authorUid = "u$n",
            authorName = "author $n",
            category = ListCategory.entries[n % ListCategory.entries.size].id,
            itemCount = 10 + n,
            tierColors = listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99"),
            updatedAtMillis = 0,
        )
    }

    private val explore = ExploreUiState(
        sections = Section.BUILT_IN.map { it.copy(listCount = 120) },
        feed = FeedPane(lists = lists, phase = FeedPhase.Ready),
    )

    @Test
    fun community_scrolled_threeButton() = capture("bars_community_scrolled", THREE_BUTTON_BAR_DP, ExploreTestTags.SCREEN) {
        CommunityHomeContent(explore, FollowingFeedUiState())
    }

    private companion object {
        const val SWIPES = 8
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
