package com.artiuillab.tieryourlife.feature.community.presentation.explore

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
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStats
import com.artiuillab.tieryourlife.feature.community.domain.repository.CachedFeed
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

internal fun summary(id: String, category: ListCategory = ListCategory.Food, author: String = "u-$id", topicId: String? = null) = PublishedListSummary(
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
        val sectionsAnswer: Result<List<SectionStats>> = Result.success(ListCategory.entries.map { SectionStats(it, newThisWeek = 3) }),
        val followFails: Boolean = false,
    ) : FakeCommunityRepository() {
        val sorts = mutableListOf<FeedSort>()
        val queries = mutableListOf<String?>()
        val followed = CompletableDeferred<ListCategory>()

        override suspend fun feed(
            category: ListCategory?,
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

        override suspend fun sections(): Result<List<SectionStats>> = sectionsAnswer

        override suspend fun followSection(section: ListCategory): Result<Unit> {
            followed.complete(section)
            return if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)
        }
    }

    private val made = mutableListOf<ExploreViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(community: FakeCommunityRepository, cache: FakeCommunityFeedCache = FakeCommunityFeedCache()) =
        ExploreViewModel(community, cache, FakeCommunityNetwork(), FakeAppPreferences(), clock).also { made += it }

    @Test
    fun opening_showsThePopularFeed_andTheNewLinesOnceCounted() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready && it.countsKnown }

        assertEquals(listOf("a", "b"), state.feed.lists.map { it.id })
        assertEquals(FeedSort.Popular, community.sorts.first())
        assertEquals(3, state.sections.first { it.section == ListCategory.Food }.newThisWeek)
    }

    @Test
    fun sectionsTheServerCannotCount_leaveTheTilesWithoutNewLines() = runBlocking {
        val viewModel = viewModel(Community(sectionsAnswer = Result.failure(IOException("not deployed"))))

        viewModel.open()
        val state = viewModel.state.firstWithin { it.feed.phase == FeedPhase.Ready }

        assertFalse(state.countsKnown)
        assertEquals(ListCategory.entries, state.sections.map { it.section })
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
        viewModel.state.firstWithin { it.countsKnown }

        viewModel.follow(ListCategory.Food)
        withTimeout(5_000) { community.followed.await() }

        assertEquals(CommunityNotice.FollowFailed, withTimeout(5_000) { viewModel.notices.first() })
        viewModel.state.firstWithin { state -> !state.sections.first { it.section == ListCategory.Food }.following }
        Unit
    }

    @Test
    fun followingASection_saysSo() = runBlocking {
        val viewModel = viewModel(Community())
        viewModel.open()
        viewModel.state.firstWithin { it.countsKnown }

        viewModel.follow(ListCategory.Games)

        assertEquals(CommunityNotice.NowFollowing(ListCategory.Games), withTimeout(5_000) { viewModel.notices.first() })
        val counted = viewModel.state.firstWithin { state -> state.sections.first { it.section == ListCategory.Games }.following }
        assertEquals(1, counted.sections.first { it.section == ListCategory.Games }.followerCount)
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
    fun aFailedSearch_asksAgain_onRetry() = runBlocking {
        val community = object : Community() {
            var refuse = true

            override suspend fun feed(
                category: ListCategory?,
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
