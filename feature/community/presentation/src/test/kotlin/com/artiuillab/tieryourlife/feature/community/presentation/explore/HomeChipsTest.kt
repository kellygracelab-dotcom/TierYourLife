package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Community's chips, where Explore · Following used to be (design audit 16.11, option B). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-xhdpi")
class HomeChipsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun card(id: String, title: String) = PublishedListSummary(
        id = id,
        title = title,
        authorUid = "u-$id",
        authorName = "mariia",
        category = "games",
        itemCount = 3,
        tierColors = emptyList(),
        updatedAtMillis = 0,
    )

    private var followingOpened = 0
    private var newestOpened = 0
    private var popularOpened = 0
    private val openedTopics = mutableListOf<String>()
    private val openedSections = mutableListOf<String>()

    private fun show(explore: ExploreUiState = ExploreUiState(), following: FollowingFeedUiState = FollowingFeedUiState()) {
        compose.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides WindowShape.of(411.dp, 891.dp)) {
                    CommunityHomeContent(
                        explore = explore,
                        following = following,
                        exploreFeed = FeedCallbacks(onOpen = { popularOpened++ }),
                        newestFeed = FeedCallbacks(onOpen = { newestOpened++ }),
                        followingFeed = FeedCallbacks(onOpen = { followingOpened++ }),
                        destinations = CommunityDestinations(
                            onOpenTopic = { openedTopics += it.id },
                            onOpenSection = { openedSections += it },
                        ),
                    )
                }
            }
        }
    }

    private val feeds = ExploreUiState(
        feed = FeedPane(lists = listOf(card("p1", "Popular list")), phase = FeedPhase.Ready),
        newest = FeedPane(lists = listOf(card("n1", "Newest list")), phase = FeedPhase.Ready),
    )
    private val followed = FollowingFeedUiState(feed = FeedPane(lists = listOf(card("f1", "Followed list")), phase = FeedPhase.Ready))

    @Test
    fun communityOpensOnPopular_andAsksForFollowingAtOnce_butNotForNew() {
        show(feeds, followed)

        compose.onNodeWithTag(ExploreTestTags.chip(HomeChip.Popular)).assertIsSelected()
        compose.onNodeWithTag(ExploreTestTags.card("p1")).assertExists()
        assertEquals(1, popularOpened)
        assertEquals(1, followingOpened)
        assertEquals(0, newestOpened)
    }

    @Test
    fun theNewChip_asksForTheNewestLists_andShowsThemInstead() {
        show(feeds, followed)

        compose.onNodeWithTag(ExploreTestTags.chip(HomeChip.New)).performClick()

        compose.onNodeWithTag(ExploreTestTags.chip(HomeChip.New)).assertIsSelected()
        compose.onNodeWithTag(ExploreTestTags.chip(HomeChip.Popular)).assertIsNotSelected()
        compose.onNodeWithTag(ExploreTestTags.card("n1")).assertExists()
        compose.onNodeWithTag(ExploreTestTags.card("p1")).assertDoesNotExist()
        assertEquals(1, newestOpened)
    }

    @Test
    fun theFollowingChip_showsWhatIsFollowed_underTheSameSections() {
        show(feeds, followed)

        compose.onNodeWithTag(ExploreTestTags.chip(HomeChip.Following)).performClick()

        compose.onNodeWithTag(ExploreTestTags.FOLLOWING_FEED).assertExists()
        compose.onNodeWithTag(ExploreTestTags.card("f1")).assertExists()
        compose.onNodeWithTag(ExploreTestTags.tile(ListCategory.Anime.id)).assertExists()
    }

    @Test
    fun followingNothing_theFollowingChipOffersTheSectionsToPick() {
        show(feeds, FollowingFeedUiState(following = Following(emptyList(), emptyList(), emptyList())))

        compose.onNodeWithTag(ExploreTestTags.chip(HomeChip.Following)).performClick()

        compose.onNodeWithTag(ExploreTestTags.FOLLOWING_EMPTY).assertExists()
        compose.onNodeWithTag(ExploreTestTags.FOLLOW_PICKED).assertExists()
    }

    @Test
    fun eachChip_isAWholeFingersTarget() {
        show(feeds, followed)

        HomeChip.entries.forEach { compose.onNodeWithTag(ExploreTestTags.chip(it)).assertTouchHeightIsEqualTo(48.dp) }
    }

    @Test
    fun anUprightPhone_showsFourSections_andTheWayToTheRest() {
        show(feeds, followed)

        Section.BUILT_IN.filterNot { it.isOther }.forEachIndexed { index, section ->
            val tile = compose.onNodeWithTag(ExploreTestTags.tile(section.id))
            if (index < PHONE_SECTIONS) tile.assertExists() else tile.assertDoesNotExist()
        }
        compose.onNodeWithTag(ExploreTestTags.ALL_SECTIONS_TILE).assertExists()
    }

    @Test
    fun searchOnCommunity_saysItSearchesCommunityLists() {
        show(feeds, followed)

        compose.onNodeWithContentDescription("Search community lists").assertExists()
        compose.onNodeWithContentDescription("Search your lists").assertDoesNotExist()
    }

    @Test
    fun anEmptySearch_offersTheSectionsAndThePopularTopics() {
        val bleach = Topic(id = "bleach", name = "Bleach", section = ListCategory.Anime.id, status = TopicStatus.Approved)
        show(feeds.copy(query = "", popularTopics = listOf(bleach)), followed)

        compose.onNodeWithTag(ExploreTestTags.SEARCH_BROWSE_SECTIONS).assertExists()
        compose.onNodeWithTag(ExploreTestTags.browseSection(ListCategory.Food.id)).performClick()
        compose.onNodeWithTag(ExploreTestTags.browseTopic("bleach")).performClick()

        assertEquals(listOf(ListCategory.Food.id), openedSections)
        assertEquals(listOf("bleach"), openedTopics)
    }

    @Test
    fun anEmptySearch_withNoTopicsYet_offersTheSectionsAlone() {
        show(feeds.copy(query = ""), followed)

        compose.onNodeWithTag(ExploreTestTags.SEARCH_BROWSE_SECTIONS).assertExists()
        compose.onNodeWithTag(ExploreTestTags.SEARCH_BROWSE_TOPICS).assertDoesNotExist()
    }
}
