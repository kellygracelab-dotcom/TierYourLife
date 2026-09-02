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

        val tmdbItems = tmdbResult.getOrDefault(emptyList())
        val wikidataCandidates = wikidataResult.getOrDefault(emptyList())

        val presentTmdbIds = tmdbItems.mapNotNull { it.tmdbNumericId() }.toSet()

        val wikidataItems = wikidataCandidates
            .filter { it.linkedTmdbId == null || it.linkedTmdbId !in presentTmdbIds }
            .map { it.item }

        return Result.success(rank(query, interleave(tmdbItems, wikidataItems)))
    }

    /**
     * Which of these are worth showing, best match first. A later page is
     * ranked on its own and appended, so the rows already under the reader's
     * finger keep their order and their ticks.
     *
     * A picture is the second question, not the first. Whole subjects have no
     * free picture anywhere -- a game's cover, a book's jacket and an album's
     * sleeve are all somebody's property, and Commons will not hold them --
     * so a catalogue filtered down to what is illustrated is a catalogue with
     * games, books and records missing from it entirely. Sorting by the match
     * first keeps what somebody actually typed at the top, and letting the
     * picture break the tie keeps the shelf looking like a shelf.
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
