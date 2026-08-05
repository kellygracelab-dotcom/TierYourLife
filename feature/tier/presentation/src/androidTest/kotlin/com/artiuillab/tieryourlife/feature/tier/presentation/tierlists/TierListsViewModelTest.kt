package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Runs as an instrumented test, not a plain JVM unit test: TierListsViewModel uses
// viewModelScope, which needs a real Main dispatcher — available on the device, not
// in a bare JVM test without adding kotlinx-coroutines-test as a dependency.
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
        val viewModel = TierListsViewModel(repository)

        viewModel.loadTierLists()
        val state = viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success

        assertEquals(listOf(1L, 2L), state.lists.map { it.id })
        assertEquals(listOf("Existing list", "Another list"), state.lists.map { it.title })
    }

    // An empty library used to be impossible: finding no lists, the load seeded two demo
    // lists, so deleting everything brought them straight back on the next read and the
    // empty state could never appear. Nothing is created on the user's behalf now.
    @Test
    fun loadTierLists_onAnEmptyRepository_staysEmpty_andCreatesNothing() = runBlocking {
        val repository = FakeTierRepository(initial = emptyList())
        val viewModel = TierListsViewModel(repository)

        viewModel.loadTierLists()
        val state = viewModel.state.first { it is TierListsUiState.Success } as TierListsUiState.Success

        assertEquals(emptyList<Long>(), state.lists.map { it.id })
        assertEquals(0, state.totalListCount)
    }

    @Test
    fun secondLoad_afterARenameOnTheRepository_showsTheNewTitle() = runBlocking {
        val repository = FakeTierRepository(initial = listOf(fakeList(id = 1, title = "Old title")))
        val viewModel = TierListsViewModel(repository)

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
        val viewModel = TierListsViewModel(repository)

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
        val viewModel = TierListsViewModel(repository)

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
        val viewModel = TierListsViewModel(repository)

        viewModel.loadTierLists()
        viewModel.state.first { it is TierListsUiState.Success }

        repository.renameTierList(1, "New title")

        val collected = recordStates(viewModel)

        viewModel.loadTierLists()
        collected.awaitUntil {
            it is TierListsUiState.Success && it.lists.singleOrNull()?.title == "New title"
        }
        collected.job.cancel()

        // Exactly two values are ever observed across the whole reload: the list
        // already on screen (replayed the instant this recorder subscribes) and the
        // new one replacing it directly — never Loading in between, and never more
        // than that single step.
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
        val viewModel = TierListsViewModel(repository)

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
        val viewModel = TierListsViewModel(repository)

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
        val viewModel = TierListsViewModel(repository)

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
        val viewModel = TierListsViewModel(repository)

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

    private fun fakeList(id: Long, title: String): TierList = TierList(id = id, title = title, tiers = emptyList())

    private class RecordedStates(val values: MutableList<TierListsUiState>, val job: Job) {
        // Waits on this recorder's own list rather than on a second, independent
        // subscription to viewModel.state: two separate collectors resumed from the
        // same emission can be scheduled in either order, so a wait based on the other
        // subscription can return before this one has appended its value — polling the
        // list this recorder itself appends to cannot race against itself that way.
        suspend fun awaitUntil(predicate: (TierListsUiState) -> Boolean) {
            withTimeout(5_000) {
                while (values.none(predicate)) {
                    yield()
                }
            }
        }
    }

    // Subscribes and suspends until the first (replayed) value has actually been
    // recorded, so the caller can rely on collected.values already holding whatever
    // was on screen before triggering the load that's under test — without this, a
    // race between this subscription and the load's own dispatch could let the
    // collector miss the pre-load value entirely.
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

private class FakeTierRepository(initial: List<TierList>) : TierRepository {

    private val lists = initial.associateBy { it.id }.toMutableMap()
    private val trashed = mutableMapOf<Long, TierList>()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun getTierListById(id: Long): TierList? = lists[id]

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
    override suspend fun addMovieToPool(tierListId: Long, title: String, imageUrl: String?): Long = unsupported()
    override suspend fun addMoviesToPool(tierListId: Long, movies: List<PoolMovieDraft>) = unsupported()
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
    override suspend fun deleteTier(id: Long) = unsupported()
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
