package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: TierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TrashUiState>(TrashUiState.Loading)
    val state: StateFlow<TrashUiState> = _state.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            try {
                _state.value = TrashUiState.Success(repository.getTrashEntries())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = TrashUiState.Error(e.message ?: "Failed to load trash")
            }
        }
    }

    // Restore never confirms — the row leaving the trash is the confirmation
    // (docs/design-spec-home.md, section 6).
    fun restoreList(id: Long) {
        viewModelScope.launch {
            repository.restoreTierLists(listOf(id))
            loadEntries()
        }
    }

    fun restoreItem(id: Long) {
        viewModelScope.launch {
            repository.restoreTierItem(id)
            loadEntries()
        }
    }

    fun removeListPermanently(id: Long) {
        viewModelScope.launch {
            repository.deleteTierListPermanently(id)
            loadEntries()
        }
    }

    fun removeItemPermanently(id: Long) {
        viewModelScope.launch {
            repository.deleteTierItemPermanently(id)
            loadEntries()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            loadEntries()
        }
    }
}
