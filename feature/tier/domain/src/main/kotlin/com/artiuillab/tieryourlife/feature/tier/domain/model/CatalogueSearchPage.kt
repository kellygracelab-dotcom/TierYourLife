package com.artiuillab.tieryourlife.feature.tier.domain.model

data class CatalogueSearchPage(
    val items: List<CatalogueItem>,
    val hasMore: Boolean,
)
