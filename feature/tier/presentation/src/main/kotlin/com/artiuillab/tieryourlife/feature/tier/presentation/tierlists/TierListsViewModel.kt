package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    // Every list as last read from the repository, unfiltered — the single source the
    // search query is applied against. Lives here, not in the composable: "Filtering is
    // done in the view model, not the composable" (docs/design-spec-home.md, section 2).
    private var lastLoadedLists: List<TierList> = emptyList()

    // Lives here rather than in the composable so it survives a configuration change.
    // Search, selection and browsing are mutually exclusive by the HomeMode type itself
    // (see TierListsUiState.kt) — there is exactly one value here at any time.
    private var mode: HomeMode = HomeMode.Browsing

    // No init-time load here on purpose: the screen's own resume effect (see
    // OnResumeEffect in TierListsScreen.kt) is the single trigger for every load,
    // including the first one — its catch-up dispatch fires on initial mount too.
    // Loading here as well would mean the first appearance reads the repository
    // twice.

    fun loadTierLists() {
        viewModelScope.launch {
            loadTierListsInternal()
        }
    }

    private suspend fun loadTierListsInternal() = loadMutex.withLock {
        // Loading only replaces what's on screen when there's nothing there to protect
        // yet, i.e. the very first read. Every later read — triggered by returning to
        // this screen — leaves the current list showing right up until the new one is
        // ready, so a background refresh never blinks the screen empty.
        val hasVisibleList = _state.value is TierListsUiState.Success

        if (!hasVisibleList) {
            _state.value = TierListsUiState.Loading
        }

        try {
            lastLoadedLists = repository.loadTierListsForPresentation()
            emitSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed re-read must not erase a list that's already visible — that
            // would trade the blink this change fixes for something worse, an error
            // screen replacing real data over a transient failure. So a repeat read
            // that fails is swallowed and the stale list stays exactly as it was; only
            // the first read, which has nothing to protect, surfaces the failure.
            if (!hasVisibleList) {
                _state.value = TierListsUiState.Error(e.message ?: "Failed to load lists")
            }
        }
    }

    private fun emitSuccess() {
        val query = (mode as? HomeMode.Searching)?.query
        val filtered = if (query != null) {
            // Case-insensitive, matches anywhere in the title — "piz" finds "Pizza in
            // Lisbon" (docs/design-spec-home.md, section 2).
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
        )
    }

    // Mode changes are synchronous and never touch the repository, so they're applied
    // directly rather than through viewModelScope.launch — typing in the search field
    // must never wait on a coroutine dispatch. Only takes effect once a first load has
    // actually produced a Success: search/selection UI only exists once lists are on
    // screen, so a mode change before that can't happen from a real user action.
    private fun setMode(newMode: HomeMode) {
        mode = newMode
        if (_state.value is TierListsUiState.Success) {
            emitSuccess()
        }
    }

    fun enterSearch() = setMode(HomeMode.Searching(""))

    fun updateSearchQuery(query: String) = setMode(HomeMode.Searching(query))

    fun exitSearch() = setMode(HomeMode.Browsing)

    fun enterSelection(id: Long) = setMode(HomeMode.Selecting(setOf(id)))

    // Deselecting the last remaining card exits selection mode automatically
    // (docs/design-spec-home.md, section 3).
    fun toggleSelection(id: Long) {
        val current = mode as? HomeMode.Selecting ?: return
        val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
        setMode(if (updated.isEmpty()) HomeMode.Browsing else HomeMode.Selecting(updated))
    }

    fun exitSelection() = setMode(HomeMode.Browsing)

    // Deletes immediately and exits selection mode on the same tap — no confirmation.
    // Selection mode is cleared synchronously so the contextual bar disappears at once,
    // ahead of the (async) reload that actually removes the cards.
    fun deleteTierLists(ids: List<Long>) {
        setMode(HomeMode.Browsing)
        viewModelScope.launch {
            repository.deleteTierLists(ids)
            loadTierListsInternal()
        }
    }

    fun restoreTierLists(ids: List<Long>) {
        viewModelScope.launch {
            repository.restoreTierLists(ids)
            loadTierListsInternal()
        }
    }

    // The FAB's create-and-open path (docs/design-spec-home.md, section 4): the list is
    // created and saved immediately, and onCreated is invoked with its id as soon as
    // that finishes so the caller can navigate straight to it — the reload that follows
    // keeps Home's own list in sync for whenever the user comes back to it.
    fun createTierList(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createTierList("Untitled list")
            onCreated(id)
            loadTierListsInternal()
        }
    }
}

internal suspend fun TierRepository.loadTierListsForPresentation(): List<TierList> {
    var overviewLists = getAllTierLists()
    if (overviewLists.isEmpty()) {
        createTierList("Sci-fi films")
        createTierList("Every A24 film")
        overviewLists = getAllTierLists()
    }

    return overviewLists.map { overview ->
        getTierListById(overview.id) ?: overview
    }
}
