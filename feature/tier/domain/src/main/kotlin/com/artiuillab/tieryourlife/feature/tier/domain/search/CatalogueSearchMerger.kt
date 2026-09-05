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
        val sources = listOf(tmdbResult, wikidataResult)
        if (sources.all { it.isFailure }) {
            return Result.failure(sources.firstNotNullOf { it.exceptionOrNull() })
        }

        val tmdbItems = tmdbResult.getOrDefault(emptyList())
        val wikidataCandidates = wikidataResult.getOrDefault(emptyList())

        val presentTmdbIds = tmdbItems.mapNotNull { it.tmdbNumericId() }.toSet()

        val wikidataItems = wikidataCandidates
            .filter { it.linkedTmdbId == null || it.linkedTmdbId !in presentTmdbIds }
            .map { it.item }

        return Result.success(rank(query, interleave(tmdbItems, wikidataItems)))
    }

    /**
     * Best match first; a later page is ranked on its own and appended, so
     * rows under the reader's finger keep their order. A picture is the second
     * question: games, books and records have no free cover anywhere, so
     * filtering to the illustrated drops them entirely. The picture breaks ties.
     */
    fun rank(query: String, items: List<CatalogueItem>): List<CatalogueItem> {
        val trimmedQuery = query.trim()
        return items.sortedWith(
            compareBy({ score(it.title, trimmedQuery) }, { if (it.hasImage()) 0 else 1 }),
        )
    }

    private fun CatalogueItem.hasImage(): Boolean = !imageUrl.isNullOrBlank()

    private fun CatalogueItem.tmdbNumericId(): Long? =
        if (id.startsWith(TMDB_ID_PREFIX)) id.removePrefix(TMDB_ID_PREFIX).toLongOrNull() else null

    /** One from each catalogue in turn, so stable ranking has no reason to prefer whichever was asked first. */
    private fun interleave(vararg lists: List<CatalogueItem>): List<CatalogueItem> {
        val result = ArrayList<CatalogueItem>(lists.sumOf { it.size })
        val longest = lists.maxOfOrNull { it.size } ?: 0
        for (index in 0 until longest) {
            lists.forEach { list -> list.getOrNull(index)?.let(result::add) }
        }
        return result
    }

    private fun score(title: String, trimmedQuery: String): Int = when {
        trimmedQuery.isNotEmpty() && title.equals(trimmedQuery, ignoreCase = true) -> 0
        trimmedQuery.isNotEmpty() && title.startsWith(trimmedQuery, ignoreCase = true) -> 1
        else -> 2
    }
}
