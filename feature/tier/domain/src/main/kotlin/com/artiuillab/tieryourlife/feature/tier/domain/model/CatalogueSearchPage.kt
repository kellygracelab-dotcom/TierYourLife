package com.artiuillab.tieryourlife.feature.tier.domain.model

data class CatalogueSearchPage(
    val items: List<CatalogueItem>,
    val hasMore: Boolean,
    val notice: SearchNotice? = null,
    /** A quick answer's: the whole search, slower, can still add to it. */
    val rest: Boolean = false,
)
