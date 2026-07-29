package com.artiuillab.tieryourlife.feature.tier.presentation.state

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

sealed interface MovieSearchUiState {

    data object Initial : MovieSearchUiState

    data object Loading : MovieSearchUiState

    data class Empty(
        val query: String,
    ) : MovieSearchUiState

    data class Success(
        val items: List<TierItem>,
    ) : MovieSearchUiState

    data class Error(
        val message: String,
    ) : MovieSearchUiState
}