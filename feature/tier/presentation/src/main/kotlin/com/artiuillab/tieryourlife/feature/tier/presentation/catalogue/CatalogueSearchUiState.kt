package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

sealed interface CatalogueSearchUiState {

    data object Initial : CatalogueSearchUiState

    data object Loading : CatalogueSearchUiState

    data class Empty(
        val query: String,
    ) : CatalogueSearchUiState

    data class Success(
        val items: List<CatalogueItem>,
        val canLoadMore: Boolean = false,
        val loadingMore: Boolean = false,
    ) : CatalogueSearchUiState

    data object Error : CatalogueSearchUiState
}
