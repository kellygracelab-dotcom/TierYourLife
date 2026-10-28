package com.artiuillab.tieryourlife.feature.community.presentation.followfeed

import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.summary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class FollowingFeedViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val pokemon = Topic(id = "t1", name = "Pokémon", section = ListCategory.Games.id, status = TopicStatus.Approved)

    @Test
    fun aFollowedPerson_needsNoLine_thenATopic_thenASection() {
        val following = Following(
            authors = listOf(FollowedAuthor("ann", "Ann", null)),
            topics = listOf(pokemon),
            sections = listOf(FollowedSection(ListCategory.Food.id)),
        )

        assertNull(reasonFor(summary("1", ListCategory.Food.id, author = "ann", topicId = "t1"), following))
        assertEquals(FeedReason.InTopic("Topic t1"), reasonFor(summary("2", ListCategory.Food.id, topicId = "t1"), following))
        assertEquals(FeedReason.InSection(ListCategory.Food.id), reasonFor(summary("3", ListCategory.Food.id), following))
        assertNull(reasonFor(summary("4", ListCategory.Games.id), following))
    }

    private inner class Community(
        var following: Following = Following(emptyList(), emptyList()),
        val mine: List<Topic> = emptyList(),
        val followFails: Boolean = false,
    ) : FakeCommunityRepository() {
        val feeds = mutableListOf<Pair<Boolean, FeedSort>>()

        override suspend fun following(): Result<Following> = Result.success(following)

        override suspend fun myTopics(): Result<List<Topic>> = Result.success(mine)

        override suspend fun feed(
            category: String?,
            query: String?,
            author: String?,
            after: String?,
            sort: FeedSort,
            following: Boolean,
            topic: String?,
        ): Result<CommunityPage> {
            feeds += following to sort
            val nobody = this.following.sections.isEmpty() && this.following.authors.isEmpty()
            return Result.success(CommunityPage(if (nobody) emptyList() else listOf(summary("x", ListCategory.Food.id)), followingNobody = nobody))
        }

        override suspend fun followSection(section: String): Result<Unit> {
            if (followFails) return Result.failure(IOException("offline"))
            following = following.copy(sections = following.sections + FollowedSection(section))
            return Result.success(Unit)
        }
    }

    private val made = mutableListOf<FollowingFeedViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(community: FakeCommunityRepository) =
        FollowingFeedViewModel(community, FakeCommunityFeedCache(), FakeCommunityNetwork(), FakeAppPreferences(), FakeCommunityClock(), sectionCatalogFor(community)).also { made += it }

    @Test
    fun followingNothing_offersTheSections_andOneTapMakesItAFeed() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)
        viewModel.open()
        assertTrue(viewModel.state.firstWithin { it.following != null && it.feed.phase == FeedPhase.Ready }.followsNothing)

        viewModel.quickFollow(ListCategory.Food.id)
        viewModel.state.firstWithin { ListCategory.Food.id in it.quickFollowed }

        val after = viewModel.state.firstWithin { it.feed.lists.isNotEmpty() }
        assertFalse(after.followsNothing)
        assertEquals(listOf(true to FeedSort.Recent), community.feeds.distinct())
    }

    @Test
    fun aRefusedQuickFollow_putsThePlusBack() = runBlocking {
        val viewModel = viewModel(Community(followFails = true))
        viewModel.open()
        viewModel.state.firstWithin { it.following != null }

        viewModel.quickFollow(ListCategory.Sport.id)

        assertEquals(CommunityNotice.FollowFailed, withTimeout(5_000) { viewModel.notices.first() })
        viewModel.state.firstWithin { ListCategory.Sport.id !in it.quickFollowed }
        Unit
    }

    @Test
    fun onlyThisPersonsWaitingTopics_standInTheStrip() = runBlocking {
        val waiting = Topic(id = "w", name = "Shiny hunts", section = null, status = TopicStatus.Pending)
        val turnedDown = Topic(id = "r", name = "Nope", section = null, status = TopicStatus.Rejected)
        val viewModel = viewModel(Community(following = Following(listOf(FollowedAuthor("ann", "Ann", null)), emptyList()), mine = listOf(waiting, turnedDown, pokemon)))

        viewModel.open()

        assertEquals(listOf("w"), viewModel.state.firstWithin { it.waiting.isNotEmpty() }.waiting.map { it.id })
    }
}
