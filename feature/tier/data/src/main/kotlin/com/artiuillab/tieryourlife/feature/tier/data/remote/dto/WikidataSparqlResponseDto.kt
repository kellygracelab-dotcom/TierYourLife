package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WikidataSparqlResponseDto(
    val results: WikidataSparqlResultsDto = WikidataSparqlResultsDto(),
)

@Serializable
data class WikidataSparqlResultsDto(
    val bindings: List<WikidataSparqlBindingDto> = emptyList(),
)

@Serializable
data class WikidataSparqlBindingDto(
    val item: WikidataSparqlValueDto? = null,
    val image: WikidataSparqlValueDto? = null,
    val logo: WikidataSparqlValueDto? = null,
    val tmdb: WikidataSparqlValueDto? = null,
)

@Serializable
data class WikidataSparqlValueDto(
    val value: String = "",
)
