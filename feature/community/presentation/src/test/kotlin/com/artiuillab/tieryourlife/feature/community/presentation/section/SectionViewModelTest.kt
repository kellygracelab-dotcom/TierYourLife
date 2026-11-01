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
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionLookup
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SectionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val zelda = Topic(id = "z", name = "Zelda", section = ListCategory.Games.id, status = TopicStatus.Approved, followerCount = 5)
    private val pokemon = Topic(id = "p", name = "Pokémon", section = ListCategory.Games.id, status = TopicStatus.Approved, followerCount = 9)

    private inner class Community(
        val followFails: Boolean = false,
        val lookups: Map<String, SectionLookup> = emptyMap(),
    ) : FakeCommunityRepository() {
        val asked = mutableListOf<Triple<String?, String?, FeedSort>>()
        val looked = mutableListOf<String>()
        val newestAsked = CompletableDeferred<Unit>()

        /** Held until the test has read the optimistic state; the refusal comes after. */
        val refusal = CompletableDeferred<Unit>()

        override suspend fun sections(): Result<List<Section>> =
            Result.success(Section.BUILT_IN.map { it.copy(listCount = 812) })

        override suspend fun section(id: String): Result<SectionLookup> {
            looked += id
            return Result.success(lookups[id] ?: SectionLookup.Unknown)
        }

        override suspend fun topics(query: String?, section: String?): Result<List<Topic>> = Result.success(listOf(zelda, pokemon))

        override suspend fun feed(
            category: String?,
            query: String?,
            author: String?,
            after: String?,
            sort: FeedSort,
            following: Boolean,
            topic: String?,
        ): Result<CommunityPage> {
            asked += Triple(category, topic, sort)
            if (sort == FeedSort.Recent) newestAsked.complete(Unit)
            return Result.success(CommunityPage(listOf(summary(topic ?: "all", ListCategory.Games.id))))
        }

        override suspend fun followSection(section: String): Result<Unit> {
            if (!followFails) return Result.success(Unit)
            refusal.await()
            return Result.failure(IOException("offline"))
        }

        override suspend fun followTopic(id: String): Result<Unit> =
            if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)
    }

    private val made = mutableListOf<SectionViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(
        community: FakeCommunityRepository,
        memory: SectionSortMemory = SectionSortMemory(),
        sectionId: String = "games",
    ) = SectionViewModel(
        savedStateHandle = SavedStateHandle(mapOf("sectionId" to sectionId)),
        community = community,
        cache = FakeCommunityFeedCache(),
        network = FakeCommunityNetwork(),
        preferences = FakeAppPreferences(),
        clock = FakeCommunityClock(),
        sortMemory = memory,
        catalog = sectionCatalogFor(community),
    ).also { made += it }

    @Test
    fun theSectionOpens_withItsCounts_andItsTopicsMostFollowedFirst() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.stats != null && it.topics.isNotEmpty() && it.feed.phase == FeedPhase.Ready }

        assertEquals(ListCategory.Games.id, state.sectionId)
        assertEquals(812, state.stats?.listCount)
        assertEquals(listOf("p", "z"), state.topics.map { it.id })
        assertEquals(Triple(ListCategory.Games.id, null, FeedSort.Popular), community.asked.first())
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

        assertEquals(FeedSort.Recent, memory.sortOf(ListCategory.Games.id))
        assertTrue(Triple(ListCategory.Games.id, null, FeedSort.Recent) in community.asked)
    }

    @Test
    fun followingTheSection_goesBackWhenRefused() = runBlocking {
        val community = Community(followFails = true)
        val viewModel = viewModel(community)
        viewModel.open()
        viewModel.state.firstWithin { it.stats != null }

        viewModel.toggleSectionFollow()
        // The screen state is combined from several flows, so it is awaited rather than read at once.
        viewModel.state.firstWithin { it.stats?.following == true }

        community.refusal.complete(Unit)
        assertEquals(CommunityNotice.FollowFailed, withTimeout(5_000) { viewModel.notices.first() })
        viewModel.state.firstWithin { it.stats?.following == false }
        Unit
    }

    @Test
    fun aSectionNobodyHereKnows_isDrawnAsOther_untilTheServerSaysWhatItIs() = runBlocking {
        val cars = Section(id = "cars", name = "Cars", listCount = 41)
        val community = Community(lookups = mapOf("cars" to SectionLookup.Found(cars)))
        val viewModel = viewModel(community, sectionId = "cars")
        assertEquals(Section(id = "cars"), viewModel.state.value.section)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.section.name == "Cars" }

        assertEquals(listOf("cars"), community.looked)
        assertEquals(41, state.stats?.listCount)
        assertTrue(community.asked.all { it.first == "cars" })
    }

    @Test
    fun aLinkToAMergedSection_opensTheSectionItBecame() = runBlocking {
        val community = Community(lookups = mapOf("autos" to SectionLookup.Merged("games")))
        val viewModel = viewModel(community, sectionId = "autos")

        viewModel.open()
        val state = viewModel.state.firstWithin { it.sectionId == ListCategory.Games.id && it.feed.phase == FeedPhase.Ready }

        assertEquals(ListCategory.Games.id, state.section.id)
        assertTrue(Triple(ListCategory.Games.id, null, FeedSort.Popular) in community.asked)
    }
}
