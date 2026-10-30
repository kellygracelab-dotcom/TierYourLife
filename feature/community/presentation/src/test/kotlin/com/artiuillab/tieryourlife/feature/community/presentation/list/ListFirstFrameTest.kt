package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.ListGone
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private val summary = PublishedListSummary(
    id = "abc",
    title = "Every A24 film",
    authorUid = "author-1",
    authorName = "danylo",
    category = ListCategory.FilmTv.id,
    itemCount = 4,
    tierColors = listOf("#B03A32", "#C06A25"),
    updatedAtMillis = 100,
    rankingCount = 13,
    basedOn = BasedOn("src", "Every A24 film", "kenji"),
    tierCounts = listOf(2, 1, 1),
    viewerFollowsAuthor = true,
)

private val theList = PublishedList(
    summary = summary,
    tiers = listOf(
        Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
        Tier(id = 1, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", items = emptyList()),
    ),
    items = listOf(
        TierItem(0, "Pearl", "https://i/0.jpg"),
        TierItem(1, "Moonlight", "https://i/1.jpg"),
        TierItem(2, "Lady Bird", null),
        TierItem(3, "Minari", "https://i/3.jpg"),
    ),
    arrangement = listOf(0, 1, 0, null),
)

/** What the page draws before the server answers, what it keeps when it does, and the copy kept on the phone. */
class ListFirstFrameTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    @Test
    fun theFirstFrame_isTheRealHeader_andABlankCardForEveryCard_inItsTier() {
        val viewModel = viewModel(community = Answering(gate = CompletableDeferred()))

        val first = viewModel.state.value as CommunityListUiState.Success

        assertTrue(first.arriving)
        assertEquals(Showing.Theirs, first.showing)
        assertEquals(listOf(2, 1, 1), first.list.tiers.map { it.items.size })
        assertTrue(first.list.tiers.last().isPool)
        assertTrue(first.list.tiers.flatMap { it.items }.all { it.title.isEmpty() && it.imageUrl == null })
        assertEquals(listOf("#B03A32", "#C06A25"), first.list.tiers.dropLast(1).map { it.colorLight })
        assertEquals(listOf("#F1948C", "#E9A867"), first.list.tiers.dropLast(1).map { it.colorDark })
        assertEquals(FollowState(following = true, followers = 0), first.follow)
        assertEquals(summary.basedOn, first.basedOn)
        assertEquals("danylo", first.authorName)
        assertEquals(13, first.rankingCount)
        assertEquals(4, first.mine.tiers.single { it.isPool }.items.size)
    }

    @Test
    fun aCardThatDidNotSayHowTheTiersFill_leavesTheSkeletonToIt() {
        val viewModel = viewModel(community = Answering(gate = CompletableDeferred()), route = CommunityRoute.CommunityList.of(summary.copy(tierCounts = emptyList())))

        assertEquals(CommunityListUiState.Loading, viewModel.state.value)
    }

    @Test
    fun whenTheListLands_everyTierHoldsAsManyCardsAsTheBlanksDid() = runBlocking {
        val viewModel = viewModel(community = Answering())
        val first = viewModel.state.value as CommunityListUiState.Success

        val loaded = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false } as CommunityListUiState.Success

        assertEquals(first.list.tiers.map { it.items.size }, loaded.list.tiers.map { it.items.size })
        assertEquals(first.list.tiers.map { it.id }, loaded.list.tiers.map { it.id })
        assertEquals(first.showing, loaded.showing)
        assertEquals(first.follow, loaded.follow)
        assertEquals(first.basedOn, loaded.basedOn)
        assertEquals(listOf("Pearl", "Lady Bird"), loaded.list.tiers.first().items.map { it.title })
    }

    @Test
    fun theListSayingWhetherTheAuthorIsFollowed_asksNothingMore() = runBlocking {
        val community = Answering()
        val viewModel = viewModel(community = community)

        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false }

        assertEquals(0, community.followStateAsked)
    }

    @Test
    fun aServerThatDoesNotSay_isAskedTheOldWay() = runBlocking {
        val community = Answering(list = theList.copy(summary = summary.copy(viewerFollowsAuthor = null)))
        val viewModel = viewModel(community = community, route = CommunityRoute.CommunityList.of(summary.copy(viewerFollowsAuthor = null)))

        val loaded = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow != null } as CommunityListUiState.Success

        assertEquals(1, community.followStateAsked)
        assertEquals(FollowState(following = false, followers = 7), loaded.follow)
    }

    @Test
    fun blankCards_cannotBeMovedSavedOrPlayed() {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(community = Answering(gate = CompletableDeferred()), tiers = tiers)
        val first = viewModel.state.value as CommunityListUiState.Success

        viewModel.moveItem(first.list.tiers.first().items.first().id, toTierId = 1, toPosition = 0)
        viewModel.saveToMyLists(Showing.Theirs)

        assertEquals(first, viewModel.state.value)
        assertTrue(tiers.templates.isEmpty())
    }

    @Test
    fun aListOpenedBefore_isDrawnFromThePhone_whileTheServerIsAsked() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(community = Answering(gate = gate), kept = FakeOpenedLists(theList))

        val kept = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false } as CommunityListUiState.Success

        assertEquals(listOf("Pearl", "Lady Bird"), kept.list.tiers.first().items.map { it.title })
        assertFalse(gate.isCompleted)
    }

    @Test
    fun theSameBoardFromTheServer_keepsWhatTheReaderDid_andTakesTheNewCounts() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fresher = theList.copy(summary = summary.copy(rankingCount = 14))
        val opened = FakeOpenedLists(theList)
        val viewModel = viewModel(community = Answering(list = fresher, gate = gate), kept = opened)
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false }
        viewModel.show(Showing.Mine)
        viewModel.moveItem(itemId = 3, toTierId = 1, toPosition = 0)

        gate.complete(Unit)
        val after = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankingCount == 14 } as CommunityListUiState.Success

        assertEquals(Showing.Mine, after.showing)
        assertTrue(after.arranged)
        assertEquals(listOf("Minari"), after.mine.tiers.first { it.id == 1L }.items.map { it.title })
        assertEquals(14, opened.kept.getValue("abc").summary.rankingCount)
    }

    @Test
    fun aBoardTheAuthorChangedSince_replacesTheKeptOne() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val changed = theList.copy(summary = summary.copy(updatedAtMillis = 200), arrangement = listOf(1, 1, 1, 1))
        val viewModel = viewModel(community = Answering(list = changed, gate = gate), kept = FakeOpenedLists(theList))
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false }

        gate.complete(Unit)
        val after = viewModel.state.firstWithin {
            (it as? CommunityListUiState.Success)?.list?.tiers?.firstOrNull { tier -> tier.id == 1L }?.items?.size == 4
        } as CommunityListUiState.Success

        assertTrue(after.list.tiers.first().items.isEmpty())
    }

    @Test
    fun aListThatIsGone_isForgotten_andSaysSo() = runBlocking {
        val opened = FakeOpenedLists(theList)
        val viewModel = viewModel(community = Answering(failure = ListGone()), kept = opened)

        viewModel.state.firstWithin { it == CommunityListUiState.Error }

        assertEquals(listOf("abc"), opened.forgotten)
        assertNull(opened.kept["abc"])
    }

    @Test
    fun noNetwork_leavesTheKeptListOnScreen() = runBlocking {
        val opened = FakeOpenedLists(theList)
        val community = Answering(failure = IOException("offline"))
        val viewModel = viewModel(community = community, kept = opened)

        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false }
        community.answered.await()
        // Behind whatever the main thread was doing with the answer.
        withContext(Dispatchers.Main) {}

        assertTrue(viewModel.state.value is CommunityListUiState.Success)
        assertTrue(opened.forgotten.isEmpty())
    }

    @Test
    fun noNetwork_andNothingKept_isTheFailure() = runBlocking {
        val viewModel = viewModel(community = Answering(failure = IOException("offline")))

        assertEquals(CommunityListUiState.Error, viewModel.state.firstWithin { it == CommunityListUiState.Error })
    }

    @Test
    fun anOpenedList_isKeptForNextTime() = runBlocking {
        val opened = FakeOpenedLists()
        val viewModel = viewModel(community = Answering(), kept = opened)

        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arriving == false }

        assertEquals(theList, opened.kept["abc"])
    }

    private fun viewModel(
        community: Answering,
        tiers: RecordingTierRepository = RecordingTierRepository(),
        kept: FakeOpenedLists = FakeOpenedLists(),
        route: CommunityRoute.CommunityList = CommunityRoute.CommunityList.of(summary),
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = FakeAppPreferences(),
        savedStateHandle = handleOf(route),
        accounts = FakeAccountRepository(),
        openedLists = kept,
    ).also { made += it }
}

/** The route as Navigation would put it in the handle: by property name, nulls left out. */
private fun handleOf(route: CommunityRoute.CommunityList) = SavedStateHandle(
    listOf(
        "publishedId" to route.publishedId,
        "title" to route.title,
        "authorName" to route.authorName,
        "itemCount" to route.itemCount,
        "tierColors" to route.tierColors,
        "tierCounts" to route.tierCounts,
        "authorUid" to route.authorUid,
        "authorPhotoUrl" to route.authorPhotoUrl,
        "byNobody" to route.byNobody,
        "mine" to route.mine,
        "anonymous" to route.anonymous,
        "section" to route.section,
        "rankingCount" to route.rankingCount,
        "followKnown" to route.followKnown,
        "following" to route.following,
        "basedOnId" to route.basedOnId,
        "basedOnTitle" to route.basedOnTitle,
        "basedOnAuthor" to route.basedOnAuthor,
    ).filter { it.second != null }.toMap(),
)

/** Answers with [list] once [gate] opens, or with [failure]. */
private class Answering(
    private val list: PublishedList = theList,
    private val gate: CompletableDeferred<Unit>? = null,
    private val failure: Throwable? = null,
) : FakeCommunityRepository() {
    var followStateAsked = 0
    val answered = CompletableDeferred<Unit>()

    override suspend fun open(id: String): Result<PublishedList> {
        gate?.await()
        answered.complete(Unit)
        return failure?.let { Result.failure(it) } ?: Result.success(list)
    }

    override suspend fun followState(authorUid: String): Result<FollowState> {
        followStateAsked++
        return Result.success(FollowState(following = false, followers = 7))
    }
}
