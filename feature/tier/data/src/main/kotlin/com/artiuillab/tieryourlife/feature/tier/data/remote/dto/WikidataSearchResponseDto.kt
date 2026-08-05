package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

// action=wbsearchentities response shape:
// {"search":[{"id":"Q11788","label":"...","description":"...", ...}]}
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
