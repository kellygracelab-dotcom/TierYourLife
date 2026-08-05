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

    private val route = savedStateHandle.toRoute<Route.TierDetail>()
    private val tierListId = route.tierListId

    private val _state = MutableStateFlow<TierDetailUiState>(TierDetailUiState.Loading)
    val state: StateFlow<TierDetailUiState> = _state.asStateFlow()

    // Whether the leading control on this screen is still the discard cross rather
    // than the ordinary back arrow (docs/design-spec-home.md, section 12). Seeded from
    // startInTitleEdit — the same signal Route.TierDetail already uses for "this list
    // was just created" — but kept as its own flag: startInTitleEdit only ever drives
    // the title field's initial edit state, never the leading control, per the spec's
    // instruction to keep the two concepts separate.
    //
    // One-way by construction: markTouched() only ever sets this false, never back to
    // true, so retyping and clearing a title (or any other action that briefly returns
    // the list to its untouched shape) leaves the cross gone for good — a leading
    // control that oscillates under the user's thumb would be worse than one that
    // commits early.
    private val _canDiscard = MutableStateFlow(route.startInTitleEdit)
    val canDiscard: StateFlow<Boolean> = _canDiscard.asStateFlow()

    init {
        loadTierList()
    }

    // Called by every mutation below, and separately by the title field on its very
    // first keystroke (TierDetailScreen wires that call directly) — a keystroke that
    // never actually commits a rename still has to flip this, which is why it can't
    // simply live inside renameTierList().
    fun markTouched() {
        if (_canDiscard.value) _canDiscard.value = false
    }

    // The discard cross's action: a hard delete — never the trash, since an untouched
    // list has no name and no items, nothing worth recovering — followed by whatever
    // the caller does to leave (popping back to Home). Guarded by canDiscard so this
    // can only ever fire against a list that is still in its untouched state.
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
                    _state.value = TierDetailUiState.Error("Tier list not found")
                }
            }
        }
    }

    fun addMovieToPool(title: String, imageUrl: String?) {
        markTouched()
        viewModelScope.launch {
            repository.addMovieToPool(tierListId, title, imageUrl)
            loadTierList()
        }
    }

    fun addMoviesToPool(movies: List<PoolMovieDraft>) {
        markTouched()
        viewModelScope.launch {
            repository.addMoviesToPool(tierListId, movies)
            loadTierList()
        }
    }

    // One item per picked photo. The title only applies when there is a single item for it to
    // belong to — with several photos the dialog does not offer the field at all, so an empty
    // title here is the picture standing in as the item's identity, which the tile already
    // supports.
    //
    // attachImageToItem is what copies a picked Uri into internal storage and stores the path
    // of the copy; addMovieToPool is only ever passed imageUrl = null here, so the raw gallery
    // reference never reaches the database.
    fun addManualItem(title: String, photoUris: List<String>) {
        markTouched()
        viewModelScope.launch {
            if (photoUris.isEmpty()) {
                repository.addMovieToPool(tierListId, title, imageUrl = null)
            } else {
                val itemTitle = if (photoUris.size == 1) title else ""
                photoUris.forEach { photoUri ->
                    val newItemId = repository.addMovieToPool(tierListId, itemTitle, imageUrl = null)
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

    // Dropping a tier on the trash removes the tier but not its contents: each item is
    // moved into the pool first (the same repository call MoveItemSheet's "Back to the
    // pool" action already uses), and only the now-empty tier is deleted.
    fun deleteTierToPool(tierId: Long, poolId: Long, poolSize: Int, itemIds: List<Long>) {
        markTouched()
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
        markTouched()
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

    // Renaming and recoloring are two separate repository calls — there's no combined
    // one, and adding one would mean changing the domain layer, out of scope here — but
    // one user action ("save this tier's edits") should still mean one reload, not two,
    // so both run before loadTierList() rather than each triggering its own.
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
}
