package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.commonsThumbnailUrl
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.qidFromEntityUri
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.search.WikidataCandidate

private const val WIKIDATA_ID_PREFIX = "wikidata:"

internal data class WikidataItemDetails(
    val imageUrl: String? = null,
    val linkedTmdbId: Long? = null,
)

internal fun WikidataSparqlResponseDto.toDetailsByQid(): Map<String, WikidataItemDetails> {
    val detailsByQid = mutableMapOf<String, WikidataItemDetails>()

    results.bindings.forEach { binding ->
        val qid = binding.item?.value?.let { qidFromEntityUri(it) } ?: return@forEach
        val existing = detailsByQid[qid]

        // The photograph first and the logo only after it: a company may have
        // both, and a picture of the building is the better card.
        val picture = binding.image?.value ?: binding.logo?.value

        detailsByQid[qid] = WikidataItemDetails(
            imageUrl = existing?.imageUrl
                ?: picture?.takeIf { it.isNotEmpty() }?.let { commonsThumbnailUrl(it) },
            linkedTmdbId = existing?.linkedTmdbId ?: binding.tmdb?.value?.toLongOrNull(),
        )
    }

    return detailsByQid
}

internal fun WikidataSearchItemDto.toDomain(details: WikidataItemDetails?): WikidataCandidate =
    WikidataCandidate(
        item = CatalogueItem(
            id = "$WIKIDATA_ID_PREFIX$id",
            title = label ?: id,
            subtitle = description,
            imageUrl = details?.imageUrl,
        ),
        linkedTmdbId = details?.linkedTmdbId,
    )
