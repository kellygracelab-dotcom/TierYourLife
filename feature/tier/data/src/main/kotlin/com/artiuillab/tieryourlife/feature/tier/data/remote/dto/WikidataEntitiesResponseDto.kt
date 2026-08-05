package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

// action=wbgetentities&props=claims response shape:
// {"entities":{"Q11788":{"claims":{"P18":[{"mainsnak":{"datavalue":{"value":"Foo.jpg"}}}]}}}}
//
// Both properties this app reads off a claim — P18 (Commons image filename) and P4947 (TMDB
// movie id) — are Wikibase "commonsMedia"/"external-id" datatypes, whose datavalue is always a
// plain JSON string, so a single shape covers both.
@Serializable
data class WikidataEntitiesResponseDto(
    val entities: Map<String, WikidataEntityDto> = emptyMap(),
)

@Serializable
data class WikidataEntityDto(
    val claims: Map<String, List<WikidataClaimDto>> = emptyMap(),
)

@Serializable
data class WikidataClaimDto(
    val mainsnak: WikidataMainSnakDto? = null,
)

@Serializable
data class WikidataMainSnakDto(
    val datavalue: WikidataDataValueDto? = null,
)

@Serializable
data class WikidataDataValueDto(
    val value: String? = null,
)

// The first claim's string value for a property, or null if the entity has no claim for it at
// all — a missing image (no P18) must resolve to null here, not throw.
internal fun WikidataEntityDto.firstClaimValue(property: String): String? =
    claims[property]?.firstOrNull()?.mainsnak?.datavalue?.value
