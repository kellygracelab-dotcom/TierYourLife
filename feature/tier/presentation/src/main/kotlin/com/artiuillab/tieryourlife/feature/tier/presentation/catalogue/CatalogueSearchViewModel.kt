package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val MIN_QUERY_LENGTH = 2
private const val FIRST_PAGE = 1

@HiltViewModel
class CatalogueSearchViewModel @Inject constructor(
    private val repository: CatalogueSearchRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state =
        MutableStateFlow<CatalogueSearchUiState>(CatalogueSearchUiState.Initial)

    val state: StateFlow<CatalogueSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var moreJob: Job? = null
    private var searchedQuery = ""
    private var loadedPage = FIRST_PAGE

    fun onQueryChange(query: String) {
        cancelInFlight()
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = CatalogueSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            performSearch(trimmed)
        }
    }

    fun search(query: String) {
        cancelInFlight()
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = CatalogueSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch { performSearch(trimmed) }
    }

    fun loadMore() {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        if (!shown.canLoadMore || shown.loadingMore) return

        val nextPage = loadedPage + 1
        _state.value = shown.copy(loadingMore = true)
        moreJob = viewModelScope.launch {
            repository.search(searchedQuery, appPreferences.languageTag(), nextPage)
                .fold(
                    onSuccess = { page -> append(nextPage, page) },
                    // A page that never arrived is no reason to take away the
                    // ones that did. The next scroll asks again.
                    onFailure = { stopWaiting() },
                )
        }
    }

    private suspend fun performSearch(trimmedQuery: String) {
        _state.value = CatalogueSearchUiState.Loading
        searchedQuery = trimmedQuery
        loadedPage = FIRST_PAGE

        repository.search(trimmedQuery, appPreferences.languageTag(), FIRST_PAGE)
            .fold(
                onSuccess = { page ->
                    _state.value = if (page.items.isEmpty()) {
                        CatalogueSearchUiState.Empty(
                            query = trimmedQuery,
                        )
                    } else {
                        CatalogueSearchUiState.Success(
                            items = page.items,
                            canLoadMore = page.hasMore,
                        )
                    }
                },
                onFailure = { _state.value = CatalogueSearchUiState.Error },
            )
    }

    private fun append(page: Int, loaded: CatalogueSearchPage) {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        loadedPage = page
        val alreadyShown = shown.items.mapTo(mutableSetOf()) { it.id }
        _state.value = shown.copy(
            items = shown.items + loaded.items.filterNot { it.id in alreadyShown },
            canLoadMore = loaded.hasMore,
            loadingMore = false,
        )
    }

    private fun stopWaiting() {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        _state.value = shown.copy(loadingMore = false)
    }

    private fun cancelInFlight() {
        searchJob?.cancel()
        moreJob?.cancel()
    }
}
