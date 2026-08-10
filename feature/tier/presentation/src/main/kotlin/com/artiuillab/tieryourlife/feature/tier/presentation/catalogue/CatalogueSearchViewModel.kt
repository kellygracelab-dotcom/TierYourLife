package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
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

@HiltViewModel
class CatalogueSearchViewModel @Inject constructor(
    private val repository: CatalogueSearchRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state =
        MutableStateFlow<CatalogueSearchUiState>(CatalogueSearchUiState.Initial)

    val state: StateFlow<CatalogueSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        searchJob?.cancel()
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
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = CatalogueSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch { performSearch(trimmed) }
    }

    private suspend fun performSearch(trimmedQuery: String) {
        _state.value = CatalogueSearchUiState.Loading

        repository.search(trimmedQuery, appPreferences.languageTag())
            .fold(
                onSuccess = { items ->
                    _state.value = if (items.isEmpty()) {
                        CatalogueSearchUiState.Empty(
                            query = trimmedQuery,
                        )
                    } else {
                        CatalogueSearchUiState.Success(
                            items = items,
                        )
                    }
                },
                onFailure = { _state.value = CatalogueSearchUiState.Error },
            )
    }
}
