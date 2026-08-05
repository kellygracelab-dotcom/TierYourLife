package com.artiuillab.tieryourlife.feature.tier.domain.model

// A result from the remote catalogue, identified by that catalogue's id — distinct from
// TierItem,
// which is a persisted list entry keyed by the local Room row id.
data class MovieSearchResult(
    val tmdbId: Long,
    val title: String,
    val imageUrl: String?,
    val year: Int?,
)
