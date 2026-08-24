package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.ordering.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "TierDetail"

@HiltViewModel
class TierDetailViewModel @Inject constructor(
    private val repository: TierRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.TierDetail>()
    private val tierListId = route.tierListId

    private val _state = MutableStateFlow<TierDetailUiState>(TierDetailUiState.Loading)
    val state: StateFlow<TierDetailUiState> = _state.asStateFlow()

    private val _canDiscard = MutableStateFlow(route.startInTitleEdit)
    val canDiscard: StateFlow<Boolean> = _canDiscard.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        loadTierList()
    }

    fun markTouched() {
        if (_canDiscard.value) _canDiscard.value = false
    }

    fun discardList(onDiscarded: () -> Unit) {
        if (!_canDiscard.value) return
        viewModelScope.launch {
            val deleted = messages.guard("Discarding list") {
                repository.deleteTierListPermanently(tierListId)
            }
            if (deleted) onDiscarded()
        }
    }

    fun loadTierList() {
        viewModelScope.launch { reloadTierList() }
    }

    fun addItemToPool(title: String, imageUrl: String?) {
        markTouched()
        mutate("Adding item to pool") {
            repository.addItemToPool(tierListId, title, imageUrl)
        }
    }

    fun addItemsToPool(items: List<PoolItemDraft>) {
        markTouched()
        mutate("Adding items to pool") {
            repository.addItemsToPool(tierListId, items)
        }
    }

    fun addManualItem(title: String, photoUris: List<String>) {
        markTouched()
        mutate("Adding a manual item") {
            if (photoUris.isEmpty()) {
                repository.addItemToPool(tierListId, title, imageUrl = null)
            } else {
                val itemTitle = if (photoUris.size == 1) title else ""
                photoUris.forEach { photoUri ->
                    val newItemId = repository.addItemToPool(tierListId, itemTitle, imageUrl = null)
                    repository.attachImageToItem(newItemId, photoUri)
                }
            }
        }
    }

    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        markTouched()
        val current = _state.value
        if (current is TierDetailUiState.Success) {
            _state.value = TierDetailUiState.Success(current.list.withItemMoved(itemId, toTierId, toPosition))
        }
        mutate("Moving an item") {
            repository.moveItem(itemId, toTierId, toPosition)
        }
    }

    fun reorderTiers(orderedTierIds: List<Long>) {
        markTouched()
        val current = _state.value
        if (current is TierDetailUiState.Success) {
            val orderedSet = orderedTierIds.toSet()
            val byId = current.list.tiers.associateBy { it.id }
            val queue = ArrayDeque(orderedTierIds.mapNotNull { byId[it] })
            val reorderedTiers = current.list.tiers.map { tier ->
                if (tier.id in orderedSet) queue.removeFirst() else tier
            }
            _state.value = TierDetailUiState.Success(current.list.copy(tiers = reorderedTiers))
        }
        mutate("Reordering tiers") {
            repository.reorderTiers(orderedTierIds)
        }
    }

    fun deleteTierToPool(tierId: Long) {
        markTouched()
        mutate("Deleting a tier") {
            repository.deleteTierToPool(tierId)
        }
    }

    fun restoreTier(
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) {
        markTouched()
        mutate("Restoring a tier") {
            repository.restoreTier(tierListId, label, caption, colorLight, colorDark, position, itemIds)
        }
    }

    fun deleteItem(itemId: Long) {
        markTouched()
        mutate("Deleting an item") {
            repository.deleteTierItem(itemId)
        }
    }

    fun restoreItem(itemId: Long) {
        markTouched()
        mutate("Restoring an item") {
            repository.restoreTierItem(itemId)
        }
    }

    fun addTier(label: String, caption: String?, colorLight: String, colorDark: String) {
        markTouched()
        mutate("Adding a tier") {
            repository.addTier(tierListId, label, caption, colorLight, colorDark)
        }
    }

    fun editTier(id: Long, label: String, caption: String?, colorLight: String, colorDark: String) {
        markTouched()
        mutate("Editing a tier") {
            repository.renameTier(id, label, caption)
            repository.updateTierColors(id, colorLight, colorDark)
        }
    }

    fun setDisplayMode(displayMode: TierListDisplayMode) {
        markTouched()
        mutate("Changing the display mode") {
            repository.setTierListDisplayMode(tierListId, displayMode)
        }
    }

    fun renameTierList(title: String) {
        markTouched()
        mutate("Renaming the list") {
            repository.renameTierList(tierListId, title)
        }
    }

    fun removeAddedItems(itemIds: List<Long>) {
        mutate("Removing added items") {
            itemIds.forEach { itemId -> repository.deleteTierItemPermanently(itemId) }
        }
    }

    private fun mutate(operation: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            messages.guard(operation) { block() }
            reloadTierList()
        }
    }

    private suspend fun reloadTierList() {
        val hasVisibleList = _state.value is TierDetailUiState.Success
        try {
            val list = repository.getTierListById(tierListId)
            _state.value = if (list != null) {
                TierDetailUiState.Success(list)
            } else {
                TierDetailUiState.Error
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Loading the tier list failed", e)
            if (hasVisibleList) {
                messages.send(UserMessage.ActionFailed)
            } else {
                _state.value = TierDetailUiState.Error
            }
        }
    }
}
