package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CommunityFeedViewModelTest {

    @Test
    fun aListHiddenOnAnotherScreen_isGoneWhenTheFeedComesBackIntoView() = runBlocking {
        val preferences = FakeAppPreferences()
        val community = FakeFeedRepository(
            feed = listOf(published("a", "Sci-fi films"), published("b", "Every A24 film")),
        )
        val viewModel = CommunityFeedViewModel(community, preferences)
        viewModel.load()
        viewModel.state.first { it.feed is CommunityFeed.Ready }

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

        val state = viewModel.state.first { it.feed is CommunityFeed.Unverified }
        assertEquals(CommunityFeed.Unverified, state.feed)
    }

    @Test
    fun whenTheFeedSimplyDidNotArrive_itStaysTheOneWithATryAgain() = runBlocking {
        val community = FakeFeedRepository(firstPageFails = IOException("offline"))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()

        val state = viewModel.state.first { it.feed is CommunityFeed.Failed }
        assertEquals(CommunityFeed.Failed, state.feed)
    }

    @Test
    fun comingBackWithNothingHidden_leavesTheFeedAlone() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "Sci-fi films")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.first { it.feed is CommunityFeed.Ready }

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
        viewModel.state.first { it.feed is CommunityFeed.Ready }

        preferences.hideList("a", "Sci-fi films")
        viewModel.refreshHidden()
        assertEquals(listOf("b"), shownIds(viewModel))

        preferences.unhideList("a")
        viewModel.refreshHidden()
        val back = viewModel.state.first { readyFeed(it)?.lists?.size == 2 }
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
        viewModel.state.first { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()
        viewModel.state.first { readyFeed(it)?.loadingMore == false && readyFeed(it)?.lists?.size == 2 }

        assertEquals(listOf("a", "b"), shownIds(viewModel))
        assertEquals(listOf(null, "0"), community.cursorsAsked)
    }

    @Test
    fun theLastPage_isNotFollowedByAnotherRequest() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.first { it.feed is CommunityFeed.Ready }

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
        viewModel.state.first { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()
        viewModel.state.first { readyFeed(it)?.loadingMore == false }

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
        viewModel.state.first { it.feed is CommunityFeed.Ready }

        viewModel.loadMore()
        viewModel.state.first { readyFeed(it)?.loadingMore == false && readyFeed(it)?.lists?.size == 2 }

        assertEquals(listOf("a", "c"), shownIds(viewModel))
    }

    // Design asked for a quiet note where the card was, not a silent gap:
    // vanishing reads as "deleted", which is not what happened.
    @Test
    fun hidingFromTheFeed_leavesANoteWhereTheCardWas() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One"), published("b", "Two")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.first { it.feed is CommunityFeed.Ready }

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
        viewModel.state.first { it.feed is CommunityFeed.Ready }

        viewModel.report(published("a", "One"), ReportReason.Spam, null)

        assertEquals(mapOf("a" to true), readyFeed(viewModel.state.value)!!.justHidden)
    }

    @Test
    fun theNextLoad_carriesNoNotes() = runBlocking {
        val community = FakeFeedRepository(feed = listOf(published("a", "One"), published("b", "Two")))
        val viewModel = CommunityFeedViewModel(community, FakeAppPreferences())
        viewModel.load()
        viewModel.state.first { it.feed is CommunityFeed.Ready }
        viewModel.hideList(published("a", "One"))

        viewModel.load()
        val reloaded = viewModel.state.first {
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
        viewModel.state.first { it.feed is CommunityFeed.Ready }

        viewModel.enterSearch()
        viewModel.updateQuery("ghibli")
        viewModel.state.first { it.query == "ghibli" && it.feed is CommunityFeed.Ready }
        assertEquals(listOf(null, "ghibli"), community.queriesAsked)

        viewModel.exitSearch()
        viewModel.state.first { it.query == null && it.feed is CommunityFeed.Ready }
        assertEquals(listOf(null, "ghibli", null), community.queriesAsked)
    }

    private fun shownIds(viewModel: CommunityFeedViewModel): List<String> =
        readyFeed(viewModel.state.value)!!.lists.map { it.id }

    private fun readyFeed(state: CommunityFeedUiState): CommunityFeed.Ready? = state.feed as? CommunityFeed.Ready

    private fun published(id: String, title: String) = PublishedListSummary(
        id = id,
        title = title,
        authorUid = "author-$id",
        authorName = "Olena M.",
        category = ListCategory.FilmTv,
        itemCount = 12,
        updatedAtMillis = 0,
    )
}

private class FakeFeedRepository(
    private val feed: List<PublishedListSummary> = emptyList(),
    /** Pages after the first, in order. The cursor to each is its index. */
    private val nextPages: List<List<PublishedListSummary>> = emptyList(),
    private val laterPagesFail: Boolean = false,
    /** What the first page comes back as, when it does not come back. */
    private val firstPageFails: Throwable? = null,
) : CommunityRepository {
    val cursorsAsked = mutableListOf<String?>()
    val queriesAsked = mutableListOf<String?>()

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
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

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())
    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())
    override suspend fun publish(list: TierList): Result<Published> = Result.failure(IllegalStateException())
    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun makeFace(pictureId: String): Result<String> = Result.success("https://example.test/face.jpg")
    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)
    override suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit> =
        Result.success(Unit)

    override suspend fun reports(): Result<List<ModerationReport>> = Result.failure(IllegalStateException())
    override suspend fun takeDown(publishedId: String, ban: BanLength?): Result<Unit> = Result.success(Unit)
    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun follow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(emptyList())
    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)
}
