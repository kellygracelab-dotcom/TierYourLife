package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataSparqlApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueSearchRepositoryPagingTest {

    @Test
    fun aSearchAsksForTheFirstPage_andSaysThereIsMoreBehindIt() = runBlocking {
        val tmdb = FakeTmdbApi(totalPages = 7)
        val repository = repositoryWith(tmdb)

        val page = repository.search("marvel", languageTag = "en-GB", page = 1).getOrThrow()

        assertEquals(listOf(1), tmdb.requestedPages)
        assertTrue(page.hasMore)
        assertEquals(2, page.items.size)
    }

    @Test
    fun theNextPageIsAskedOfTheFilmsAlone_notOfWikidataAllOverAgain() = runBlocking {
        val tmdb = FakeTmdbApi(totalPages = 7)
        val wikidata = FakeWikidataApi()
        val repository = repositoryWith(tmdb, wikidata)

        repository.search("marvel", languageTag = "en-GB", page = 1).getOrThrow()
        assertEquals(1, wikidata.searches)

        val second = repository.search("marvel", languageTag = "en-GB", page = 2).getOrThrow()

        assertEquals(listOf(1, 2), tmdb.requestedPages)
        assertEquals(1, wikidata.searches)
        assertEquals(listOf("tmdb:200", "tmdb:201"), second.items.map { it.id })
    }

    @Test
    fun theLastPageSaysThereIsNothingBehindIt() = runBlocking {
        val tmdb = FakeTmdbApi(totalPages = 3)
        val repository = repositoryWith(tmdb)

        val page = repository.search("marvel", languageTag = "en-GB", page = 3).getOrThrow()

        assertFalse(page.hasMore)
    }

    // TMDB answers with an error past page five hundred however many it claims,
    // so the screen must never be told there is a five hundred and first.
    @Test
    fun theFiveHundredthPageIsTheEnd_howeverManyTmdbClaims() = runBlocking {
        val tmdb = FakeTmdbApi(totalPages = 20_000)
        val repository = repositoryWith(tmdb)

        assertTrue(repository.search("marvel", languageTag = "en-GB", page = 499).getOrThrow().hasMore)
        assertFalse(repository.search("marvel", languageTag = "en-GB", page = 500).getOrThrow().hasMore)
    }

    @Test
    fun aPageThatFailsIsReportedRatherThanComingBackEmpty() = runBlocking {
        val repository = repositoryWith(FailingTmdbApi())

        val result = repository.search("marvel", languageTag = "en-GB", page = 2)

        assertTrue(result.isFailure)
    }

    private fun repositoryWith(
        tmdb: TmdbApi,
        wikidata: FakeWikidataApi = FakeWikidataApi(),
    ) = CatalogueSearchRepositoryImpl(tmdb, wikidata, FakeWikidataSparqlApi())
}

private class FakeTmdbApi(private val totalPages: Int) : TmdbApi {

    val requestedPages = mutableListOf<Int>()

    override suspend fun searchMulti(query: String, language: String, page: Int): MovieSearchResponseDto {
        requestedPages += page
        return MovieSearchResponseDto(
            page = page,
            results = List(2) { index ->
                MovieDto(
                    id = (page * 100 + index).toLong(),
                    mediaType = "movie",
                    title = "Film $page-$index",
                    posterPath = "/poster$page$index.jpg",
                )
            },
            totalPages = totalPages,
        )
    }
}

private class FailingTmdbApi : TmdbApi {
    override suspend fun searchMulti(query: String, language: String, page: Int): MovieSearchResponseDto =
        throw java.io.IOException("offline")
}

private class FakeWikidataApi : WikidataApi {

    var searches = 0
        private set

    override suspend fun searchEntities(
        search: String,
        language: String,
        uselang: String,
        action: String,
        type: String,
        format: String,
        limit: Int,
    ): WikidataSearchResponseDto {
        searches++
        return WikidataSearchResponseDto()
    }
}

private class FakeWikidataSparqlApi : WikidataSparqlApi {
    override suspend fun query(query: String): WikidataSparqlResponseDto = WikidataSparqlResponseDto()
}
