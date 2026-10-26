package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.lifecycle.SavedStateHandle
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
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStats
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.summary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SectionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val zelda = Topic(id = "z", name = "Zelda", section = ListCategory.Games, status = TopicStatus.Approved, followerCount = 5, listCount = 3)
    private val pokemon = Topic(id = "p", name = "Pokémon", section = ListCategory.Games, status = TopicStatus.Approved, followerCount = 9, listCount = 7)

    private inner class Community(val followFails: Boolean = false) : FakeCommunityRepository() {
        val asked = mutableListOf<Triple<ListCategory?, String?, FeedSort>>()
        val newestAsked = CompletableDeferred<Unit>()

        override suspend fun sections(): Result<List<SectionStats>> =
            Result.success(ListCategory.entries.map { SectionStats(it, listCount = 812, followerCount = 3410) })

        override suspend fun topics(query: String?, section: ListCategory?): Result<List<Topic>> = Result.success(listOf(zelda, pokemon))

        override suspend fun feed(
            category: ListCategory?,
            query: String?,
            author: String?,
            after: String?,
            sort: FeedSort,
            following: Boolean,
            topic: String?,
        ): Result<CommunityPage> {
            asked += Triple(category, topic, sort)
            if (sort == FeedSort.Recent) newestAsked.complete(Unit)
            return Result.success(CommunityPage(listOf(summary(topic ?: "all", ListCategory.Games))))
        }

        override suspend fun followSection(section: ListCategory): Result<Unit> =
            if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)

        override suspend fun followTopic(id: String): Result<Unit> =
            if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)
    }

    private val made = mutableListOf<SectionViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(community: FakeCommunityRepository, topic: String? = null, memory: SectionSortMemory = SectionSortMemory()) = SectionViewModel(
        savedStateHandle = SavedStateHandle(mapOf("sectionId" to "games", "topicId" to topic)),
        community = community,
        cache = FakeCommunityFeedCache(),
        network = FakeCommunityNetwork(),
        preferences = FakeAppPreferences(),
        clock = FakeCommunityClock(),
        sortMemory = memory,
    ).also { made += it }

    @Test
    fun theSectionOpens_withItsCounts_andItsTopicsMostFollowedFirst() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.stats != null && it.topics.isNotEmpty() && it.feed.phase == FeedPhase.Ready }

        assertEquals(ListCategory.Games, state.section)
        assertEquals(812, state.stats?.listCount)
        assertEquals(listOf("p", "z"), state.topics.map { it.id })
        assertEquals(Triple(ListCategory.Games, null, FeedSort.Popular), community.asked.first())
    }

    @Test
    fun choosingATopic_filtersTheFeedInPlace_andAllClearsIt() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)
        viewModel.open()
        viewModel.state.firstWithin { it.topics.isNotEmpty() }

        viewModel.selectTopic(pokemon)
        val filtered = viewModel.state.firstWithin { it.feed.lists.map { l -> l.id } == listOf("p") }
        assertEquals("p", filtered.selectedTopic?.id)

        viewModel.selectTopic(null)
        val all = viewModel.state.firstWithin { it.selectedTopic == null && it.feed.lists.map { l -> l.id } == listOf("all") }
        assertEquals(null, all.selectedTopic)
    }

    @Test
    fun aTopicInTheRoute_opensChosen() = runBlocking {
        val viewModel = viewModel(Community(), topic = "z")

        viewModel.open()

        assertEquals("z", viewModel.state.firstWithin { it.selectedTopic != null }.selectedTopic?.id)
    }

    @Test
    fun theOrderIsRememberedForTheSection() = runBlocking {
        val memory = SectionSortMemory()
        val community = Community()
        val viewModel = viewModel(community, memory = memory)
        viewModel.open()
        viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        viewModel.selectSort(FeedSort.Recent)
        withTimeout(5_000) { community.newestAsked.await() }

        assertEquals(FeedSort.Recent, memory.sortOf(ListCategory.Games))
        assertTrue(Triple(ListCategory.Games, null, FeedSort.Recent) in community.asked)
    }

    @Test
    fun followingTheSection_goesBackWhenRefused() = runBlocking {
        val viewModel = viewModel(Community(followFails = true))
        viewModel.open()
        viewModel.state.firstWithin { it.stats != null }

        viewModel.toggleSectionFollow()
        assertTrue(viewModel.state.value.stats?.following == true)
        assertEquals(3411, viewModel.state.value.stats?.followerCount)

        assertEquals(CommunityNotice.FollowFailed, withTimeout(5_000) { viewModel.notices.first() })
        assertFalse(viewModel.state.value.stats?.following == true)
    }

    @Test
    fun theTopicRowFollowsTheTopic_notTheSection() = runBlocking {
        val viewModel = viewModel(Community())
        viewModel.open()
        viewModel.state.firstWithin { it.topics.isNotEmpty() && it.stats != null }
        viewModel.selectTopic(zelda)
        viewModel.state.firstWithin { it.selectedTopic != null }

        viewModel.toggleTopicFollow()

        val state = viewModel.state.firstWithin { it.selectedTopic?.following == true }
        assertEquals(6, state.selectedTopic?.followerCount)
        assertFalse(state.stats?.following == true)
    }
}
