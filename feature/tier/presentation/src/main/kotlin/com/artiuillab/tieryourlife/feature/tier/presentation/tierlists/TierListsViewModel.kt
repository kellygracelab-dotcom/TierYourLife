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
            _state.value = TierListsUiState.Success(repository.loadTierListsForPresentation())
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

    fun createTierList(title: String) {
        viewModelScope.launch {
            repository.createTierList(title)
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
