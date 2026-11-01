package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice

sealed interface CatalogueSearchUiState {

    data object Initial : CatalogueSearchUiState

    data object Loading : CatalogueSearchUiState

    data class Empty(
        val query: String,
        /** Nothing even with every catalogue and the web: no way further to offer. */
        val searchedEverywhere: Boolean = false,
        val notice: SearchNotice? = null,
    ) : CatalogueSearchUiState

    data class Success(
        val items: List<CatalogueItem>,
        val canLoadMore: Boolean = false,
        val loadingMore: Boolean = false,
        val notice: SearchNotice? = null,
    ) : CatalogueSearchUiState

    data object Error : CatalogueSearchUiState
}
