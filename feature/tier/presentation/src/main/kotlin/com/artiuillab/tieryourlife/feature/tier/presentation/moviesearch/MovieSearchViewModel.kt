package com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.MovieSearchRepository
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
class MovieSearchViewModel @Inject constructor(
    private val repository: MovieSearchRepository,
) : ViewModel() {

    private val _state =
        MutableStateFlow<MovieSearchUiState>(MovieSearchUiState.Initial)

    val state: StateFlow<MovieSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    // Called on every keystroke: debounced, so typing doesn't fire a request per
    // character. Below MIN_QUERY_LENGTH there is nothing to debounce — the sheet
    // just shows its own "type more" hint.
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = MovieSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            performSearch(trimmed)
        }
    }

    // Called by the keyboard's search action: runs immediately, bypassing the
    // debounce, even when the query text hasn't changed since the last search.
    fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = MovieSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch { performSearch(trimmed) }
    }

    private suspend fun performSearch(trimmedQuery: String) {
        _state.value = MovieSearchUiState.Loading

        repository.searchMovies(trimmedQuery)
            .fold(
                onSuccess = { items ->
                    _state.value = if (items.isEmpty()) {
                        MovieSearchUiState.Empty(
                            query = trimmedQuery,
                        )
                    } else {
                        MovieSearchUiState.Success(
                            items = items,
                        )
                    }
                },
                onFailure = { exception ->
                    _state.value = MovieSearchUiState.Error(
                        message = exception.message
                            ?: "Search failed",
                    )
                },
            )
    }
}
