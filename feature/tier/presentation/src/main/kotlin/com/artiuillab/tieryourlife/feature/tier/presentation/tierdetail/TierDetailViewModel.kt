package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.tierDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private val tierListId = savedStateHandle.tierDetailRoute().tierListId

    private val _screen = MutableStateFlow(TierDetailScreenState())
    val screen: StateFlow<TierDetailScreenState> = _screen.asStateFlow()

    // Views onto one value, so a write is seen at once and the screen gets one frame per change.
    private var board: TierDetailUiState
        get() = _screen.value.board
        set(value) = _screen.update { it.copy(board = value) }
    private var allLists: List<TierList>
        get() = _screen.value.allLists
        set(value) = _screen.update { it.copy(allLists = value) }
    private var publishing: Boolean
        get() = _screen.value.publish.publishing
        set(value) = updatePublish { it.copy(publishing = value) }
    private var publishError: PublishError?
        get() = _screen.value.publish.error
        set(value) = updatePublish { it.copy(error = value) }
    private var publishedIsBehind: Boolean
        get() = _screen.value.publish.publishedIsBehind
        set(value) = updatePublish { it.copy(publishedIsBehind = value) }
    private var publicPending: Boolean?
        get() = _screen.value.publish.publicPending
        set(value) = updatePublish { it.copy(publicPending = value) }
    private var categoryWanted: Boolean
        get() = _screen.value.publish.categoryWanted
        set(value) = updatePublish { it.copy(categoryWanted = value) }

    private fun updatePublish(change: (PublishState) -> PublishState) =
        _screen.update { it.copy(publish = change(it.publish)) }

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        viewModelScope.launch {
            accountRepository.account.collect { account ->
                _screen.update { it.copy(signedIn = account is Account.SignedIn) }
            }
        }
        loadTierList()
    }

    /** Offered where somebody stands after changing the board. The same act as publishing, over the same id, so the link keeps working. */
    fun updatePublished() = setPublic(true)

    private fun refreshPublishedStanding() {
        viewModelScope.launch {
            val standing = runSuspendCatching { repository.publishedStanding() }
                .onFailure { Timber.w(it, "Comparing the published copy failed") }
                .getOrNull() ?: return@launch
            val id = (board as? TierDetailUiState.Success)?.list?.publishedId
            publishedIsBehind = id != null && id in standing.knownBehind
        }
    }

    fun setPublic(public: Boolean) {
        val list = (board as? TierDetailUiState.Success)?.list ?: return
        if (publishing) return
        publishError = null

        if (public && list.tiers.none { it.items.isNotEmpty() }) {
            publishError = PublishError.NothingToPublish
            return
        }
        if (public && list.category == null) {
            categoryWanted = true
            return
        }

        publishing = true
        publicPending = public
        viewModelScope.launch {
            val result: Result<Published?> = if (public) {
                community.publish(list)
            } else {
                takeDown(list)
            }
            result.fold(
                onSuccess = { published -> recordPublished(list, published) },
                onFailure = { failure ->
                    publishError = (failure as? PublishRefused)?.error ?: PublishError.Unknown
                    if (publishError == PublishError.TopicUnavailable) forgetTopic()
                },
            )
            publishing = false
            publicPending = null
        }
    }

    /** A ranking comes down from the list it sits beside; a list of its own comes down by its id. */
    private suspend fun takeDown(list: TierList): Result<Published?> {
        val rankingOf = list.rankingOf
        val publishedId = list.publishedId
        return when {
            rankingOf != null -> community.withdrawRanking(rankingOf).map { null }
            publishedId != null -> community.unpublish(publishedId).map { null }
            else -> Result.success(null)
        }
    }

    private suspend fun recordPublished(list: TierList, published: Published?) {
        var waiting: String? = null
        val recorded = messages.guard("Recording the published list") {
            // A ranking has no id of its own: it is out as part of the list it answers.
            repository.setPublished(tierListId, published?.takeUnless { it.ranking }?.id, published?.fingerprint)
            repository.setRankingOf(tierListId, published?.takeIf { it.ranking }?.id)
            // The server named the topic: a name that waited is a topic now, though maybe one still waiting.
            published?.topicId?.let { topicId ->
                waiting = waitingName(list, topicId)
                repository.setTopic(tierListId, topicId, list.topicName ?: list.newTopicName, waiting)
            }
        }
        // Reading the list back would land after publicPending clears
        // and spring the switch back for a frame. The id and the topic are all that changed.
        if (recorded) showPublished(published, waiting)
        // Just sent, so nothing is behind any more.
        publishedIsBehind = false
        if (recorded && published?.ranking == true) messages.send(UserMessage.PublishedAsRanking)
    }

    /** Finishes the publish that asked for it: a tap becoming two is smaller than a tap that only refuses. */
    fun setCategory(category: ListCategory) {
        val publishNext = categoryWanted
        categoryWanted = false
        viewModelScope.launch {
            messages.guard("Setting the category") { repository.setCategory(tierListId, category) }
            reloadTierList()
            if (publishNext) setPublic(true)
        }
    }

    fun categoryNotChosen() {
        categoryWanted = false
    }

    fun loadTierList() {
        viewModelScope.launch { reloadTierList() }
        viewModelScope.launch {
            runSuspendCatching { repository.getAllTierLists() }
                .onSuccess { lists -> allLists = lists }
                .onFailure { Timber.w(it, "Reading the lists beside this board failed") }
        }
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
        val current = board
        if (current is TierDetailUiState.Success) {
            board = TierDetailUiState.Success(current.list.withItemMoved(itemId, toTierId, toPosition))
        }
        mutate("Moving an item") {
            repository.moveItem(itemId, toTierId, toPosition)
        }
    }

    fun reorderTiers(orderedTierIds: List<Long>) {
        val current = board
        if (current is TierDetailUiState.Success) {
            val orderedSet = orderedTierIds.toSet()
            val byId = current.list.tiers.associateBy { it.id }
            val queue = ArrayDeque(orderedTierIds.mapNotNull { byId[it] })
            val reorderedTiers = current.list.tiers.map { tier ->
                if (tier.id in orderedSet) queue.removeFirst() else tier
            }
            board = TierDetailUiState.Success(current.list.copy(tiers = reorderedTiers))
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

    private fun showPublished(published: Published?, waiting: String?) {
        val current = board
        if (current is TierDetailUiState.Success) {
            val list = current.list
            val out = list.copy(
                publishedId = published?.takeUnless { it.ranking }?.id,
                rankingOf = published?.takeIf { it.ranking }?.id,
            )
            val topicId = published?.topicId
            board = TierDetailUiState.Success(
                if (topicId == null) {
                    out
                } else {
                    out.copy(topicId = topicId, topicName = list.topicName ?: list.newTopicName, newTopicName = waiting)
                },
            )
        }
    }

    /** A name the server had not seen is a topic now; whether it still waits for a moderator decides what the row says. */
    private suspend fun waitingName(list: TierList, topicId: String): String? {
        val proposed = list.newTopicName?.takeIf { list.topicId == null } ?: return null
        val settled = community.topic(topicId).map { it.status != TopicStatus.Pending }.getOrDefault(false)
        return proposed.takeUnless { settled }
    }

    /** A topic the server will not take any more is no topic: the next attempt goes without it. */
    private fun forgetTopic() {
        viewModelScope.launch {
            val recorded = messages.guard("Forgetting a refused topic") { repository.setTopic(tierListId, null, null, null) }
            val current = board
            if (recorded && current is TierDetailUiState.Success) {
                board = TierDetailUiState.Success(current.list.copy(topicId = null, topicName = null, newTopicName = null))
            }
        }
    }

    /** Only the message the list itself can answer is dropped; a server refusal stands until the next attempt. */
    private fun dropSettledPublishError(list: TierList) {
        val settled = publishError == PublishError.NothingToPublish &&
            list.tiers.any { it.items.isNotEmpty() }
        if (settled) publishError = null
    }

    private fun mutate(operation: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            messages.guard(operation) { block() }
            reloadTierList()
        }
    }

    private suspend fun reloadTierList() {
        val hasVisibleList = board is TierDetailUiState.Success
        try {
            val list = repository.getTierListById(tierListId)
            board = if (list != null) {
                dropSettledPublishError(list)
                if (list.publishedId != null) refreshPublishedStanding()
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
                board = TierDetailUiState.Error
            }
        }
    }
}
