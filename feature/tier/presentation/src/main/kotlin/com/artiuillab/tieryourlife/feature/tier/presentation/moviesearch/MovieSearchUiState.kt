package com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

sealed interface MovieSearchUiState {

    data object Initial : MovieSearchUiState

    data object Loading : MovieSearchUiState

    data class Empty(
        val query: String,
    ) : MovieSearchUiState

    data class Success(
        val items: List<CatalogueItem>,
    ) : MovieSearchUiState

    data class Error(
        val message: String,
    ) : MovieSearchUiState
}
