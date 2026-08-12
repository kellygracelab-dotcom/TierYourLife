package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import android.util.Log
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

private const val LOAD_LOG_TAG = "TierLists"

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    private var lastLoadedLists: List<TierList> = emptyList()

    private var mode: HomeMode = HomeMode.Browsing


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
            Log.w(LOAD_LOG_TAG, "Loading tier lists failed", e)
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
        )
    }

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

    fun toggleSelection(id: Long) {
        val current = mode as? HomeMode.Selecting ?: return
        val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
        setMode(if (updated.isEmpty()) HomeMode.Browsing else HomeMode.Selecting(updated))
    }

    fun exitSelection() = setMode(HomeMode.Browsing)

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

    fun createTierList(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createTierList(title)
            onCreated(id)
            loadTierListsInternal()
        }
    }
}
