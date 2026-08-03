package com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.MovieSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieSearchViewModel @Inject constructor(
    private val repository: MovieSearchRepository,
) : ViewModel() {

    private val _state =
        MutableStateFlow<MovieSearchUiState>(MovieSearchUiState.Initial)

    val state: StateFlow<MovieSearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) {
            _state.value = MovieSearchUiState.Initial
            return
        }

        _state.value = MovieSearchUiState.Loading

        viewModelScope.launch {
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
}
