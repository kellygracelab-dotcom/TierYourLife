package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.PageGone
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CachedFeed
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** The loading rules of §13, one at a time. */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedLoaderTest {

    private val minute = 60_000L
    private val clock = FakeCommunityClock(now = 100 * minute)
    private var answers = ArrayDeque<Result<CommunityPage>>()
    private val asked = mutableListOf<String?>()
    private fun summary(id: String) = PublishedListSummary(
        id = id,
        title = "List $id",
        authorUid = "u",
        authorName = "Author",
        category = ListCategory.Food.id,
        itemCount = 3,
        updatedAtMillis = 0,
    )

    private fun page(vararg ids: String, next: String? = null) = CommunityPage(ids.map(::summary), nextCursor = next)

    private fun TestScope.loader(cache: FakeCommunityFeedCache = FakeCommunityFeedCache()) = FeedLoader(
        key = "explore",
        scope = this,
        cache = cache,
        clock = clock,
    ) { after ->
        asked += after
        answers.removeFirstOrNull() ?: Result.failure(IOException("nothing set up"))
    }

    // A's lists, "Your list" on A's anonymous ones included, must be gone before B sees the tab.
    @Test
    fun somebodyElseSignedInMeanwhile_startsFromNothingAndAsksForThem() = runTest {
        val cache = object : FakeCommunityFeedCache() {
            var who: String? = "uid-a"
            private val keptFor = mutableMapOf<String, String?>()
            override fun reader(): String? = who
            override suspend fun read(key: String): CachedFeed? = super.read(key)?.takeIf { keptFor[key] == who }
            override suspend fun write(key: String, page: CommunityPage, fetchedAtMillis: Long) {
                keptFor[key] = who
                super.write(key, page, fetchedAtMillis)
            }
        }
        answers += Result.success(page("a-list"))
        val loader = loader(cache)
        loader.open()
        advanceUntilIdle()

        cache.who = "uid-b"
        answers += Result.success(page("b-list"))
        loader.open()

        assertEquals(FeedPane(), loader.pane.value)
        advanceUntilIdle()
        assertEquals(listOf("b-list"), loader.pane.value.lists.map { it.id })
        assertEquals(listOf<String?>(null, null), asked)
    }

    @Test
    fun theSamePersonComingBack_keepsWhatIsOnScreen() = runTest {
        answers += Result.success(page("a"))
        val loader = loader()
        loader.open()
        advanceUntilIdle()

        loader.open()
        advanceUntilIdle()

        assertEquals(listOf("a"), loader.pane.value.lists.map { it.id })
        assertEquals(1, asked.size)
    }

    // 409 CURSOR_GONE: the last list on the page went; asking from that cursor again is refused again.
    @Test
    fun aPageThatIsGone_startsTheFeedAgainFromTheTop() = runTest {
        answers += Result.success(page("a", "b", next = "b"))
        val loader = loader()
        loader.open()
        advanceUntilIdle()

        answers += Result.failure(PageGone())
        answers += Result.success(page("c", "a", next = "a"))
        loader.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(null, "b", null), asked)
        assertEquals(listOf("c", "a"), loader.pane.value.lists.map { it.id })
        assertTrue(loader.pane.value.canLoadMore)
    }

    @Test
    fun aPageThatFailsOtherwise_keepsItsCursorForTheNextTry() = runTest {
        answers += Result.success(page("a", "b", next = "b"))
        val loader = loader()
        loader.open()
        advanceUntilIdle()

        answers += Result.failure(IOException("offline"))
        loader.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(null, "b"), asked)
        assertEquals(listOf("a", "b"), loader.pane.value.lists.map { it.id })
    }

    @Test
    fun aKeptFeedShowsAtOnce_andIsNotAskedForWhileFresh() = runTest {
        val cache = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = clock.now - minute)))
        val loader = loader(cache)

        loader.open()
        advanceUntilIdle()

        assertEquals(listOf("a"), loader.pane.value.lists.map { it.id })
        assertEquals(FeedPhase.Ready, loader.pane.value.phase)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun aKeptFeedOlderThanFiveMinutes_isRefreshedInPlaceWhenNotScrolled() = runTest {
        val cache = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = clock.now - 6 * minute)))
        answers += Result.success(page("b", "a"))
        val loader = loader(cache)

        loader.open()
        advanceUntilIdle()

        assertEquals(listOf("b", "a"), loader.pane.value.lists.map { it.id })
        assertFalse(loader.pane.value.newListsWaiting)
        assertEquals(clock.now, cache.kept.getValue("explore").fetchedAtMillis)
    }

    @Test
    fun anAnswerWhileScrolledDown_isHeldBehindThePill() = runTest {
        val cache = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = clock.now - 6 * minute)))
        answers += Result.success(page("b", "a"))
        val loader = loader(cache)
        loader.onScrolledDown(true)

        loader.open()
        advanceUntilIdle()

        assertEquals(listOf("a"), loader.pane.value.lists.map { it.id })
        assertTrue(loader.pane.value.newListsWaiting)

        loader.applyHeld()
        assertEquals(listOf("b", "a"), loader.pane.value.lists.map { it.id })
        assertFalse(loader.pane.value.newListsWaiting)
    }

    @Test
    fun scrollingBackToTheTop_appliesWhatWasHeld() = runTest {
        val cache = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = clock.now - 6 * minute)))
        answers += Result.success(page("b"))
        val loader = loader(cache)
        loader.onScrolledDown(true)
        loader.open()
        advanceUntilIdle()

        loader.onScrolledDown(false)

        assertEquals(listOf("b"), loader.pane.value.lists.map { it.id })
    }

    @Test
    fun firstOpenWithoutAnythingKept_failsIntoTheErrorBlock() = runTest {
        answers += Result.failure(IOException("offline"))
        val loader = loader()

        loader.open()
        advanceUntilIdle()

        assertEquals(FeedPhase.Failed, loader.pane.value.phase)
    }

    @Test
    fun aFailureKeepsTheKeptFeed_andTheStripOnlyWhenItIsOverAnHourOld() = runTest {
        val recent = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = clock.now - 30 * minute)))
        answers += Result.failure(IOException("offline"))
        val young = loader(recent)
        young.open()
        advanceUntilIdle()
        assertEquals(listOf("a"), young.pane.value.lists.map { it.id })
        assertNull(young.pane.value.offlineSinceMillis)

        val oldAt = clock.now - 2 * 60 * minute
        val old = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = oldAt)))
        answers += Result.failure(IOException("offline"))
        val stale = loader(old)
        stale.open()
        advanceUntilIdle()
        assertEquals(oldAt, stale.pane.value.offlineSinceMillis)
    }

    @Test
    fun aFailedPull_saysSoInASnackbarAndNotTheStrip() = runTest {
        val keptAt = clock.now - 2 * 60 * minute
        val cache = FakeCommunityFeedCache(mapOf("explore" to CachedFeed(page("a"), fetchedAtMillis = keptAt)))
        answers += Result.success(page("a"))
        val loader = loader(cache)
        loader.open()
        advanceUntilIdle()

        answers += Result.failure(IOException("offline"))
        loader.pullToRefresh()
        advanceUntilIdle()

        assertEquals(FeedNotice.RefreshFailed(clock.now), loader.notices.first())
        assertNull(loader.pane.value.offlineSinceMillis)
        assertFalse(loader.pane.value.refreshing)
    }

    @Test
    fun theNetworkComingBack_retriesAFailedFeed_atMostOnceInTenSeconds() = runTest {
        answers += Result.failure(IOException("offline"))
        val loader = loader()
        loader.open()
        advanceUntilIdle()

        answers += Result.failure(IOException("still offline"))
        loader.onNetworkBack()
        advanceUntilIdle()
        loader.onNetworkBack()
        advanceUntilIdle()
        assertEquals(2, asked.size)

        clock.now += 10_000
        answers += Result.success(page("a"))
        loader.onNetworkBack()
        advanceUntilIdle()
        assertEquals(3, asked.size)
        assertEquals(FeedPhase.Ready, loader.pane.value.phase)
    }

    @Test
    fun moreLists_followTheCursor_withoutRepeats() = runTest {
        answers += Result.success(page("a", "b", next = "c1"))
        val loader = loader()
        loader.open()
        advanceUntilIdle()

        answers += Result.success(page("b", "c"))
        loader.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(null, "c1"), asked)
        assertEquals(listOf("a", "b", "c"), loader.pane.value.lists.map { it.id })
        assertFalse(loader.pane.value.canLoadMore)
    }

    @Test
    fun hiddenLists_leaveWithANoteWhereTheyWere() = runTest {
        val hidden = mutableSetOf<String>()
        answers += Result.success(page("a", "b"))
        val loader = FeedLoader("explore", this, FakeCommunityFeedCache(), clock, visible = { it.id !in hidden }) { answers.removeFirst() }
        loader.open()
        advanceUntilIdle()

        hidden += "a"
        loader.refilter(justHidden = mapOf("a" to true))
        assertEquals(listOf("a", "b"), loader.pane.value.lists.map { it.id })
        assertEquals(mapOf("a" to true), loader.pane.value.justHidden)

        loader.refilter(justHidden = emptyMap())
        assertEquals(listOf("b"), loader.pane.value.lists.map { it.id })
    }
}
