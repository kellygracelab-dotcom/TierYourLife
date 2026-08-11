package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    val addedItemId: StateFlow<Long?> = savedStateHandle.getStateFlow(ADDED_ITEM_KEY, null)

    init {
        loadTierList()
    }

    fun markTouched() {
        if (_canDiscard.value) _canDiscard.value = false
    }

    fun discardList(onDiscarded: () -> Unit) {
        if (!_canDiscard.value) return
        viewModelScope.launch {
            repository.deleteTierListPermanently(tierListId)
            onDiscarded()
        }
    }

    fun loadTierList() {
        viewModelScope.launch {
            repository.getTierListById(tierListId).let {
                if (it != null) {
                    _state.value = TierDetailUiState.Success(it)
                } else {
                    _state.value = TierDetailUiState.Error
                }
            }
        }
    }

    fun addItemToPool(title: String, imageUrl: String?) {
        markTouched()
        viewModelScope.launch {
            repository.addItemToPool(tierListId, title, imageUrl)
            loadTierList()
        }
    }

    fun addItemsToPool(items: List<PoolItemDraft>) {
        markTouched()
        viewModelScope.launch {
            repository.addItemsToPool(tierListId, items)
            loadTierList()
        }
    }

    fun addManualItem(title: String, photoUris: List<String>) {
        markTouched()
        viewModelScope.launch {
            if (photoUris.isEmpty()) {
                repository.addItemToPool(tierListId, title, imageUrl = null)
            } else {
                val itemTitle = if (photoUris.size == 1) title else ""
                photoUris.forEach { photoUri ->
                    val newItemId = repository.addItemToPool(tierListId, itemTitle, imageUrl = null)
                    repository.attachImageToItem(newItemId, photoUri)
                }
            }
            loadTierList()
        }
    }

    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        markTouched()
        viewModelScope.launch {
            repository.moveItem(itemId, toTierId, toPosition)
            loadTierList()
        }
    }

    fun reorderTiers(orderedTierIds: List<Long>) {
        markTouched()
        viewModelScope.launch {
            repository.reorderTiers(orderedTierIds)
            loadTierList()
        }
    }

    fun deleteTierToPool(tierId: Long) {
        markTouched()
        viewModelScope.launch {
            repository.deleteTierToPool(tierId)
            loadTierList()
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
        viewModelScope.launch {
            repository.restoreTier(tierListId, label, caption, colorLight, colorDark, position, itemIds)
            loadTierList()
        }
    }

    fun deleteItem(itemId: Long) {
        markTouched()
        viewModelScope.launch {
            repository.deleteTierItem(itemId)
            loadTierList()
        }
    }

    fun restoreItem(itemId: Long) {
        markTouched()
        viewModelScope.launch {
            repository.restoreTierItem(itemId)
            loadTierList()
        }
    }

    fun addTier(label: String, caption: String?, colorLight: String, colorDark: String) {
        markTouched()
        viewModelScope.launch {
            repository.addTier(tierListId, label, caption, colorLight, colorDark)
            loadTierList()
        }
    }

    fun editTier(id: Long, label: String, caption: String?, colorLight: String, colorDark: String) {
        markTouched()
        viewModelScope.launch {
            repository.renameTier(id, label, caption)
            repository.updateTierColors(id, colorLight, colorDark)
            loadTierList()
        }
    }

    fun setDisplayMode(displayMode: TierListDisplayMode) {
        markTouched()
        viewModelScope.launch {
            repository.setTierListDisplayMode(tierListId, displayMode)
            loadTierList()
        }
    }

    fun renameTierList(title: String) {
        markTouched()
        viewModelScope.launch {
            repository.renameTierList(tierListId, title)
            loadTierList()
        }
    }

    fun consumeAddedItem() {
        if (addedItemId.value == null) return
        savedStateHandle[ADDED_ITEM_KEY] = null
        loadTierList()
    }

    fun removeAddedItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteTierItemPermanently(itemId)
            loadTierList()
        }
    }

    private companion object {
        const val ADDED_ITEM_KEY = "ai_added_item_id"
    }
}
