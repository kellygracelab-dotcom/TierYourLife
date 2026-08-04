package com.artiuillab.tieryourlife.feature.tier.domain.model

// A found movie from search, identified by its TMDB id — distinct from TierItem,
// which is a persisted list entry keyed by the local Room row id.
data class MovieSearchResult(
    val tmdbId: Long,
    val title: String,
    val imageUrl: String?,
    val year: Int?,
)
