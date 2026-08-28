package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
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

// Instrumented because viewModelScope needs Android's main dispatcher.
@RunWith(AndroidJUnit4::class)
class TierDetailViewModelTest {

    @Test
    fun addManualItem_withTitleOnly_addsToThePoolWithNoImage() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())

        viewModel.addManualItem("Dune", photoUris = emptyList())
        val state = viewModel.state.first {
            it is TierDetailUiState.Success && it.list.tiers.single().items.isNotEmpty()
        } as TierDetailUiState.Success

        val added = state.list.tiers.single().items.single()
        assertEquals("Dune", added.title)
        assertNull(added.imageUrl)
        assertEquals(emptyList<String>(), repository.attachedSources)
    }

    @Test
    fun addManualItem_withAPhoto_sendsTheGalleryUriOnlyToAttachImageToItem() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())

        viewModel.addManualItem("Dune", photoUris = listOf("content://gallery/42"))
        viewModel.state.first {
            it is TierDetailUiState.Success && it.list.tiers.single().items.isNotEmpty()
        }

        assertEquals(listOf("content://gallery/42"), repository.attachedSources)
        assertNull(repository.lastAddedImageUrl)
    }

    @Test
    fun addManualItem_withSeveralPhotos_addsOneItemPerPhoto() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())
        val picked = listOf("content://gallery/1", "content://gallery/2", "content://gallery/3")

        viewModel.addManualItem("", photoUris = picked)
        val state = viewModel.state.first {
            it is TierDetailUiState.Success && it.list.tiers.single().items.size == picked.size
        } as TierDetailUiState.Success

        assertEquals(picked, repository.attachedSources)
        assertEquals(listOf("", "", ""), state.list.tiers.single().items.map { it.title })
        assertNull(repository.lastAddedImageUrl)
    }

    @Test
    fun reorderTiers_emitsTheNewOrderSynchronously_beforeTheRepositoryResponds() = runBlocking {
        val repository = FakeTierRepository(rankedList())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())
        viewModel.state.first { it is TierDetailUiState.Success }

        viewModel.reorderTiers(listOf(20L, 10L))

        val optimistic = viewModel.state.value as TierDetailUiState.Success
        assertEquals(listOf(20L, 10L, 30L), optimistic.list.tiers.map { it.id })

        repository.reorderStarted.await()
        assertEquals(listOf(20L, 10L), repository.lastReorderedIds)
        val stillOptimistic = viewModel.state.value as TierDetailUiState.Success
        assertEquals(listOf(20L, 10L, 30L), stillOptimistic.list.tiers.map { it.id })

        repository.releaseReorder()
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

    private fun rankedList(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(id = 10, label = "S", colorLight = "#000000", colorDark = "#000000", items = emptyList()),
            Tier(id = 20, label = "A", colorLight = "#000000", colorDark = "#000000", items = emptyList()),
            Tier(id = 30, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
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
    var lastReorderedIds: List<Long>? = null
        private set
    val reorderStarted = CompletableDeferred<Unit>()
    private val reorderGate = CompletableDeferred<Unit>()

    fun releaseReorder() {
        reorderGate.complete(Unit)
    }

    override suspend fun getTierListById(id: Long): TierList? = list

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long {
        lastAddedImageUrl = imageUrl
        val id = nextItemId++
        val pool = list.tiers.single { it.isPool }
        val newItem = TierItem(id = id, title = title, imageUrl = imageUrl, source = source)
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
    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = unsupported()
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
    override suspend fun reorderTiers(orderedTierIds: List<Long>) {
        lastReorderedIds = orderedTierIds
        reorderStarted.complete(Unit)
        reorderGate.await()
        val orderedSet = orderedTierIds.toSet()
        val byId = list.tiers.associateBy { it.id }
        val queue = ArrayDeque(orderedTierIds.mapNotNull { byId[it] })
        list = list.copy(tiers = list.tiers.map { tier -> if (tier.id in orderedSet) queue.removeFirst() else tier })
    }
    override suspend fun deleteTierLists(ids: List<Long>) = unsupported()
    override suspend fun restoreTierLists(ids: List<Long>) = unsupported()
    override suspend fun deleteTierItem(id: Long) = unsupported()
    override suspend fun restoreTierItem(id: Long) = unsupported()
    override suspend fun deleteTierListPermanently(id: Long) {
        permanentlyDeletedIds += id
    }
    override suspend fun deleteTierItemPermanently(id: Long) {
        permanentlyDeletedIds += id
        list = list.copy(
            tiers = list.tiers.map { tier -> tier.copy(items = tier.items.filterNot { it.id == id }) },
        )
    }
    override suspend fun emptyTrash() = unsupported()
    override suspend fun getTrashEntries(): List<TrashEntry> = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("Not used by TierDetailViewModel manual-entry tests")
}
