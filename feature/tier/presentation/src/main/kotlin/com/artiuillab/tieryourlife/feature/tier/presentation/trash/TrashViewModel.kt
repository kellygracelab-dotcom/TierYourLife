package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: TierRepository,
    private val community: CommunityRepository,
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

    /** Restored private: its snapshot is either gone or about to be. */
    fun restoreList(id: Long) {
        mutate("Restoring a list") {
            takeDownPublished(listOf(id))
            repository.setPublishedId(id, null)
            repository.restoreTierLists(listOf(id))
        }
    }

    fun restoreItem(id: Long) {
        mutate("Restoring an item") { repository.restoreTierItem(id) }
    }

    fun removeListPermanently(id: Long) {
        mutate("Deleting a list for good") {
            takeDownPublished(listOf(id))
            repository.deleteTierListPermanently(id)
        }
    }

    fun removeItemPermanently(id: Long) {
        mutate("Deleting an item for good") { repository.deleteTierItemPermanently(id) }
    }

    fun emptyTrash() {
        mutate("Emptying the trash") {
            takeDownPublished(trashedListIds())
            repository.emptyTrash()
        }
    }

    /**
     * Lists trashed before deleting also took them out of the community still
     * carry a snapshot. Emptying the trash is the last chance to remove it.
     */
    private suspend fun takeDownPublished(ids: List<Long>) {
        val entries = (_state.value as? TrashUiState.Success)?.entries.orEmpty()
        entries.filterIsInstance<TrashEntry.DeletedList>()
            .filter { it.id in ids }
            .mapNotNull { it.publishedId }
            .forEach { publishedId ->
                community.unpublish(publishedId)
                    .onFailure { Timber.w(it, "Could not take a trashed list out of the community") }
            }
    }

    private fun trashedListIds(): List<Long> =
        (_state.value as? TrashUiState.Success)?.entries.orEmpty()
            .filterIsInstance<TrashEntry.DeletedList>()
            .map { it.id }

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
            Timber.w(e, "Loading the trash failed")
            _state.value = TrashUiState.Error
        }
    }
}
