package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeSectionStore
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CachedFeed
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

internal fun summary(id: String, category: String = ListCategory.Food.id, author: String = "u-$id", topicId: String? = null) = PublishedListSummary(
    id = id,
    title = "List $id",
    authorUid = author,
    authorName = "Author $id",
    category = category,
    itemCount = 4,
    updatedAtMillis = 0,
    topicId = topicId,
    topicName = topicId?.let { "Topic $it" },
)

class ExploreViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = FakeCommunityClock(now = 1_000_000L)

    private open inner class Community(
        val lists: List<PublishedListSummary> = listOf(summary("a"), summary("b")),
        val sectionsAnswer: Result<List<Section>> = Result.success(Section.BUILT_IN.map { it.copy(lists30d = ANSWERED) }),
        val followFails: Boolean = false,
    ) : FakeCommunityRepository() {
        val sorts = mutableListOf<FeedSort>()
        val queries = mutableListOf<String?>()
        val followed = CompletableDeferred<String>()

        override suspend fun feed(
            category: String?,
            query: String?,
            author: String?,
            after: String?,
            sort: FeedSort,
            following: Boolean,
            topic: String?,
        ): Result<CommunityPage> {
            sorts += sort
            queries += query
            return Result.success(CommunityPage(if (query == null) lists else lists.take(1)))
        }

        override suspend fun sections(): Result<List<Section>> = sectionsAnswer

        override suspend fun followSection(section: String): Result<Unit> {
            followed.complete(section)
            return if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)
        }
    }

    private val made = mutableListOf<ExploreViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(
        community: FakeCommunityRepository,
        cache: FakeCommunityFeedCache = FakeCommunityFeedCache(),
        kept: SectionDirectory? = null,
    ) = ExploreViewModel(
        community,
        cache,
        FakeCommunityNetwork(),
        FakeAppPreferences(),
        clock,
        sectionCatalogFor(community, FakeSectionStore(kept), clock),
    ).also { made += it }

    @Test
    fun opening_showsThePopularFeed_andTheServersSections() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready && it.answered() }

        assertEquals(listOf("a", "b"), state.feed.lists.map { it.id })
        assertEquals(FeedSort.Popular, community.sorts.first())
    }

    @Test
    fun sectionsTheServerCannotSend_leaveTheNine() = runBlocking {
        val viewModel = viewModel(Community(sectionsAnswer = Result.failure(IOException("not deployed"))))

        viewModel.open()
        val state = viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        assertEquals(ListCategory.entries.map { it.id }, state.sections.map { it.id })
    }

    @Test
    fun aKeptFeed_isWhatOpens() = runBlocking {
        val cache = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(CommunityPage(listOf(summary("kept"))), clock.now)))
        val viewModel = viewModel(Community(), cache)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        assertEquals(listOf("kept"), state.feed.lists.map { it.id })
    }

    @Test
    fun followingASection_answersAtOnce_andGoesBackWhenRefused() = runBlocking {
        val community = Community(followFails = true)
        val viewModel = viewModel(community)
        viewModel.open()
        viewModel.state.firstWithin { it.answered() }

        viewModel.follow(ListCategory.Food.id)
        withTimeout(5_000) { community.followed.await() }

        assertEquals(CommunityNotice.FollowFailed, withTimeout(5_000) { viewModel.notices.first() })
        viewModel.state.firstWithin { state -> !state.sections.first { it.id == ListCategory.Food.id }.following }
        Unit
    }

    @Test
    fun followingASection_saysSo() = runBlocking {
        val viewModel = viewModel(Community())
        viewModel.open()
        viewModel.state.firstWithin { it.answered() }

        viewModel.follow(ListCategory.Games.id)

        assertEquals(CommunityNotice.NowFollowing(ListCategory.Games.id), withTimeout(5_000) { viewModel.notices.first() })
        val counted = viewModel.state.firstWithin { state -> state.sections.first { it.id == ListCategory.Games.id }.following }
        // Followed sections come first, by the server's own rule.
        assertEquals(ListCategory.Games.id, counted.sections.first().id)
    }

    @Test
    fun theTiles_drawOnlyApprovedSectionsTheModeratorsHaveNotHidden_inTheServersOrder() = runBlocking {
        val answer = listOf(
            Section(id = "cars", name = "Cars"),
            Section(id = "boats", name = "Boats", hidden = true),
            Section(id = "kites", name = "Kites", status = SectionStatus.Pending),
        ) + Section.BUILT_IN
        val viewModel = viewModel(Community(sectionsAnswer = Result.success(answer)))

        viewModel.open()
        val state = viewModel.state.firstWithin { s -> s.sections.any { it.id == "cars" } }

        assertEquals(listOf("cars") + ListCategory.entries.map { it.id }, state.sections.map { it.id })
    }

    @Test
    fun keptSections_areWhatOpens_beforeTheServerSaysAnything() = runBlocking {
        val kept = SectionDirectory(sections = listOf(Section(id = "cars", name = "Cars")) + Section.BUILT_IN, fetchedAtMillis = clock.now)
        val viewModel = viewModel(Community(sectionsAnswer = Result.failure(IOException("offline"))), kept = kept)

        val state = viewModel.state.firstWithin { s -> s.sections.any { it.id == "cars" } }

        assertEquals("cars", state.sections.first().id)
    }

    @Test
    fun aSearch_asksTheServer_andClosingItLeavesTheFeed() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)
        viewModel.open()
        viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        viewModel.enterSearch()
        viewModel.updateQuery("ramen")
        val searched = viewModel.state.firstWithin { it.results.lists.isNotEmpty() }
        assertEquals(listOf("a"), searched.results.lists.map { it.id })
        assertTrue("ramen" in community.queries)

        viewModel.exitSearch()
        val closed = viewModel.state.firstWithin { it.query == null }
        assertEquals(listOf("a", "b"), closed.feed.lists.map { it.id })
    }

    @Test
    fun aSearch_findsTopicsToo_approvedOnesOnly() = runBlocking {
        val starWars = Topic(id = "sw", name = "Star Wars", section = ListCategory.Anime.id, status = TopicStatus.Approved)
        val waiting = Topic(id = "st", name = "Star Trek", section = null, status = TopicStatus.Pending)
        val community = object : Community() {
            override suspend fun topics(query: String?, section: String?): Result<List<Topic>> =
                Result.success(if (query == "star") listOf(starWars, waiting) else emptyList())
        }
        val viewModel = viewModel(community)
        viewModel.open()
        viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        withContext(Dispatchers.Main) {
            viewModel.enterSearch()
            viewModel.updateQuery("star")
        }

        val found = viewModel.state.firstWithin { it.results.topics.isNotEmpty() }
        assertEquals(listOf("sw"), found.results.topics.map { it.id })
    }

    @Test
    fun aFailedSearch_asksAgain_onRetry() = runBlocking {
        val community = object : Community() {
            var refuse = true

            override suspend fun feed(
                category: String?,
                query: String?,
                author: String?,
                after: String?,
                sort: FeedSort,
                following: Boolean,
                topic: String?,
            ): Result<CommunityPage> {
                if (query != null && refuse) {
                    refuse = false
                    queries += query
                    return Result.failure(IOException("offline"))
                }
                return super.feed(category, query, author, after, sort, following, topic)
            }
        }
        val viewModel = viewModel(community)
        viewModel.open()
        viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }
        viewModel.enterSearch()
        viewModel.updateQuery("ramen")
        viewModel.state.firstWithin { it.results.failed }

        viewModel.retrySearch()

        val found = viewModel.state.firstWithin { it.results.lists.isNotEmpty() }
        assertEquals(listOf("a"), found.results.lists.map { it.id })
        assertEquals(2, community.queries.count { it == "ramen" })
    }

    @Test
    fun hidingAList_leavesANoteWhereItWas() = runBlocking {
        val viewModel = viewModel(Community())
        viewModel.open()
        val ready = viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        viewModel.hideList(ready.feed.lists.first())

        assertEquals(mapOf("a" to false), viewModel.state.firstWithin { it.feed.justHidden.isNotEmpty() }.feed.justHidden)
    }
}

private const val ANSWERED = 3

private fun ExploreUiState.answered() = sections.any { it.lists30d == ANSWERED }
