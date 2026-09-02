package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Games, already reduced to what a card needs.
 *
 * Unlike TMDB, this is not the catalogue's own shape: IGDB speaks a query
 * language of its own and the proxy asks the question, so what arrives here
 * is already the answer rather than a page to be interpreted.
 */
@Serializable
data class GameSearchResponseDto(
    val results: List<GameDto> = emptyList(),
)

@Serializable
data class GameDto(
    val id: Long,
    val name: String,
    /** Absent for something announced but not dated. */
    val year: Int? = null,
    /** Absent when even IGDB has no cover, which happens. */
    val imageUrl: String? = null,
)
