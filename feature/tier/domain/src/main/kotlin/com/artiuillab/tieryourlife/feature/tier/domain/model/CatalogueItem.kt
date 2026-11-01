package com.artiuillab.tieryourlife.feature.tier.domain.model

data class CatalogueItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    /** A smaller copy for a grid of results; null when only [imageUrl] exists. */
    val thumbUrl: String? = null,
    /** Found by the web search: someone else's site, so a card keeps its own copy rather than the address. */
    val fromWeb: Boolean = false,
)
