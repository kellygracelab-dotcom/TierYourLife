package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tierListId = savedStateHandle.toRoute<Route.TierDetail>().tierListId

    private val _state = MutableStateFlow<TierDetailUiState>(TierDetailUiState.Loading)
    val state: StateFlow<TierDetailUiState> = _state.asStateFlow()

    init {
        loadTierList()
    }

    fun loadTierList() {
        viewModelScope.launch {
            repository.getTierListById(tierListId).let {
                if (it != null) {
                    _state.value = TierDetailUiState.Success(it)
                } else {
                    _state.value = TierDetailUiState.Error("Tier list not found")
                }
            }
        }
    }

    fun addMovieToPool(title: String, imageUrl: String?) {
        viewModelScope.launch {
            repository.addMovieToPool(tierListId, title, imageUrl)
            loadTierList()
        }
    }

    fun addMoviesToPool(movies: List<PoolMovieDraft>) {
        viewModelScope.launch {
            repository.addMoviesToPool(tierListId, movies)
            loadTierList()
        }
    }

    // The photo, if any, is a picked gallery Uri — attachImageToItem is what copies it
    // into internal storage and stores the copy's path, exactly as it already does for
    // an existing item; addMovieToPool itself is only ever given imageUrl = null here,
    // never the picked Uri directly, so the raw gallery reference never reaches the DB.
    fun addManualItem(title: String, photoUri: String?) {
        viewModelScope.launch {
            val newItemId = repository.addMovieToPool(tierListId, title, imageUrl = null)
            if (photoUri != null) {
                repository.attachImageToItem(newItemId, photoUri)
            }
            loadTierList()
        }
    }

    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        viewModelScope.launch {
            repository.moveItem(itemId, toTierId, toPosition)
            loadTierList()
        }
    }

    // orderedTierIds excludes the pool — it never takes part in reordering and always
    // sorts after every id passed here, so leaving it out keeps it exactly where it was.
    fun reorderTiers(orderedTierIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderTiers(orderedTierIds)
            loadTierList()
        }
    }

    // Dropping a tier on the trash removes the tier but not its contents: each item is
    // moved into the pool first (the same repository call MoveItemSheet's "Back to the
    // pool" action already uses), and only the now-empty tier is deleted.
    fun deleteTierToPool(tierId: Long, poolId: Long, poolSize: Int, itemIds: List<Long>) {
        viewModelScope.launch {
            itemIds.forEachIndexed { index, itemId ->
                repository.moveItem(itemId, poolId, poolSize + index)
            }
            repository.deleteTier(tierId)
            loadTierList()
        }
    }

    // Undo for the above: recreates the tier (a new id — the old one is gone for good),
    // moves it back to its former position among the ranked tiers, then pulls each item
    // back out of the pool into it in their original order. All four calls already exist
    // on TierRepository; nothing here is new to domain or data.
    fun restoreTier(
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) {
        viewModelScope.launch {
            val newTierId = repository.addTier(tierListId, label, caption, colorLight, colorDark)

            val current = repository.getTierListById(tierListId)
            if (current != null) {
                val rankedIds = current.tiers.filterNot { it.isPool }.map { it.id }.toMutableList()
                rankedIds.remove(newTierId)
                rankedIds.add(position.coerceIn(0, rankedIds.size), newTierId)
                repository.reorderTiers(rankedIds)
            }

            itemIds.forEachIndexed { index, itemId ->
                repository.moveItem(itemId, newTierId, index)
            }
            loadTierList()
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteTierItem(itemId)
            loadTierList()
        }
    }

    fun restoreItem(itemId: Long) {
        viewModelScope.launch {
            repository.restoreTierItem(itemId)
            loadTierList()
        }
    }

    fun addTier(label: String, caption: String?, colorLight: String, colorDark: String) {
        viewModelScope.launch {
            repository.addTier(tierListId, label, caption, colorLight, colorDark)
            loadTierList()
        }
    }

    // Renaming and recoloring are two separate repository calls — there's no combined
    // one, and adding one would mean changing the domain layer, out of scope here — but
    // one user action ("save this tier's edits") should still mean one reload, not two,
    // so both run before loadTierList() rather than each triggering its own.
    fun editTier(id: Long, label: String, caption: String?, colorLight: String, colorDark: String) {
        viewModelScope.launch {
            repository.renameTier(id, label, caption)
            repository.updateTierColors(id, colorLight, colorDark)
            loadTierList()
        }
    }

    fun setDisplayMode(displayMode: TierListDisplayMode) {
        viewModelScope.launch {
            repository.setTierListDisplayMode(tierListId, displayMode)
            loadTierList()
        }
    }

    fun renameTierList(title: String) {
        viewModelScope.launch {
            repository.renameTierList(tierListId, title)
            loadTierList()
        }
    }
}
