package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A section's search and a topic's: the same search, asked of one place until its chip widens it. */
class ScopedSearchTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After fun stop() = scope.cancel()

    private class Asked(val category: String?, val topic: String?, val query: String?, val after: String?)

    private open class Community : FakeCommunityRepository() {
        val feeds = mutableListOf<Asked>()
        val topicSearches = mutableListOf<Pair<String?, String?>>()
        val misses = mutableListOf<String>()

        override suspend fun feed(
            category: String?,
            query: String?,
            author: String?,
            after: String?,
            sort: FeedSort,
            following: Boolean,
            topic: String?,
        ): Result<CommunityPage> {
            synchronized(feeds) { feeds += Asked(category, topic, query, after) }
            val id = listOfNotNull(category, topic).firstOrNull() ?: "everywhere"
            return Result.success(CommunityPage(lists = listOf(summary(id)), nextCursor = "next".takeIf { after == null }))
        }

        override suspend fun topics(query: String?, section: String?): Result<List<Topic>> {
            synchronized(topicSearches) { topicSearches += query to section }
            return Result.success(listOf(Topic(id = "t-${section ?: "all"}", name = "Naruto", section = section, status = TopicStatus.Approved)))
        }

        override suspend fun reportSearchMiss(query: String): Result<Unit> {
            misses += query
            return Result.success(Unit)
        }
    }

    @Test
    fun inASection_itAsksForThatSectionsTopicsAndLists_andTheChipWidensIt() = runBlocking {
        val community = Community()
        val search = ExploreSearch(community, scope, visible = { true }) { false }

        search.enter(SearchScope.Section("anime"))
        search.update("naruto")
        val here = search.results.firstWithin { it.lists.isNotEmpty() }
        assertEquals(listOf("anime"), here.lists.map { it.id })
        assertEquals(listOf("t-anime"), here.topics.map { it.id })
        assertEquals("anime", community.feeds.last().category)
        assertEquals("naruto" to "anime", community.topicSearches.last())

        search.widen()
        val everywhere = search.results.firstWithin { it.lists.singleOrNull()?.id == "everywhere" }
        assertEquals(null, search.within.value)
        assertEquals(listOf("t-all"), everywhere.topics.map { it.id })
        assertEquals(null, community.feeds.last().category)
    }

    @Test
    fun inATopic_itAsksOnlyForThatTopicsLists_andTheNextPageStaysThere() = runBlocking {
        val community = Community()
        val search = ExploreSearch(community, scope, visible = { true }) { false }

        search.enter(SearchScope.Topic("naruto"))
        search.update("best")
        val found = search.results.firstWithin { it.lists.isNotEmpty() }
        assertEquals(listOf("naruto"), found.lists.map { it.id })
        assertTrue("a topic's search finds no topics", found.topics.isEmpty())
        assertTrue(community.topicSearches.isEmpty())

        search.loadMore()
        search.results.firstWithin { !it.loadingMore && it.cursor == null }
        assertEquals("naruto", community.feeds.last().topic)
        assertEquals("next", community.feeds.last().after)
    }

    @Test
    fun closingTheSearch_forgetsWhereItLooked() {
        val search = ExploreSearch(Community(), scope, visible = { true }) { false }

        search.enter(SearchScope.Section("anime"))
        search.exit()

        assertEquals(null, search.within.value)
        assertEquals(null, search.query.value)
    }

    private companion object {
        fun summary(id: String) = PublishedListSummary(
            id = id, title = id, authorUid = "u", authorName = "a", category = "anime", itemCount = 1, updatedAtMillis = 0,
        )
    }
}
