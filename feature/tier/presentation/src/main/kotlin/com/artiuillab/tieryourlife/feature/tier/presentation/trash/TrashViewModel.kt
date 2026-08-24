package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
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

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch { reloadEntries() }
    }

    fun restoreList(id: Long) {
        mutate("Restoring a list") { repository.restoreTierLists(listOf(id)) }
    }

    fun restoreItem(id: Long) {
        mutate("Restoring an item") { repository.restoreTierItem(id) }
    }

    fun removeListPermanently(id: Long) {
        mutate("Deleting a list for good") { repository.deleteTierListPermanently(id) }
    }

    fun removeItemPermanently(id: Long) {
        mutate("Deleting an item for good") { repository.deleteTierItemPermanently(id) }
    }

    fun emptyTrash() {
        mutate("Emptying the trash") { repository.emptyTrash() }
    }

    private fun mutate(operation: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            messages.guard(operation) { block() }
            reloadEntries()
        }
    }

    private suspend fun reloadEntries() {
        try {
            _state.value = TrashUiState.Success(repository.getTrashEntries())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = TrashUiState.Error
        }
    }
}
