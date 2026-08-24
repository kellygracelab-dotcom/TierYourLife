package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val AWAIT_TIMEOUT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class TierDetailViewModelErrorsTest {

    @Test
    fun aFailingMoveRollsTheOptimisticChangeBackAndReportsIt() = runBlocking {
        val repository = FailingTierRepository(twoTiers())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())
        awaitSuccess(viewModel)

        viewModel.moveItem(itemId = 1L, toTierId = 20L, toPosition = 0)

        val message = withTimeout(AWAIT_TIMEOUT_MILLIS) { viewModel.userMessages.first() }
        assertEquals(UserMessage.ActionFailed, message)

        val state = withTimeout(AWAIT_TIMEOUT_MILLIS) {
            viewModel.state.first { it is TierDetailUiState.Success && it.list == repository.stored }
        } as TierDetailUiState.Success
        assertEquals(listOf(1L), state.list.tiers.first { it.id == 10L }.items.map { it.id })
        assertTrue(state.list.tiers.first { it.id == 20L }.items.isEmpty())
    }

    @Test
    fun aFailingMutationDoesNotTakeTheScreenDown() = runBlocking {
        val repository = FailingTierRepository(twoTiers())
        val viewModel = TierDetailViewModel(repository, savedStateHandle())
        awaitSuccess(viewModel)

        viewModel.renameTierList("New title")
        withTimeout(AWAIT_TIMEOUT_MILLIS) { viewModel.userMessages.first() }

        val state = withTimeout(AWAIT_TIMEOUT_MILLIS) { viewModel.state.first { it is TierDetailUiState.Success } }
        assertTrue(state is TierDetailUiState.Success)
    }

    @Test
    fun aFailingDiscardKeepsTheUserOnTheScreen() = runBlocking {
        val repository = FailingTierRepository(twoTiers())
        val viewModel = TierDetailViewModel(repository, savedStateHandle(startInTitleEdit = true))
        awaitSuccess(viewModel)

        var discarded = false
        viewModel.discardList { discarded = true }
        withTimeout(AWAIT_TIMEOUT_MILLIS) { viewModel.userMessages.first() }

        assertFalse(discarded)
    }

    @Test
    fun aFailingFirstLoadShowsTheErrorState() = runBlocking {
        val repository = FailingTierRepository(twoTiers(), failReads = true)
        val viewModel = TierDetailViewModel(repository, savedStateHandle())

        val state = withTimeout(AWAIT_TIMEOUT_MILLIS) {
            viewModel.state.first { it is TierDetailUiState.Error }
        }
        assertTrue(state is TierDetailUiState.Error)
    }

    private suspend fun awaitSuccess(viewModel: TierDetailViewModel) {
        withTimeout(AWAIT_TIMEOUT_MILLIS) { viewModel.state.first { it is TierDetailUiState.Success } }
    }

    private fun savedStateHandle(startInTitleEdit: Boolean = false) = SavedStateHandle(
        mapOf("tierListId" to 1L, "startInTitleEdit" to startInTitleEdit),
    )

    private fun twoTiers() = TierList(
        id = 1L,
        title = "List",
        displayMode = TierListDisplayMode.WRAP,
        tiers = listOf(
            Tier(
                id = 10L,
                label = "S",
                caption = null,
                colorLight = "#FFFFFF",
                colorDark = "#000000",
                isPool = false,
                items = listOf(TierItem(id = 1L, title = "Dune", imageUrl = null, source = TierItemSource.MANUAL)),
            ),
            Tier(
                id = 20L,
                label = "Pool",
                caption = null,
                colorLight = "#FFFFFF",
                colorDark = "#000000",
                isPool = true,
                items = emptyList(),
            ),
        ),
    )
}

private class FailingTierRepository(
    val stored: TierList,
    private val failReads: Boolean = false,
) : TierRepository {

    override suspend fun getTierListById(id: Long): TierList? {
        if (failReads) throw IllegalStateException("read failed")
        return stored
    }

    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) = fail()

    override suspend fun renameTierList(id: Long, title: String) = fail()

    override suspend fun deleteTierListPermanently(id: Long) = fail()

    override suspend fun getAllTierLists(): List<TierList> = fail()

    override suspend fun createTierList(title: String): Long = fail()

    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = fail()

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long = fail()

    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = fail()

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) = fail()

    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long = fail()

    override suspend fun renameTier(id: Long, label: String, caption: String?) = fail()

    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) = fail()

    override suspend fun deleteTierToPool(id: Long) = fail()

    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) = fail()

    override suspend fun reorderTiers(orderedTierIds: List<Long>) = fail()

    override suspend fun deleteTierLists(ids: List<Long>) = fail()

    override suspend fun restoreTierLists(ids: List<Long>) = fail()

    override suspend fun deleteTierItem(id: Long) = fail()

    override suspend fun restoreTierItem(id: Long) = fail()

    override suspend fun deleteTierItemPermanently(id: Long) = fail()

    override suspend fun emptyTrash() = fail()

    override suspend fun getTrashEntries(): List<TrashEntry> = fail()

    private fun fail(): Nothing = throw IllegalStateException("storage failed")
}
