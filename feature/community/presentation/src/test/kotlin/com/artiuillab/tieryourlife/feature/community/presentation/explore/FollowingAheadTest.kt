package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The first swipe to Following used to compose the tab, and start loading it, under the finger. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class FollowingAheadTest {

    @get:Rule
    val compose = createComposeRule()

    private val followed = PublishedListSummary(
        id = "f1",
        title = "Followed list",
        authorUid = "u-1",
        authorName = "mariia",
        category = "games",
        itemCount = 3,
        tierColors = emptyList(),
        updatedAtMillis = 0,
    )

    private var followingOpened = 0

    private fun show() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides WindowShape.of(411.dp, 891.dp)) {
                    CommunityHomeContent(
                        explore = ExploreUiState(feed = FeedPane(phase = FeedPhase.Ready)),
                        following = FollowingFeedUiState(feed = FeedPane(lists = listOf(followed), phase = FeedPhase.Ready)),
                        followingFeed = FeedCallbacks(onOpen = { followingOpened++ }),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun followingIsAskedForWhenCommunityOpens() {
        show()

        assertEquals(1, followingOpened)
    }

    @Test
    fun followingIsDrawnOffScreenOnceCommunityHasComeIn() {
        show()
        compose.onNodeWithTag(ExploreTestTags.card("f1")).assertDoesNotExist()

        compose.mainClock.advanceTimeBy(600)

        compose.onNodeWithTag(ExploreTestTags.card("f1")).assertExists()
    }
}
