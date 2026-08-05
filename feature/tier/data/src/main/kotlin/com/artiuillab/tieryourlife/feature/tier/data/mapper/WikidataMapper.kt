package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.commonsThumbnailUrl
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.qidFromEntityUri
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.search.WikidataCandidate

private const val WIKIDATA_ID_PREFIX = "wikidata:"

// What the details query knows about one item. Both fields are optional: plenty of items have
// no image at all, and only films carry a TMDB id.
internal data class WikidataItemDetails(
    val imageUrl: String? = null,
    val linkedTmdbId: Long? = null,
)

// The Query Service answers with one row per item, keyed by its full entity URI. An item with
// several images produces several rows; the first is kept, which is the same choice the old
// first-claim reading made.
internal fun WikidataSparqlResponseDto.toDetailsByQid(): Map<String, WikidataItemDetails> {
    val detailsByQid = mutableMapOf<String, WikidataItemDetails>()

    results.bindings.forEach { binding ->
        val qid = binding.item?.value?.let { qidFromEntityUri(it) } ?: return@forEach
        val existing = detailsByQid[qid]

        detailsByQid[qid] = WikidataItemDetails(
            imageUrl = existing?.imageUrl
                ?: binding.image?.value?.takeIf { it.isNotEmpty() }?.let { commonsThumbnailUrl(it) },
            linkedTmdbId = existing?.linkedTmdbId ?: binding.tmdb?.value?.toLongOrNull(),
        )
    }

    return detailsByQid
}

// details is null when the query returned no row for this item, which also covers the details
// call having failed outright. A result with no picture is still a usable result, so everything
// coming from it degrades to null rather than throwing.
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
