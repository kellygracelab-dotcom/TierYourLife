package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.ordering.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TierDetailViewModel @Inject constructor(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    accountRepository: AccountRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val signedIn: StateFlow<Boolean> = accountRepository.account
        .map { it is Account.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()

    private val _publishError = MutableStateFlow<PublishError?>(null)
    val publishError: StateFlow<PublishError?> = _publishError.asStateFlow()

    /**
     * What the switch should read while the request is in flight. Waiting for
     * the server to answer left it sitting on the old position for a second,
     * which reads as a tap that missed.
     */
    private val _publicPending = MutableStateFlow<Boolean?>(null)
    val publicPending: StateFlow<Boolean?> = _publicPending.asStateFlow()

    private val tierListId = savedStateHandle.toRoute<Route.TierDetail>().tierListId

    private val _state = MutableStateFlow<TierDetailUiState>(TierDetailUiState.Loading)
    val state: StateFlow<TierDetailUiState> = _state.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        loadTierList()
    }

    fun setPublic(public: Boolean) {
        val list = (_state.value as? TierDetailUiState.Success)?.list ?: return
        if (_publishing.value) return
        _publishError.value = null

        if (public && list.tiers.none { it.items.isNotEmpty() }) {
            _publishError.value = PublishError.NothingToPublish
            return
        }
        if (public && list.category == null) {
            _publishError.value = PublishError.NoCategory
            return
        }

        _publishing.value = true
        _publicPending.value = public
        viewModelScope.launch {
            val result: Result<String?> = if (public) {
                community.publish(list)
            } else {
                list.publishedId?.let { community.unpublish(it).map { _ -> null } } ?: Result.success(null)
            }
            result.fold(
                onSuccess = { publishedId ->
                    val recorded = messages.guard("Recording the published list") {
                        repository.setPublishedId(tierListId, publishedId)
                    }
                    // Reading the list back would land after publicPending is
                    // cleared below, so the switch would spring back to the old
                    // position for a frame or two. The id is all that changed.
                    if (recorded) showPublishedId(publishedId)
                },
                onFailure = { failure ->
                    _publishError.value = (failure as? PublishRefused)?.error ?: PublishError.Unknown
                },
            )
            _publishing.value = false
            _publicPending.value = null
        }
    }

    fun setCategory(category: ListCategory) {
        mutate("Setting the category") { repository.setCategory(tierListId, category) }
    }

    fun setCoverImageUrl(coverImageUrl: String?) {
        mutate("Setting the cover") { repository.setCoverImageUrl(tierListId, coverImageUrl) }
    }

    fun loadTierList() {
        viewModelScope.launch { reloadTierList() }
    }

    fun addItemToPool(title: String, imageUrl: String?) {
        mutate("Adding item to pool") {
            repository.addItemToPool(tierListId, title, imageUrl)
        }
    }

    fun addItemsToPool(items: List<PoolItemDraft>) {
        mutate("Adding items to pool") {
            repository.addItemsToPool(tierListId, items)
        }
    }

    fun addManualItem(title: String, photoUris: List<String>) {
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
        val current = _state.value
        if (current is TierDetailUiState.Success) {
            _state.value = TierDetailUiState.Success(current.list.withItemMoved(itemId, toTierId, toPosition))
        }
        mutate("Moving an item") {
            repository.moveItem(itemId, toTierId, toPosition)
        }
    }

    fun reorderTiers(orderedTierIds: List<Long>) {
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
        mutate("Restoring a tier") {
            repository.restoreTier(tierListId, label, caption, colorLight, colorDark, position, itemIds)
        }
    }

    fun deleteItem(itemId: Long) {
        mutate("Deleting an item") {
            repository.deleteTierItem(itemId)
        }
    }

    fun restoreItem(itemId: Long) {
        mutate("Restoring an item") {
            repository.restoreTierItem(itemId)
        }
    }

    fun addTier(label: String, caption: String?, colorLight: String, colorDark: String) {
        mutate("Adding a tier") {
            repository.addTier(tierListId, label, caption, colorLight, colorDark)
        }
    }

    fun editTier(id: Long, label: String, caption: String?, colorLight: String, colorDark: String) {
        mutate("Editing a tier") {
            repository.renameTier(id, label, caption)
            repository.updateTierColors(id, colorLight, colorDark)
        }
    }

    fun setDisplayMode(displayMode: TierListDisplayMode) {
        mutate("Changing the display mode") {
            repository.setTierListDisplayMode(tierListId, displayMode)
        }
    }

    fun renameTierList(title: String) {
        mutate("Renaming the list") {
            repository.renameTierList(tierListId, title)
        }
    }

    fun removeAddedItems(itemIds: List<Long>) {
        mutate("Removing added items") {
            itemIds.forEach { itemId -> repository.deleteTierItemPermanently(itemId) }
        }
    }

    private fun showPublishedId(publishedId: String?) {
        val current = _state.value
        if (current is TierDetailUiState.Success) {
            _state.value = TierDetailUiState.Success(current.list.copy(publishedId = publishedId))
        }
    }

    /**
     * "Choose a category before you publish this list" has to stop saying that
     * once a category is chosen. Only the two the list itself can answer are
     * dropped; a refusal from the server stands until the next attempt.
     */
    private fun dropSettledPublishError(list: TierList) {
        val settled = when (_publishError.value) {
            PublishError.NoCategory -> list.category != null
            PublishError.NothingToPublish -> list.tiers.any { it.items.isNotEmpty() }
            else -> false
        }
        if (settled) _publishError.value = null
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
                dropSettledPublishError(list)
                TierDetailUiState.Success(list)
            } else {
                TierDetailUiState.Error
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Loading the tier list failed")
            if (hasVisibleList) {
                messages.send(UserMessage.ActionFailed)
            } else {
                _state.value = TierDetailUiState.Error
            }
        }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
