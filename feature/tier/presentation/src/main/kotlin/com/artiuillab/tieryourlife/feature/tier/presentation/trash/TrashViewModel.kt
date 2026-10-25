package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
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

    /**
     * Restored private when its snapshot could be taken down. When it could
     * not, it comes back still knowing it is public, so its own switch can
     * take it down later; forgetting the id would leave it public for good.
     */
    fun restoreList(id: Long) {
        mutate("Restoring a list") {
            val stillPublic = takeDownPublished(listOf(id))
            if (id !in stillPublic) repository.setPublished(id, null, null)
            repository.restoreTierLists(listOf(id))
            if (stillPublic.isNotEmpty()) messages.send(UserMessage.PublishedListStillPublic)
        }
    }

    fun restoreItem(id: Long) {
        mutate("Restoring an item") { repository.restoreTierItem(id) }
    }

    /** Kept while its snapshot is still public: deleted, nothing on this phone could take that down any more. */
    fun removeListPermanently(id: Long) {
        mutate("Deleting a list for good") {
            if (takeDownPublished(listOf(id)).isEmpty()) {
                repository.deleteTierListPermanently(id)
            } else {
                messages.send(UserMessage.PublishedListStillPublic)
            }
        }
    }

    fun removeItemPermanently(id: Long) {
        mutate("Deleting an item for good") { repository.deleteTierItemPermanently(id) }
    }

    fun emptyTrash() {
        mutate("Emptying the trash") {
            val stillPublic = takeDownPublished(trashedListIds())
            if (stillPublic.isEmpty()) {
                repository.emptyTrash()
                return@mutate
            }
            // Everything else goes; the lists still out there stay, for the next try.
            (_state.value as? TrashUiState.Success)?.entries.orEmpty().forEach { entry ->
                when (entry) {
                    is TrashEntry.DeletedList -> if (entry.id !in stillPublic) repository.deleteTierListPermanently(entry.id)
                    is TrashEntry.DeletedItem -> repository.deleteTierItemPermanently(entry.id)
                }
            }
            messages.send(UserMessage.PublishedListStillPublic)
        }
    }

    /**
     * Lists trashed before deleting also took them out of the community still
     * carry a snapshot. Emptying the trash is the last chance to remove it.
     * Answers with the ids whose snapshot is still out there.
     */
    private suspend fun takeDownPublished(ids: List<Long>): Set<Long> {
        val entries = (_state.value as? TrashUiState.Success)?.entries.orEmpty()
        return entries.filterIsInstance<TrashEntry.DeletedList>()
            .filter { it.id in ids }
            .mapNotNull { list ->
                val publishedId = list.publishedId ?: return@mapNotNull null
                community.unpublish(publishedId).fold(
                    onSuccess = { null },
                    onFailure = {
                        Timber.w(it, "Could not take a trashed list out of the community")
                        list.id
                    },
                )
            }
            .toSet()
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
