package com.artiuillab.tieryourlife.feature.tier.domain.search

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

// Wraps a Wikidata search hit with the linked TMDB movie id read from its P4947 claim, purely
// so the merge below can drop it when TMDB already surfaced the same film. This wrapper is
// merge-input plumbing only — CatalogueItem itself stays source-agnostic with no TMDB-specific
// field, and never leaves this function as this type.
data class WikidataCandidate(
    val item: CatalogueItem,
    val linkedTmdbId: Long?,
)

private const val TMDB_ID_PREFIX = "tmdb:"

// The fan-out itself (parallel fetch, per-source timeout) is impure and lives in the
// repository, which is the only layer allowed to touch coroutines/IO. Everything about what
// to do with the two outcomes — whether the combination is a failure, how to dedupe, how to
// rank — is a decision, not I/O, so it lives here as a pure function of two Results and a
// query. That's what makes it testable with plain JVM unit tests, the only test tier this
// project's CI actually runs.
object CatalogueSearchMerger {

    fun merge(
        query: String,
        tmdbResult: Result<List<CatalogueItem>>,
        wikidataResult: Result<List<WikidataCandidate>>,
    ): Result<List<CatalogueItem>> {
        if (tmdbResult.isFailure && wikidataResult.isFailure) {
            // One source succeeding — even with zero results — is a success; this branch is
            // only reachable when both sources failed or timed out.
            return Result.failure(
                tmdbResult.exceptionOrNull() ?: wikidataResult.exceptionOrNull()!!,
            )
        }

        val tmdbItems = tmdbResult.getOrDefault(emptyList())
        val wikidataCandidates = wikidataResult.getOrDefault(emptyList())

        val presentTmdbIds = tmdbItems.mapNotNull { it.tmdbNumericId() }.toSet()

        // Same film, one row: drop a Wikidata item whose linked TMDB id is already present
        // from TMDB. A Wikidata item with no P4947 claim at all (linkedTmdbId == null) has
        // nothing to match against, so it always survives this filter.
        val wikidataItems = wikidataCandidates
            .filter { it.linkedTmdbId == null || it.linkedTmdbId !in presentTmdbIds }
            .map { it.item }

        // Round-robin first so plain concatenation can't let one source own the whole top of
        // the list, then a *stable* sort by score on top of that — ties (most results, in
        // practice) keep the round-robin order, so neither source's items get all pushed
        // above or below the other's within a score tier.
        val interleaved = interleave(tmdbItems, wikidataItems)

        val trimmedQuery = query.trim()
        return Result.success(interleaved.sortedBy { score(it.title, trimmedQuery) })
    }

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

    // Lower is better: exact match first, then a title that starts with the query, then
    // everything else. Case-insensitive, since the ranking is about matching what the user
    // typed, not a literal-string contest.
    private fun score(title: String, trimmedQuery: String): Int = when {
        trimmedQuery.isNotEmpty() && title.equals(trimmedQuery, ignoreCase = true) -> 0
        trimmedQuery.isNotEmpty() && title.startsWith(trimmedQuery, ignoreCase = true) -> 1
        else -> 2
    }
}
