package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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

    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        viewModelScope.launch {
            repository.moveItem(itemId, toTierId, toPosition)
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
