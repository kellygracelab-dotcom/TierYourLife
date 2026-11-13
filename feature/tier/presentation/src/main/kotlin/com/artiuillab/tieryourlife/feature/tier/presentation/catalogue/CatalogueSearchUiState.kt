package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture

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

/** The field holds an address: no search, the picture behind it instead. [answer] is null while the server looks. */
data class LinkUiState(val url: String, val answer: LinkPicture? = null) {
    val host: String get() = url.substringAfter("://").substringBefore('/').substringBefore('?').removePrefix("www.")
}
