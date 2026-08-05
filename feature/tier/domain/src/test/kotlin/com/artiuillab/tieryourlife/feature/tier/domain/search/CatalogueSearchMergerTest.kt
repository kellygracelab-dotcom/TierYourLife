package com.artiuillab.tieryourlife.feature.tier.domain.search

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CatalogueSearchMergerTest {

    private fun tmdb(id: Long, title: String) =
        CatalogueItem(id = "tmdb:$id", title = title, subtitle = null, imageUrl = null)

    private fun wikidata(qid: String, title: String, linkedTmdbId: Long? = null) =
        WikidataCandidate(
            item = CatalogueItem(id = "wikidata:$qid", title = title, subtitle = null, imageUrl = null),
            linkedTmdbId = linkedTmdbId,
        )

    @Test
    fun `drops a wikidata item whose P4947 id matches a tmdb item already present`() {
        val tmdbResult = Result.success(listOf(tmdb(157336, "Interstellar")))
        val wikidataResult = Result.success(listOf(wikidata("Q206529", "Interstellar", linkedTmdbId = 157336)))

        val result = CatalogueSearchMerger.merge("interstellar", tmdbResult, wikidataResult)

        assertEquals(listOf("tmdb:157336"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `keeps a wikidata item whose linked tmdb id does not match anything from tmdb`() {
        val tmdbResult = Result.success(listOf(tmdb(1, "Something Else")))
        val wikidataResult = Result.success(listOf(wikidata("Q1", "Bears", linkedTmdbId = 999)))

        val result = CatalogueSearchMerger.merge("bears", tmdbResult, wikidataResult)

        assertEquals(setOf("tmdb:1", "wikidata:Q1"), result.getOrThrow().map { it.id }.toSet())
    }

    @Test
    fun `keeps a wikidata item with no P4947 claim at all`() {
        val tmdbResult = Result.success(listOf(tmdb(1, "Unrelated")))
        val wikidataResult = Result.success(listOf(wikidata("Q1", "Bears", linkedTmdbId = null)))

        val result = CatalogueSearchMerger.merge("bears", tmdbResult, wikidataResult)

        assertEquals(setOf("tmdb:1", "wikidata:Q1"), result.getOrThrow().map { it.id }.toSet())
    }

    @Test
    fun `exact title match ranks above a prefix match which ranks above everything else`() {
        val tmdbResult = Result.success(
            listOf(
                tmdb(1, "Dune Part Two"),
                tmdb(2, "Something Unrelated"),
                tmdb(3, "Dune"),
            ),
        )

        val result = CatalogueSearchMerger.merge("dune", tmdbResult, Result.success(emptyList()))

        assertEquals(listOf("tmdb:3", "tmdb:1", "tmdb:2"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `ranking is case insensitive`() {
        val tmdbResult = Result.success(listOf(tmdb(1, "Not It"), tmdb(2, "DUNE")))

        val result = CatalogueSearchMerger.merge("dune", tmdbResult, Result.success(emptyList()))

        assertEquals("tmdb:2", result.getOrThrow().first().id)
    }

    @Test
    fun `round-robin interleaves the two sources before ranking so ties stay a fair mix`() {
        // Every item ties at the same score (none of them match the query), so the final
        // order is purely the round-robin interleave preserved by the stable sort.
        val tmdbResult = Result.success(listOf(tmdb(1, "T1"), tmdb(2, "T2"), tmdb(3, "T3")))
        val wikidataResult = Result.success(listOf(wikidata("Q1", "W1"), wikidata("Q2", "W2")))

        val result = CatalogueSearchMerger.merge("nomatch", tmdbResult, wikidataResult)

        assertEquals(
            listOf("tmdb:1", "wikidata:Q1", "tmdb:2", "wikidata:Q2", "tmdb:3"),
            result.getOrThrow().map { it.id },
        )
    }

    @Test
    fun `an exact match from wikidata still outranks a non-matching tmdb item`() {
        val tmdbResult = Result.success(listOf(tmdb(1, "Something Unrelated")))
        val wikidataResult = Result.success(listOf(wikidata("Q1", "Bears")))

        val result = CatalogueSearchMerger.merge("bears", tmdbResult, wikidataResult)

        assertEquals("wikidata:Q1", result.getOrThrow().first().id)
    }

    @Test
    fun `one source empty still returns the other source's items as a success`() {
        val tmdbResult = Result.success(listOf(tmdb(1, "Solo")))

        val result = CatalogueSearchMerger.merge("solo", tmdbResult, Result.success(emptyList()))

        assertTrue(result.isSuccess)
        assertEquals(listOf("tmdb:1"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `one source failed is still a success carrying the other source's items`() {
        val tmdbResult = Result.success(listOf(tmdb(1, "Solo")))
        val wikidataResult = Result.failure<List<WikidataCandidate>>(IOException("wikidata down"))

        val result = CatalogueSearchMerger.merge("solo", tmdbResult, wikidataResult)

        assertTrue(result.isSuccess)
        assertEquals(listOf("tmdb:1"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `one source failed with zero results from the other is still a success`() {
        val tmdbResult = Result.success(emptyList<CatalogueItem>())
        val wikidataResult = Result.failure<List<WikidataCandidate>>(IOException("wikidata down"))

        val result = CatalogueSearchMerger.merge("nothing", tmdbResult, wikidataResult)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `both sources failed is a failure`() {
        val tmdbFailure = IOException("tmdb down")
        val tmdbResult = Result.failure<List<CatalogueItem>>(tmdbFailure)
        val wikidataResult = Result.failure<List<WikidataCandidate>>(IOException("wikidata down"))

        val result = CatalogueSearchMerger.merge("anything", tmdbResult, wikidataResult)

        assertTrue(result.isFailure)
        assertEquals(tmdbFailure, result.exceptionOrNull())
    }

    @Test
    fun `both sources empty yields an empty success list, not a crash`() {
        val result = CatalogueSearchMerger.merge(
            "nothing",
            Result.success(emptyList()),
            Result.success(emptyList()),
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `blank query never claims an exact or prefix match`() {
        val tmdbResult = Result.success(listOf(tmdb(1, ""), tmdb(2, "Anything")))

        val result = CatalogueSearchMerger.merge("   ", tmdbResult, Result.success(emptyList()))

        // Neither item should be treated as an exact/prefix hit against a blank query — the
        // round-robin order (single source here, so just insertion order) is preserved.
        assertEquals(listOf("tmdb:1", "tmdb:2"), result.getOrThrow().map { it.id })
    }
}
