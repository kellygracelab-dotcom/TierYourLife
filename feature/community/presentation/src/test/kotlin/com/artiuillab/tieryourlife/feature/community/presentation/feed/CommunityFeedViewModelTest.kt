package com.artiuillab.tieryourlife.feature.community.presentation.feed

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CommunityFeedViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun aListHiddenOnAnotherScreen_isGoneWhenTheFeedComesBackIntoView() = runBlocking {
        val preferences = FakeAppPreferences()
        val community = FakeFeedRepository(
            feed = listOf(published("a", "Sci-fi films"), published("b", "Every A24 film")),
        )
        val viewModel = CommunityFeedViewModel(community, preferences)
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        // What opening the list and hiding it from in there leaves behind.
        preferences.hideList("a", "Sci-fi films")
        viewModel.refreshHidden()

        assertEquals(listOf("b"), shownIds(viewModel))
    }

    // The two refusals want opposite sentences, so the screen has to be able
    // to tell them apart before it says either.
    @Test
    fun whenPlayWillNotVouchForTheInstall_theFeedSaysSoRatherThanBlamingTheConnection() = runBlocking {
        val community = FakeFeedRepository(firstPageFails = AppUnverified())
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()

        val state = viewModel.state.firstWithin { it.feed is CommunityFeed.Unverified }
        assertEquals(CommunityFeed.Unverified, state.feed)
    }

    @Test
    fun whenTheFeedSimplyDidNotArrive_itStaysTheOneWithATryAgain() = runBlocking {
        val community = FakeFeedRepository(firstPageFails = IOException("offline"))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()

        val state = viewModel.state.firstWithin { it.feed is CommunityFeed.Failed }
        assertEquals(CommunityFeed.Failed, state.feed)
    }

    @Test
    fun comingBackWithNothingHidden_leavesTheFeedAlone() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "Sci-fi films")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.refreshHidden()

        assertEquals(listOf("a"), shownIds(viewModel))
    }

    // Undoing a hide happens in Settings, where the feed is not on screen and
    // what we hold has already had the card taken out of it.
    @Test
    fun aListPutBack_returnsToTheFeed() = runBlocking {
        val preferences = FakeAppPreferences()
        val community = FakeFeedRepository(
            feed = listOf(published("a", "Sci-fi films"), published("b", "Every A24 film")),
        )
        val viewModel = CommunityFeedViewModel(community, preferences)
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        preferences.hideList("a", "Sci-fi films")
        viewModel.refreshHidden()
        assertEquals(listOf("b"), shownIds(viewModel))

        preferences.unhideList("a")
        viewModel.refreshHidden()
        val back = viewModel.state.firstWithin { readyFeed(it)?.lists?.size == 2 }
        assertEquals(listOf("a", "b"), readyFeed(back)!!.lists.map { it.id })
    }

    @Test
    fun theNextPage_isPutUnderWhatIsAlreadyThere() = runBlocking {
        val community = FakeFeedRepository(
            feed = listOf(published("a", "One")),
            nextPages = listOf(listOf(published("b", "Two"))),
        )
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()
        viewModel.state.firstWithin { readyFeed(it)?.loadingMore == false && readyFeed(it)?.lists?.size == 2 }

        assertEquals(listOf("a", "b"), shownIds(viewModel))
        assertEquals(listOf(null, "0"), community.cursorsAsked)
    }

    @Test
    fun theLastPage_isNotFollowedByAnotherRequest() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()

        assertEquals(listOf(null), community.cursorsAsked)
        assertEquals(false, readyFeed(viewModel.state.value)?.canLoadMore)
    }

    // Losing the page someone is looking at because the one after it did not
    // arrive would be a worse answer than no more lists.
    @Test
    fun aPageThatFails_leavesTheFeedAsItIs() = runBlocking {
        val community = FakeFeedRepository(
            feed = listOf(published("a", "One")),
            nextPages = listOf(listOf(published("b", "Two"))),
            laterPagesFail = true,
        )
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()
        viewModel.state.firstWithin { readyFeed(it)?.loadingMore == false }

        assertEquals(listOf("a"), shownIds(viewModel))
    }

    @Test
    fun aListHiddenBefore_doesNotArriveWithALaterPage() = runBlocking {
        val preferences = FakeAppPreferences()
        preferences.hideList("b", "Two")
        val community = FakeFeedRepository(
            feed = listOf(published("a", "One")),
            nextPages = listOf(listOf(published("b", "Two"), published("c", "Three"))),
        )
        val viewModel = CommunityFeedViewModel(community, preferences)
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()
        viewModel.state.firstWithin { readyFeed(it)?.loadingMore == false && readyFeed(it)?.lists?.size == 2 }

        assertEquals(listOf("a", "c"), shownIds(viewModel))
    }

    // Design asked for a quiet note where the card was, not a silent gap:
    // vanishing reads as "deleted", which is not what happened.
    @Test
    fun hidingFromTheFeed_leavesANoteWhereTheCardWas() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One"), published("b", "Two")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.hideList(published("a", "One"))

        val feed = readyFeed(viewModel.state.value)!!
        assertEquals(listOf("a", "b"), feed.lists.map { it.id })
        assertEquals(mapOf("a" to false), feed.justHidden)
    }

    @Test
    fun reportingFromTheFeed_saysSoInTheNote() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.report(published("a", "One"), ReportReason.Spam, null)

        assertEquals(mapOf("a" to true), readyFeed(viewModel.state.value)!!.justHidden)
    }

    @Test
    fun theNextLoad_carriesNoNotes() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One"), published("b", "Two")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }
        viewModel.hideList(published("a", "One"))

        viewModel.load()
        val reloaded = viewModel.state.firstWithin {
            readyFeed(it)?.justHidden?.isEmpty() == true && readyFeed(it)?.lists?.size == 1
        }

        assertEquals(listOf("b"), readyFeed(reloaded)!!.lists.map { it.id })
    }

    // Searching asks the server, not the screen: the words go with the request,
    // and closing the search puts the whole feed back.
    @Test
    fun aSearch_goesToTheServer_andClosingItBringsTheFeedBack() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.enterSearch()
        viewModel.updateQuery("ghibli")
        viewModel.state.firstWithin { it.query == "ghibli" && it.feed is CommunityFeed.Ready }
        assertEquals(listOf(null, "ghibli"), community.queriesAsked)

        viewModel.exitSearch()
        viewModel.state.firstWithin { it.query == null && it.feed is CommunityFeed.Ready }
        assertEquals(listOf(null, "ghibli", null), community.queriesAsked)
    }

    @Test
    fun aFollowingFeed_bringsThePeopleItIsMadeOf() = runBlocking {
        val community = object : FakeFeedRepository(feed = listOf(published("a", "One"))) {
            override suspend fun following() = Result.success(Following(listOf(FollowedAuthor("u1", "Ann", null)), emptyList()))
        }
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.selectSource(FeedSource.Following)

        val state = viewModel.state.firstWithin { readyFeed(it)?.followed?.isNotEmpty() == true }

        assertEquals(listOf("u1"), readyFeed(state)!!.followed.map { it.uid })
    }

    // Proposed and followed arrive as two answers; a topic in both is one chip, a turned-down one is none.
    @Test
    fun yourTopics_areWhatYouProposedAndFollow_withoutTwinsOrRefusals() = runBlocking {
        val community = object : FakeFeedRepository() {
            override suspend fun myTopics(): Result<List<Topic>> =
                Result.success(listOf(knownTopic("a"), knownTopic("b", TopicStatus.Rejected)))
            override suspend fun following() = Result.success(Following(emptyList(), listOf(knownTopic("a"), knownTopic("c"))))
        }
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()

        val state = viewModel.state.firstWithin { it.yourTopics.isNotEmpty() }

        assertEquals(listOf("a", "c"), state.yourTopics.map { it.id })
    }

    // The server hands the following feed over as one page of people and topics; the chips narrow it here.
    @Test
    fun aFollowingFeed_narrowsToPeopleOrTopics_withoutAskingAgain() = runBlocking {
        val community = object : FakeFeedRepository(
            feed = listOf(published("a"), published("b").copy(authorUid = "someone", topicId = "t1", topicName = "Pokémon")),
        ) {
            override suspend fun following() =
                Result.success(Following(listOf(FollowedAuthor("author-a", "Ann", null)), listOf(knownTopic("t1"))))
        }
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.selectSource(FeedSource.Following)
        viewModel.state.firstWithin { readyFeed(it)?.followedTopics?.isNotEmpty() == true }
        val asked = community.cursorsAsked.size

        viewModel.selectFollowingFilter(FollowingFilter.People)
        assertEquals(listOf("a"), shownIds(viewModel))
        viewModel.selectFollowingFilter(FollowingFilter.Topics)
        assertEquals(listOf("b"), shownIds(viewModel))
        viewModel.selectFollowingFilter(FollowingFilter.All)
        assertEquals(listOf("a", "b"), shownIds(viewModel))

        assertEquals(asked, community.cursorsAsked.size)
    }

    // A page narrowed to topics may hold none while the next page does: the feed asks on by itself.
    @Test
    fun narrowingToTopics_asksForTheNextPage_whenThisOneHasNone() = runBlocking {
        val community = object : FakeFeedRepository(
            feed = listOf(published("a")),
            nextPages = listOf(listOf(published("b").copy(authorUid = "someone", topicId = "t1", topicName = "Pokémon"))),
        ) {
            override suspend fun following() =
                Result.success(Following(listOf(FollowedAuthor("author-a", "Ann", null)), listOf(knownTopic("t1"))))
        }
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.selectSource(FeedSource.Following)
        viewModel.state.firstWithin { readyFeed(it)?.followedTopics?.isNotEmpty() == true }

        viewModel.selectFollowingFilter(FollowingFilter.Topics)
        viewModel.state.firstWithin { readyFeed(it)?.lists?.isNotEmpty() == true }

        assertEquals(listOf("b"), shownIds(viewModel))
        assertEquals(listOf(null, "0"), community.cursorsAsked)
    }

    private fun knownTopic(id: String, status: TopicStatus = TopicStatus.Approved) =
        Topic(id = id, name = "Topic $id", section = ListCategory.Games, status = status)

    private fun shownIds(viewModel: CommunityFeedViewModel): List<String> =
        readyFeed(viewModel.state.value)!!.lists.map { it.id }

    private fun readyFeed(state: CommunityFeedUiState): CommunityFeed.Ready? = state.feed as? CommunityFeed.Ready

    private fun published(id: String, title: String = "List $id") = PublishedListSummary(
        id = id,
        title = title,
        authorUid = "author-$id",
        authorName = "Olena M.",
        category = ListCategory.FilmTv,
        itemCount = 12,
        updatedAtMillis = 0,
    )
}

private open class FakeFeedRepository(
    private val feed: List<PublishedListSummary> = emptyList(),
    /** Pages after the first, in order. The cursor to each is its index. */
    private val nextPages: List<List<PublishedListSummary>> = emptyList(),
    private val laterPagesFail: Boolean = false,
    /** What the first page comes back as, when it does not come back. */
    private val firstPageFails: Throwable? = null,
) : FakeCommunityRepository() {
    val cursorsAsked = mutableListOf<String?>()
    val queriesAsked = mutableListOf<String?>()

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
        topic: String?,
    ): Result<CommunityPage> {
        cursorsAsked += after
        queriesAsked += query
        if (after == null && firstPageFails != null) return Result.failure(firstPageFails)
        if (after != null && laterPagesFail) return Result.failure(IllegalStateException("offline"))
        val index = after?.toInt()?.plus(1) ?: 0
        val lists = if (index == 0) feed else nextPages.getOrElse(index - 1) { emptyList() }
        val more = index < nextPages.size
        return Result.success(CommunityPage(lists, nextCursor = if (more) index.toString() else null))
    }
}
