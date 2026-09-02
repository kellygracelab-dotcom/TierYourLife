package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierListsViewModelTest {

    @Test
    fun loadTierLists_showsExactlyWhatTheRepositoryReturned_noneAdded() = runBlocking {
        val repository = FakeTierRepository(
            initial = listOf(
                fakeList(id = 1, title = "Existing list"),
                fakeList(id = 2, title = "Another list"),
            ),
        )
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        val state = viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success

        assertEquals(listOf(1L, 2L), state.lists.map { it.id })
        assertEquals(listOf("Existing list", "Another list"), state.lists.map { it.title })
        assertEquals(0, repository.getByIdCalls)
    }

    @Test
    fun loadTierLists_onAnEmptyRepository_staysEmpty_andCreatesNothing() = runBlocking {
        val repository = FakeTierRepository(initial = emptyList())
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        val state = viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success

        assertEquals(emptyList<Long>(), state.lists.map { it.id })
        assertEquals(0, state.totalListCount)
    }

    @Test
    fun secondLoad_afterARenameOnTheRepository_showsTheNewTitle() = runBlocking {
        val repository = FakeTierRepository(initial = listOf(fakeList(id = 1, title = "Old title")))
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        repository.renameTierList(1, "New title")

        viewModel.loadTierLists()
        val state = viewModel.state.first {
            it is TierListsUiState.Success && it.lists.singleOrNull()?.title == "New title"
        } as TierListsUiState.Success

        assertEquals(listOf("New title"), state.lists.map { it.title })
    }

    @Test
    fun secondLoad_afterADeletionOnTheRepository_dropsTheDeletedList() = runBlocking {
        val repository = FakeTierRepository(
            initial = listOf(
                fakeList(id = 1, title = "Keeps existing"),
                fakeList(id = 2, title = "Gets deleted"),
            ),
        )
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        repository.deleteTierLists(listOf(2L))

        viewModel.loadTierLists()
        val state = viewModel.state.first {
            it is TierListsUiState.Success && it.lists.size == 1
        } as TierListsUiState.Success

        assertEquals(listOf(1L), state.lists.map { it.id })
    }

    @Test
    fun firstLoad_passesThroughLoadingState() = runBlocking {
        val repository = FakeTierRepository(initial = listOf(fakeList(id = 1, title = "Existing")))
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        val collected = recordStates(viewModel)

        viewModel.loadTierLists()
        collected.awaitUntil { it is TierListsUiState.Success }
        collected.job.cancel()

        assertEquals(listOf(TierListsUiState.Loading), collected.values.dropLast(1))
        assertTrue(collected.values.last() is TierListsUiState.Success)
    }

    @Test
    fun secondLoad_neverShowsLoading_andReplacesTheListInOneStep() = runBlocking {
        val repository = FakeTierRepository(initial = listOf(fakeList(id = 1, title = "Old title")))
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        repository.renameTierList(1, "New title")

        val collected = recordStates(viewModel)

        viewModel.loadTierLists()
        collected.awaitUntil {
            it is TierListsUiState.Success && it.lists.singleOrNull()?.title == "New title"
        }
        collected.job.cancel()

        assertEquals(2, collected.values.size)
        val before = collected.values[0] as TierListsUiState.Success
        val after = collected.values[1] as TierListsUiState.Success
        assertEquals("Old title", before.lists.single().title)
        assertEquals("New title", after.lists.single().title)
    }

    @Test
    fun search_filtersLists_caseInsensitiveAndAnywhereInTheName() = runBlocking {
        val repository = FakeTierRepository(
            initial = listOf(fakeList(id = 1, title = "Pizza in Lisbon"), fakeList(id = 2, title = "Sushi tour")),
        )
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        viewModel.enterSearch()
        viewModel.updateSearchQuery("piz")

        val state = viewModel.state.first {
            it is TierListsUiState.Success && (it.mode as? HomeMode.Searching)?.query == "piz"
        } as TierListsUiState.Success

        assertEquals(listOf("Pizza in Lisbon"), state.lists.map { it.title })
        assertEquals(2, state.totalListCount)
    }

    @Test
    fun exitSearch_returnsToBrowsingWithEveryListVisibleAgain() = runBlocking {
        val repository = FakeTierRepository(initial = listOf(fakeList(id = 1, title = "Pizza in Lisbon")))
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        viewModel.enterSearch()
        viewModel.updateSearchQuery("nothing matches this")
        viewModel.exitSearch()

        val state = viewModel.state.first {
            it is TierListsUiState.Success && it.mode == HomeMode.Browsing
        } as TierListsUiState.Success

        assertEquals(listOf(1L), state.lists.map { it.id })
    }

    @Test
    fun toggleSelection_deselectingTheLastId_returnsToBrowsingMode() = runBlocking {
        val repository = FakeTierRepository(initial = listOf(fakeList(id = 1, title = "Only list")))
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        viewModel.enterSelection(1L)
        val selecting = viewModel.state.first {
            it is TierListsUiState.Success && it.mode is HomeMode.Selecting
        } as TierListsUiState.Success
        assertEquals(HomeMode.Selecting(setOf(1L)), selecting.mode)

        viewModel.toggleSelection(1L)
        val afterDeselect = viewModel.state.first {
            it is TierListsUiState.Success
        } as TierListsUiState.Success
        assertEquals(HomeMode.Browsing, afterDeselect.mode)
    }

    @Test
    fun deleteTierLists_removesThemAndExitsSelection_thenRestoreBringsThemBack() = runBlocking {
        val repository = FakeTierRepository(
            initial = listOf(fakeList(id = 1, title = "Keeps existing"), fakeList(id = 2, title = "Gets deleted")),
        )
        val viewModel = TierListsViewModel(repository, FakeCommunityRepository(), FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        viewModel.enterSelection(2L)
        viewModel.deleteTierLists(listOf(2L))

        val afterDelete = viewModel.state.first {
            it is TierListsUiState.Success && it.lists.size == 1
        } as TierListsUiState.Success
        assertEquals(listOf(1L), afterDelete.lists.map { it.id })
        assertEquals(HomeMode.Browsing, afterDelete.mode)

        viewModel.restoreTierLists(listOf(2L))
        val afterRestore = viewModel.state.first {
            it is TierListsUiState.Success && it.lists.size == 2
        } as TierListsUiState.Success
        assertEquals(listOf(1L, 2L), afterRestore.lists.map { it.id }.sorted())
    }

    // A snapshot that outlives the list it came from is one its owner believes
    // they deleted.
    @Test
    fun deletingAPublishedList_takesItOutOfTheCommunityFirst() = runBlocking {
        val repository = FakeTierRepository(
            initial = listOf(fakeList(id = 1, title = "Films", publishedId = "published-1")),
        )
        val community = FakeCommunityRepository()
        val viewModel = TierListsViewModel(repository, community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        viewModel.deleteTierLists(listOf(1L))
        viewModel.state.first { it is TierListsUiState.Success && it.lists.isEmpty() }

        assertEquals(listOf("published-1"), community.takenDown)
    }

    // Deleting a list that stays visible to everyone is worse than a delete
    // that did not happen, so the list is kept and the reason is said out loud.
    @Test
    fun whenTheCommunityCopyWillNotComeDown_theListIsKept() = runBlocking {
        val repository = FakeTierRepository(
            initial = listOf(fakeList(id = 1, title = "Films", publishedId = "published-1")),
        )
        val community = FakeCommunityRepository(unpublishResult = Result.failure(IllegalStateException()))
        val viewModel = TierListsViewModel(repository, community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        val message = async { viewModel.userMessages.first() }
        viewModel.deleteTierLists(listOf(1L))

        assertEquals(UserMessage.PublishedListStillPublic, message.await())
        val state = viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success
        assertEquals(listOf(1L), state.lists.map { it.id })
    }

    @Test
    fun aListHiddenOnAnotherScreen_isGoneWhenTheFeedComesBackIntoView() = runBlocking {
        val preferences = FakeAppPreferences()
        val community = FakeCommunityRepository(
            feed = listOf(published("a", "Sci-fi films"), published("b", "Every A24 film")),
        )
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, preferences, guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        // What opening the list and hiding it from in there leaves behind.
        preferences.hideList("a", "Sci-fi films")
        viewModel.refreshHidden()

        val feed = (viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success)
            .community as CommunityFeed.Ready
        assertEquals(listOf("b"), feed.lists.map { it.id })
    }

    @Test
    fun comingBackWithNothingHidden_leavesTheFeedAlone() = runBlocking {
        val community = FakeCommunityRepository(feed = listOf(published("a", "Sci-fi films")))
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.refreshHidden()

        val feed = (viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success)
            .community as CommunityFeed.Ready
        assertEquals(listOf("a"), feed.lists.map { it.id })
    }

    // Undoing a hide happens in Settings, where the feed is not on screen and
    // what we hold has already had the card taken out of it.
    @Test
    fun aListPutBack_returnsToTheFeed() = runBlocking {
        val preferences = FakeAppPreferences()
        val community = FakeCommunityRepository(
            feed = listOf(published("a", "Sci-fi films"), published("b", "Every A24 film")),
        )
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, preferences, guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        preferences.hideList("a", "Sci-fi films")
        viewModel.refreshHidden()
        assertEquals(listOf("b"), shownIds(viewModel))

        preferences.unhideList("a")
        viewModel.refreshHidden()
        val back = viewModel.state.first {
            ((it as? TierListsUiState.Success)?.community as? CommunityFeed.Ready)?.lists?.size == 2
        }
        assertEquals(listOf("a", "b"), ((back as TierListsUiState.Success).community as CommunityFeed.Ready).lists.map { it.id })
    }

    private fun shownIds(viewModel: TierListsViewModel): List<String> =
        ((viewModel.state.value as TierListsUiState.Success).community as CommunityFeed.Ready).lists.map { it.id }

    @Test
    fun theNextPage_isPutUnderWhatIsAlreadyThere() = runBlocking {
        val community = FakeCommunityRepository(
            feed = listOf(published("a", "One")),
            nextPages = listOf(listOf(published("b", "Two"))),
        )
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.loadMoreCommunity()
        viewModel.state.first { readyFeed(it)?.loadingMore == false && readyFeed(it)?.lists?.size == 2 }

        assertEquals(listOf("a", "b"), shownIds(viewModel))
        assertEquals(listOf(null, "0"), community.cursorsAsked)
    }

    @Test
    fun theLastPage_isNotFollowedByAnotherRequest() = runBlocking {
        val community = FakeCommunityRepository(feed = listOf(published("a", "One")))
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.loadMoreCommunity()

        assertEquals(listOf(null), community.cursorsAsked)
        assertEquals(false, readyFeed(viewModel.state.value)?.canLoadMore)
    }

    // Losing the page someone is looking at because the one after it did not
    // arrive would be a worse answer than no more lists.
    @Test
    fun aPageThatFails_leavesTheFeedAsItIs() = runBlocking {
        val community = FakeCommunityRepository(
            feed = listOf(published("a", "One")),
            nextPages = listOf(listOf(published("b", "Two"))),
            laterPagesFail = true,
        )
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.loadMoreCommunity()
        viewModel.state.first { readyFeed(it)?.loadingMore == false }

        assertEquals(listOf("a"), shownIds(viewModel))
    }

    @Test
    fun aListHiddenBefore_doesNotArriveWithALaterPage() = runBlocking {
        val preferences = FakeAppPreferences()
        preferences.hideList("b", "Two")
        val community = FakeCommunityRepository(
            feed = listOf(published("a", "One")),
            nextPages = listOf(listOf(published("b", "Two"), published("c", "Three"))),
        )
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, preferences, guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.loadMoreCommunity()
        viewModel.state.first { readyFeed(it)?.loadingMore == false && readyFeed(it)?.lists?.size == 2 }

        assertEquals(listOf("a", "c"), shownIds(viewModel))
    }

    // Design asked for a quiet note where the card was, not a silent gap:
    // vanishing reads as "deleted", which is not what happened.
    @Test
    fun hidingFromTheFeed_leavesANoteWhereTheCardWas() = runBlocking {
        val community = FakeCommunityRepository(feed = listOf(published("a", "One"), published("b", "Two")))
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.hideCommunityList(published("a", "One"))

        val feed = readyFeed(viewModel.state.value)!!
        assertEquals(listOf("a", "b"), feed.lists.map { it.id })
        assertEquals(mapOf("a" to false), feed.justHidden)
    }

    @Test
    fun reportingFromTheFeed_saysSoInTheNote() = runBlocking {
        val community = FakeCommunityRepository(feed = listOf(published("a", "One")))
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, FakeAppPreferences(), guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }

        viewModel.reportCommunityList(published("a", "One"), ReportReason.Spam, null)

        assertEquals(mapOf("a" to true), readyFeed(viewModel.state.value)!!.justHidden)
    }

    @Test
    fun theNextLoad_carriesNoNotes() = runBlocking {
        val preferences = FakeAppPreferences()
        val community = FakeCommunityRepository(feed = listOf(published("a", "One"), published("b", "Two")))
        val viewModel = TierListsViewModel(FakeTierRepository(emptyList()), community, preferences, guestAccount(), NoBoardSync, NoPictureRestore)
        viewModel.selectTab(HomeTab.Community)
        viewModel.state.first { (it as? TierListsUiState.Success)?.community is CommunityFeed.Ready }
        viewModel.hideCommunityList(published("a", "One"))

        viewModel.loadCommunityFeed()
        val reloaded = viewModel.state.first {
            readyFeed(it)?.justHidden?.isEmpty() == true && readyFeed(it)?.lists?.size == 1
        }

        assertEquals(listOf("b"), readyFeed(reloaded)!!.lists.map { it.id })
    }

    private fun readyFeed(state: TierListsUiState): CommunityFeed.Ready? =
        (state as? TierListsUiState.Success)?.community as? CommunityFeed.Ready

    private fun published(id: String, title: String) = PublishedListSummary(
        id = id,
        title = title,
        authorUid = "author-$id",
        authorName = "Olena M.",
        category = ListCategory.FilmTv,
        itemCount = 12,
        updatedAtMillis = 0,
    )

    private fun fakeList(id: Long, title: String, publishedId: String? = null): TierList =
        TierList(id = id, title = title, tiers = emptyList(), publishedId = publishedId)

    private class RecordedStates(val values: MutableList<TierListsUiState>, val job: Job) {
        suspend fun awaitUntil(predicate: (TierListsUiState) -> Boolean) {
            withTimeout(5_000) {
                while (values.none(predicate)) {
                    yield()
                }
            }
        }
    }

    private suspend fun CoroutineScope.recordStates(viewModel: TierListsViewModel): RecordedStates {
        val values = mutableListOf<TierListsUiState>()
        val subscribed = CompletableDeferred<Unit>()
        val job = launch {
            viewModel.state.collect {
                values.add(it)
                subscribed.complete(Unit)
            }
        }
        subscribed.await()
        return RecordedStates(values, job)
    }
}

internal class FakeTierRepository(initial: List<TierList>) : TierRepository {

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
    ): Long = 0

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) = Unit

    override suspend fun publishedStanding(): PublishedStanding = PublishedStanding()

    override suspend fun boardPublishedAs(publishedId: String): TierList? = null

    override suspend fun setFavouritedAt(id: Long, at: Long?) = Unit

    override suspend fun setCategory(id: Long, category: ListCategory?) = Unit

    override suspend fun setCoverImageUrl(id: Long, coverImageUrl: String?) = Unit

    private val lists = initial.associateBy { it.id }.toMutableMap()
    private val trashed = mutableMapOf<Long, TierList>()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    var getByIdCalls = 0
        private set

    override suspend fun getTierListById(id: Long): TierList? {
        getByIdCalls++
        return lists[id]
    }

    override suspend fun getAllTierLists(): List<TierList> = lists.values.sortedBy { it.id }

    override suspend fun createTierList(title: String): Long {
        val id = nextId++
        lists[id] = TierList(id = id, title = title, tiers = emptyList())
        return id
    }

    override suspend fun renameTierList(id: Long, title: String) {
        lists[id]?.let { lists[id] = it.copy(title = title) }
    }

    override suspend fun deleteTierLists(ids: List<Long>) {
        ids.forEach { id -> lists.remove(id)?.let { trashed[id] = it } }
    }

    override suspend fun restoreTierLists(ids: List<Long>) {
        ids.forEach { id -> trashed.remove(id)?.let { lists[id] = it } }
    }

    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = unsupported()
    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long = unsupported()
    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = unsupported()
    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) = unsupported()
    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) = unsupported()
    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long = unsupported()

    override suspend fun renameTier(id: Long, label: String, caption: String?) = unsupported()
    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) = unsupported()
    override suspend fun deleteTierToPool(id: Long) = unsupported()
    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) = unsupported()
    override suspend fun reorderTiers(orderedTierIds: List<Long>) = unsupported()
    override suspend fun deleteTierItem(id: Long) = unsupported()
    override suspend fun restoreTierItem(id: Long) = unsupported()
    override suspend fun deleteTierListPermanently(id: Long) = unsupported()
    override suspend fun deleteTierItemPermanently(id: Long) = unsupported()
    override suspend fun emptyTrash() = unsupported()
    override suspend fun getTrashEntries(): List<TrashEntry> = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("Not used by TierListsViewModel")
}

private class FakeCommunityRepository(
    private val feed: List<PublishedListSummary> = emptyList(),
    private val unpublishResult: Result<Unit> = Result.success(Unit),
    /** Pages after the first, in order. The cursor to each is its index. */
    private val nextPages: List<List<PublishedListSummary>> = emptyList(),
    private val laterPagesFail: Boolean = false,
) : CommunityRepository {
    val takenDown = mutableListOf<String>()
    val cursorsAsked = mutableListOf<String?>()
    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> {
        cursorsAsked += after
        if (after != null && laterPagesFail) return Result.failure(IllegalStateException("offline"))
        val index = after?.toInt()?.plus(1) ?: 0
        val lists = if (index == 0) feed else nextPages.getOrElse(index - 1) { emptyList() }
        val more = index < nextPages.size
        return Result.success(CommunityPage(lists, nextCursor = if (more) index.toString() else null))
    }
    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())

    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())
    override suspend fun publish(list: com.artiuillab.tieryourlife.feature.tier.domain.model.TierList): Result<Published> =
        Result.failure(IllegalStateException())
    override suspend fun unpublish(publishedId: String): Result<Unit> {
        takenDown += publishedId
        return unpublishResult
    }

    override suspend fun makeFace(pictureId: String): Result<String> =
        Result.success("https://example.test/face.jpg")

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
    override suspend fun reports(): Result<List<ModerationReport>> = Result.failure(IllegalStateException())
    override suspend fun takeDown(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun follow(authorUid: String): Result<Unit> = Result.success(Unit)

    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)

    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(emptyList())

    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)
}

private fun guestAccount(): AccountRepository = object : AccountRepository {
    override val account: Flow<Account> = flowOf(Account.Guest)
    override suspend fun signInWithGoogle(idToken: String): SignInOutcome = SignInOutcome.Success
    override suspend fun setDisplayName(name: String): Boolean = true
    override suspend fun setPhotoUrl(photoUrl: String?): Boolean = true
    override suspend fun signOut() = Unit
}

/** A guest has nowhere to sync to, so every case here would report the same thing. */
private object NoBoardSync : BoardSync {
    override suspend fun sync(): SyncReport = SyncReport(signedIn = false)
}

/** Nothing is arriving on this phone, which is what the list screen sees almost always. */
private object NoPictureRestore : PictureRestore {
    override val restoring = MutableStateFlow(PictureRestore.Progress.Idle)
}
