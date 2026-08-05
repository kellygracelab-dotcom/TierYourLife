package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

// Runs as an instrumented test, not a plain JVM unit test: TierDetailViewModel uses
// viewModelScope, which needs a real Main dispatcher — available on the device, not
// in a bare JVM test without adding kotlinx-coroutines-test as a dependency.
@RunWith(AndroidJUnit4::class)
class TierDetailViewModelTest {

    @Test
    fun addManualItem_withTitleOnly_addsToThePoolWithNoImage() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())

        viewModel.addManualItem("Dune", photoUri = null)
        val state = viewModel.state.first {
            it is TierDetailUiState.Success && it.list.tiers.single().items.isNotEmpty()
        } as TierDetailUiState.Success

        val added = state.list.tiers.single().items.single()
        assertEquals("Dune", added.title)
        assertNull(added.imageUrl)
        assertEquals(emptyList<String>(), repository.attachedSources)
    }

    // The point of this test: the picked gallery reference must leave the view model
    // only through attachImageToItem (which copies the source into internal storage —
    // already proven by RoomTierRepositoryImageTest), never as addMovieToPool's own
    // imageUrl argument, which would store the raw, unstable gallery Uri as-is.
    @Test
    fun addManualItem_withAPhoto_sendsTheGalleryUriOnlyToAttachImageToItem() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())

        viewModel.addManualItem("Dune", photoUri = "content://gallery/42")
        viewModel.state.first {
            it is TierDetailUiState.Success && it.list.tiers.single().items.isNotEmpty()
        }

        assertEquals(listOf("content://gallery/42"), repository.attachedSources)
        assertNull(repository.lastAddedImageUrl)
    }


    @Test
    fun canDiscard_seededFromStartInTitleEdit() = runBlocking {
        val newListViewModel = TierDetailViewModel(FakeTierRepository(pool()), savedStateHandle(startInTitleEdit = true))
        val existingListViewModel = TierDetailViewModel(FakeTierRepository(pool()), savedStateHandle(startInTitleEdit = false))

        assertEquals(true, newListViewModel.canDiscard.first())
        assertEquals(false, existingListViewModel.canDiscard.first())
    }

    @Test
    fun markTouched_flipsCanDiscardToFalse_andItStaysFalse() {
        val viewModel = TierDetailViewModel(FakeTierRepository(pool()), savedStateHandle(startInTitleEdit = true))

        viewModel.markTouched()
        assertEquals(false, viewModel.canDiscard.value)

        // One-way: a second call (standing in for "typed a character, then deleted it
        // again") must not flip it back.
        viewModel.markTouched()
        assertEquals(false, viewModel.canDiscard.value)
    }

    @Test
    fun addManualItem_touchesTheList() {
        val viewModel = TierDetailViewModel(FakeTierRepository(pool()), savedStateHandle(startInTitleEdit = true))

        viewModel.addManualItem("Dune", photoUri = null)

        assertEquals(false, viewModel.canDiscard.value)
    }

    @Test
    fun discardList_whenUntouched_deletesPermanentlyAndInvokesCallback() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle(startInTitleEdit = true))
        // discardList runs repository.deleteTierListPermanently(...) then onDiscarded()
        // sequentially on the same coroutine, so awaiting this deferred (completed from
        // inside onDiscarded) guarantees the delete already happened by the time the
        // assertions below run — no arbitrary polling needed.
        val discardedSignal = CompletableDeferred<Unit>()
        var discarded = 0

        viewModel.discardList {
            discarded++
            discardedSignal.complete(Unit)
        }
        discardedSignal.await()

        assertEquals(listOf(1L), repository.permanentlyDeletedIds)
        assertEquals(1, discarded)
    }

    @Test
    fun discardList_onceTouched_doesNothing() {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle(startInTitleEdit = true))
        var discarded = 0

        viewModel.markTouched()
        viewModel.discardList { discarded++ }

        assertEquals(emptyList<Long>(), repository.permanentlyDeletedIds)
        assertEquals(0, discarded)
    }

    private fun savedStateHandle(startInTitleEdit: Boolean = false): SavedStateHandle =
        SavedStateHandle(mapOf("tierListId" to 1L, "startInTitleEdit" to startInTitleEdit))

    private fun pool(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(id = 10, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
        ),
    )
}

private class FakeTierRepository(initial: TierList) : TierRepository {

    private var list = initial
    private var nextItemId = 1L
    var lastAddedImageUrl: String? = "not yet called"
        private set
    val attachedSources = mutableListOf<String>()
    val permanentlyDeletedIds = mutableListOf<Long>()

    override suspend fun getTierListById(id: Long): TierList? = list

    override suspend fun addMovieToPool(tierListId: Long, title: String, imageUrl: String?): Long {
        lastAddedImageUrl = imageUrl
        val id = nextItemId++
        val pool = list.tiers.single { it.isPool }
        val newItem = TierItem(id = id, title = title, imageUrl = imageUrl)
        val updatedPool = pool.copy(items = pool.items + newItem)
        list = list.copy(tiers = list.tiers.map { if (it.id == pool.id) updatedPool else it })
        return id
    }

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) {
        attachedSources += sourceUri
    }

    override suspend fun getAllTierLists(): List<TierList> = unsupported()
    override suspend fun createTierList(title: String): Long = unsupported()
    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = unsupported()
    override suspend fun renameTierList(id: Long, title: String) = unsupported()
    override suspend fun addMoviesToPool(tierListId: Long, movies: List<PoolMovieDraft>) = unsupported()
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
    override suspend fun deleteTierLists(ids: List<Long>) = unsupported()
    override suspend fun restoreTierLists(ids: List<Long>) = unsupported()
    override suspend fun deleteTierItem(id: Long) = unsupported()
    override suspend fun restoreTierItem(id: Long) = unsupported()
    override suspend fun deleteTierListPermanently(id: Long) {
        permanentlyDeletedIds += id
    }
    override suspend fun deleteTierItemPermanently(id: Long) = unsupported()
    override suspend fun emptyTrash() = unsupported()
    override suspend fun getTrashEntries(): List<TrashEntry> = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("Not used by TierDetailViewModel manual-entry tests")
}
