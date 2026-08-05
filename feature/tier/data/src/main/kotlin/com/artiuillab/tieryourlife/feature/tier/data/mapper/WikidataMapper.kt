package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.commonsFilePathUrl
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataEntityDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.firstClaimValue
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.search.WikidataCandidate

private const val WIKIDATA_ID_PREFIX = "wikidata:"
private const val PROPERTY_IMAGE = "P18"
private const val PROPERTY_TMDB_MOVIE_ID = "P4947"

// entity is null when the claims call returned nothing for this item's id (e.g. the item
// wasn't in the batch, or the batch call itself failed upstream of this mapper) — every field
// that comes from claims degrades to null rather than throwing.
internal fun WikidataSearchItemDto.toDomain(entity: WikidataEntityDto?): WikidataCandidate {
    val imageFilename = entity?.firstClaimValue(PROPERTY_IMAGE)
    val linkedTmdbId = entity?.firstClaimValue(PROPERTY_TMDB_MOVIE_ID)?.toLongOrNull()

    return WikidataCandidate(
        item = CatalogueItem(
            id = "$WIKIDATA_ID_PREFIX$id",
            title = label ?: id,
            subtitle = description,
            imageUrl = imageFilename?.let { commonsFilePathUrl(it) },
        ),
        linkedTmdbId = linkedTmdbId,
    )
}
