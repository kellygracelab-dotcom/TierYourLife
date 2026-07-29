package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MovieSearchResponseDto(
    val page: Int,
    val results: List<MovieDto>,
)
