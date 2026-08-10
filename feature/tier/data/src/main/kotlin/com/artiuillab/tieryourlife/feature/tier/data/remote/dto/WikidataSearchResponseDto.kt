package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WikidataSearchResponseDto(
    val search: List<WikidataSearchItemDto> = emptyList(),
)

@Serializable
data class WikidataSearchItemDto(
    val id: String,
    val label: String? = null,
    val description: String? = null,
)
