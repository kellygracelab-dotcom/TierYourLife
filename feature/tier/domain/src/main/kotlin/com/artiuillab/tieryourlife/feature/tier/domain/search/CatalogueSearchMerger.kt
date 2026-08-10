package com.artiuillab.tieryourlife.feature.tier.domain.search

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

data class WikidataCandidate(
    val item: CatalogueItem,
    val linkedTmdbId: Long?,
)

private const val TMDB_ID_PREFIX = "tmdb:"

object CatalogueSearchMerger {

    fun merge(
        query: String,
        tmdbResult: Result<List<CatalogueItem>>,
        wikidataResult: Result<List<WikidataCandidate>>,
    ): Result<List<CatalogueItem>> {
        if (tmdbResult.isFailure && wikidataResult.isFailure) {
            return Result.failure(
                tmdbResult.exceptionOrNull() ?: wikidataResult.exceptionOrNull()!!,
            )
        }

        val tmdbItems = tmdbResult.getOrDefault(emptyList()).filter { it.hasImage() }
        val wikidataCandidates = wikidataResult.getOrDefault(emptyList())
            .filter { it.item.hasImage() }

        val presentTmdbIds = tmdbItems.mapNotNull { it.tmdbNumericId() }.toSet()

        val wikidataItems = wikidataCandidates
            .filter { it.linkedTmdbId == null || it.linkedTmdbId !in presentTmdbIds }
            .map { it.item }

        val interleaved = interleave(tmdbItems, wikidataItems)

        val trimmedQuery = query.trim()
        return Result.success(interleaved.sortedBy { score(it.title, trimmedQuery) })
    }

    private fun CatalogueItem.hasImage(): Boolean = !imageUrl.isNullOrBlank()

    private fun CatalogueItem.tmdbNumericId(): Long? =
        if (id.startsWith(TMDB_ID_PREFIX)) id.removePrefix(TMDB_ID_PREFIX).toLongOrNull() else null

    private fun interleave(first: List<CatalogueItem>, second: List<CatalogueItem>): List<CatalogueItem> {
        val result = ArrayList<CatalogueItem>(first.size + second.size)
        val maxSize = maxOf(first.size, second.size)
        for (index in 0 until maxSize) {
            first.getOrNull(index)?.let(result::add)
            second.getOrNull(index)?.let(result::add)
        }
        return result
    }

    private fun score(title: String, trimmedQuery: String): Int = when {
        trimmedQuery.isNotEmpty() && title.equals(trimmedQuery, ignoreCase = true) -> 0
        trimmedQuery.isNotEmpty() && title.startsWith(trimmedQuery, ignoreCase = true) -> 1
        else -> 2
    }
}
