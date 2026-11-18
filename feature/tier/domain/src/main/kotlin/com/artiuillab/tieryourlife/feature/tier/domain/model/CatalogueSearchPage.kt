package com.artiuillab.tieryourlife.feature.tier.domain.model

data class CatalogueSearchPage(
    val items: List<CatalogueItem>,
    val hasMore: Boolean,
    val notice: SearchNotice? = null,
    /** A quick answer's: the whole search, slower, can still add to it. */
    val rest: Boolean = false,
    /** A catalogue this search needed was busy: what it would have found is missing, and can be asked for at [retryAt]. */
    val busy: Boolean = false,
    /** With [busy]: when the busy catalogues may be asked again, epoch ms; null when the server did not say. */
    val retryAt: Long? = null,
)
