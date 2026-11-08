package com.artiuillab.tieryourlife.feature.community.presentation.explore

import android.view.KeyEvent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The grid and its enter-always header must restore together after opening a list. */
@RunWith(AndroidJUnit4::class)
class FeedScrollReturnTest {
    @get:Rule val compose = createComposeRule()

    @Test fun followingKeepsItsTabAndScrollAcrossRealBack() = probeReturn(following = true)

    @Test fun exploreKeepsItsTabAndScrollAcrossRealBack() = probeReturn(following = false)

    private fun probeReturn(following: Boolean) {
        val lists = List(35) { index ->
            PublishedListSummary(id = "f$index", title = "QA List $index", authorUid = "qa-author", authorName = "QA Owner", category = "games", itemCount = 6, updatedAtMillis = 0)
        }
        compose.setContent {
            TierYourLifeTheme {
                val nav = rememberNavController()
                NavHost(nav, "community") {
                    composable("community") {
                        CommunityHomeContent(
                            explore = ExploreUiState(feed = FeedPane(lists = lists, phase = FeedPhase.Ready)),
                            following = FollowingFeedUiState(
                                following = Following(emptyList(), emptyList(), listOf(FollowedSection("games"))),
                                feed = FeedPane(lists = lists, phase = FeedPhase.Ready),
                            ),
                            destinations = CommunityDestinations(onOpenList = { nav.navigate("detail") }),
                        )
                    }
                    composable("detail") {
                        TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.testTag("audit_detail")) { Text("QA Details") }
                    }
                }
            }
        }
        if (following) compose.onNodeWithTag(ExploreTestTags.TAB_FOLLOWING).performClick()
        val feed = compose.onNodeWithTag(if (following) ExploreTestTags.FOLLOWING_FEED else ExploreTestTags.SCREEN)
        feed.performScrollToIndex(21)
        val tag = ExploreTestTags.card("f20")
        val expanded = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        // A programmatic scroll never reaches the header's nested-scroll connection.
        feed.performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y - 60f), durationMillis = 700)
        }
        val before = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertTrue("The gesture must actually collapse the header", before.top < expanded.top - 1f)
        compose.onNodeWithTag(tag).performClick()
        compose.onNodeWithTag("audit_detail").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag(if (following) ExploreTestTags.TAB_FOLLOWING else ExploreTestTags.TAB_EXPLORE).assertIsSelected()
        val after = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 1f)
        feed.performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y + 60f), durationMillis = 700)
        }
        val expandedAgain = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals("The restored header still expands on the next scroll", expanded.top, expandedAgain.top, 1f)
    }
}
