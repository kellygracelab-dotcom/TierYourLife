package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-xhdpi")
class FollowingReturnTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun returningFromAList_keepsFollowingAndItsScrolledPosition() {
        var opened by mutableStateOf(false)
        val lists = List(60) { index ->
            PublishedListSummary(id = "f$index", title = "List $index", authorUid = "author", authorName = "Olena", category = "games", itemCount = 6, updatedAtMillis = 0)
        }
        compose.setContent {
            TierYourLifeTheme {
                val kept = rememberSaveableStateHolder()
                if (opened) {
                    TextButton(onClick = { opened = false }, modifier = Modifier.testTag("return")) { Text("Back") }
                } else {
                    kept.SaveableStateProvider("community") {
                        CommunityHomeContent(
                            explore = ExploreUiState(feed = FeedPane(phase = FeedPhase.Ready)),
                            following = FollowingFeedUiState(
                                following = Following(emptyList(), emptyList(), listOf(FollowedSection("games"))),
                                feed = FeedPane(lists = lists, phase = FeedPhase.Ready),
                            ),
                            destinations = CommunityDestinations(onOpenList = { opened = true }),
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag(ExploreTestTags.TAB_FOLLOWING).performClick()
        compose.onNodeWithTag(ExploreTestTags.FOLLOWING_FEED).performScrollToIndex(21)
        val tag = ExploreTestTags.card("f20")
        val before = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag(tag).performClick()
        compose.onNodeWithTag("return").performClick()
        compose.onNodeWithTag(ExploreTestTags.TAB_FOLLOWING).assertIsSelected()
        val after = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 1f)
    }
}
