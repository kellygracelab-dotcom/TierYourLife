package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

// The Wikidata Query Service answers in SPARQL 1.1 Results JSON:
//
// {"results":{"bindings":[
//   {"item":{"value":"http://www.wikidata.org/entity/Q11788"},
//    "image":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg"}}
// ]}}
//
// Every binding is optional per row: an item with no image simply has no "image" key, which is
// why each is nullable rather than defaulted to a blank string — absent and empty are different
// things and only one of them should produce a URL.
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
    val tmdb: WikidataSparqlValueDto? = null,
)

@Serializable
data class WikidataSparqlValueDto(
    val value: String = "",
)
