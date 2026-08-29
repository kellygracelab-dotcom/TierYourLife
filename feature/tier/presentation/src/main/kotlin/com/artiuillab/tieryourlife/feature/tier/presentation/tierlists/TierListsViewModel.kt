package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

private const val COMMUNITY_SEARCH_DELAY_MILLIS = 300L

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private var lastLoadedLists: List<TierList> = emptyList()

    private var mode: HomeMode = HomeMode.Browsing
    private var tab: HomeTab = HomeTab.Mine
    private var communityFeed: CommunityFeed = CommunityFeed.Loading
    private var communityCategory: ListCategory? = null
    private var communitySearch: Job? = null

    fun loadTierLists() {
        viewModelScope.launch {
            loadTierListsInternal()
        }
    }

    private suspend fun loadTierListsInternal() = loadMutex.withLock {
        val hasVisibleList = _state.value is TierListsUiState.Success

        if (!hasVisibleList) {
            _state.value = TierListsUiState.Loading
        }

        try {
            lastLoadedLists = repository.getAllTierLists()
            emitSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Loading tier lists failed")
            if (!hasVisibleList) {
                _state.value = TierListsUiState.Error
            }
        }
    }

    private fun emitSuccess() {
        val query = (mode as? HomeMode.Searching)?.query
        val filtered = if (query != null) {
            lastLoadedLists.filter { it.title.contains(query, ignoreCase = true) }
        } else {
            lastLoadedLists
        }
        val rankedCount = lastLoadedLists.sumOf { list ->
            list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
        }
        _state.value = TierListsUiState.Success(
            lists = filtered,
            totalListCount = lastLoadedLists.size,
            rankedCount = rankedCount,
            mode = mode,
            tab = tab,
            community = communityFeed,
            communityCategory = communityCategory,
        )
    }

    fun selectTab(selected: HomeTab) {
        if (tab == selected) return
        tab = selected
        emitSuccess()
        if (selected == HomeTab.Community) loadCommunityFeed()
    }

    /**
     * Searching the community is a request, not a filter over what is already
     * on screen, so it waits for the typing to settle first.
     */
    private fun searchCommunity(query: String) {
        communitySearch?.cancel()
        communityFeed = CommunityFeed.Loading
        emitSuccess()
        communitySearch = viewModelScope.launch {
            delay(COMMUNITY_SEARCH_DELAY_MILLIS)
            loadCommunityFeedNow(query)
        }
    }

    fun selectCommunityCategory(category: ListCategory?) {
        if (communityCategory == category) return
        communityCategory = category
        communityFeed = CommunityFeed.Loading
        emitSuccess()
        loadCommunityFeed()
    }

    fun loadCommunityFeed() {
        communitySearch?.cancel()
        viewModelScope.launch { loadCommunityFeedNow((mode as? HomeMode.Searching)?.query) }
    }

    private suspend fun loadCommunityFeedNow(query: String?) {
        communityFeed = community.feed(communityCategory, query).fold(
                onSuccess = { CommunityFeed.Ready(it) },
                onFailure = { error ->
                    Timber.w(error, "Loading the community feed failed")
                    CommunityFeed.Failed
                },
        )
        emitSuccess()
    }

    private fun setMode(newMode: HomeMode) {
        mode = newMode
        if (_state.value is TierListsUiState.Success) {
            emitSuccess()
        }
    }

    fun enterSearch() = setMode(HomeMode.Searching(""))

    fun updateSearchQuery(query: String) {
        setMode(HomeMode.Searching(query))
        if (tab == HomeTab.Community) searchCommunity(query)
    }

    fun exitSearch() = setMode(HomeMode.Browsing)

    fun enterSelection(id: Long) = setMode(HomeMode.Selecting(setOf(id)))

    fun toggleSelection(id: Long) {
        val current = mode as? HomeMode.Selecting ?: return
        val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
        setMode(if (updated.isEmpty()) HomeMode.Browsing else HomeMode.Selecting(updated))
    }

    fun exitSelection() = setMode(HomeMode.Browsing)

    /**
     * A published snapshot that outlives the list it came from is one its owner
     * believes they deleted, so the community copy comes down first. If it
     * cannot, the list stays where it is: a delete that leaves the thing public
     * is worse than a delete that did not happen.
     */
    fun deleteTierLists(ids: List<Long>) {
        setMode(HomeMode.Browsing)
        viewModelScope.launch {
            val stillPublic = takeDownPublished(ids)
            val deletable = ids - stillPublic
            if (deletable.isNotEmpty()) {
                messages.guard("Deleting lists") { repository.deleteTierLists(deletable) }
            }
            if (stillPublic.isNotEmpty()) {
                messages.send(UserMessage.PublishedListStillPublic)
            }
            loadTierListsInternal()
        }
    }

    /** Answers with the ids that could not be taken out of the community. */
    private suspend fun takeDownPublished(ids: List<Long>): Set<Long> {
        val published = lastLoadedLists.filter { it.id in ids && it.publishedId != null }
        return published.mapNotNull { list ->
            val publishedId = list.publishedId ?: return@mapNotNull null
            community.unpublish(publishedId).fold(
                onSuccess = {
                    repository.setPublishedId(list.id, null)
                    null
                },
                onFailure = { error ->
                    Timber.w(error, "Could not take a deleted list out of the community")
                    list.id
                },
            )
        }.toSet()
    }

    fun restoreTierLists(ids: List<Long>) {
        mutate("Restoring lists") { repository.restoreTierLists(ids) }
    }

    fun createTierList(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            var createdId: Long? = null
            messages.guard("Creating a list") { createdId = repository.createTierList(title) }
            loadTierListsInternal()
            createdId?.let(onCreated)
        }
    }

    private fun mutate(operation: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            messages.guard(operation) { block() }
            loadTierListsInternal()
        }
    }
}
