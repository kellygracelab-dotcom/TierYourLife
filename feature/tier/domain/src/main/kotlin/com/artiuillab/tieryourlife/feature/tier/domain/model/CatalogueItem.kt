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
    /** Who the picture's license asks to be named, such as "© Jane Doe, CC BY". */
    val credit: String? = null,
    val attribution: PictureAttribution? = null,
    /** What the picture is of, by the server's word (movie, anime, book…); null when it says nothing worth a label. */
    val kind: String? = null,
    /** Whose catalogue it came from, by the server's word (tmdb, anilist, web…); null when unknown. */
    val source: String? = null,
)
