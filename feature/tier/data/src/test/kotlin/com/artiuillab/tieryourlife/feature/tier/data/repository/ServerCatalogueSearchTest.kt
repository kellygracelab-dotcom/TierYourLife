package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataSparqlApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureHitDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PicturesDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ServerCatalogueSearchTest {

    @Test
    fun theServersAnswer_isWhatShows_webPicturesMarkedAsSuch() = runBlocking {
        val api = FakePicturesApi(
            PicturesDto(
                items = listOf(
                    PictureHitDto(id = "anilist:20", title = "Naruto", subtitle = "2002", imageUrl = "https://a/20.jpg", thumbUrl = "https://a/20m.jpg", source = "anilist"),
                    PictureHitDto(id = "web:x", title = "Naruto fan art", imageUrl = "https://site/x.jpg", source = "web"),
                ),
                hasMore = true,
                notices = listOf("somethingNewer", "webDailyLimit"),
            ),
        )

        val page = search(api).search("naruto", "ru", 1, PictureScope.Anime).getOrThrow()

        assertEquals(listOf("anilist:20", "web:x"), page.items.map { it.id })
        assertEquals("https://a/20m.jpg", page.items[0].thumbUrl)
        assertEquals(listOf(false, true), page.items.map { it.fromWeb })
        assertTrue(page.hasMore)
        assertEquals(SearchNotice.WebDailyLimit, page.notice)
        assertEquals(listOf(Asked("naruto", "anime", "ru", 1, null)), api.asked)
    }

    @Test
    fun everywhere_asksAllWithTheWeb_whateverTheScope() = runBlocking {
        val api = FakePicturesApi(PicturesDto())

        search(api).search("borscht", "en", 1, PictureScope.Games, everywhere = true)

        assertEquals(Asked("borscht", "all", "en", 1, "1"), api.asked.single())
    }

    @Test
    fun aServerThatCannotBeReached_leavesTheCataloguesThePhoneCanAsk() = runBlocking {
        val tmdb = OneFilmTmdb()

        val page = search(FakePicturesApi(failure = IOException("offline")), tmdb).search("dune", "en", 1, PictureScope.Books).getOrThrow()

        assertEquals(listOf("tmdb:7"), page.items.map { it.id })
        assertNull(page.notice)
        assertEquals(1, tmdb.searches)
    }

    @Test
    fun aQueryTooShortForTheServer_isNotSentToIt() = runBlocking {
        val api = FakePicturesApi(PicturesDto())

        val page = search(api).search(" a ", "en", 1).getOrThrow()

        assertTrue(api.asked.isEmpty())
        assertFalse(page.hasMore)
    }

    private fun search(api: PicturesApi, tmdb: TmdbApi = OneFilmTmdb()) =
        ServerCatalogueSearch(api, CatalogueSearchRepositoryImpl(tmdb, NoWikidata(), NoSparql()))
}

private data class Asked(val query: String, val scope: String, val language: String, val page: Int, val web: String?)

private class FakePicturesApi(private val answer: PicturesDto = PicturesDto(), private val failure: IOException? = null) : PicturesApi {
    val asked = mutableListOf<Asked>()

    override suspend fun search(query: String, scope: String, language: String, page: Int, web: String?): PicturesDto {
        asked += Asked(query, scope, language, page, web)
        failure?.let { throw it }
        return answer
    }
}

private class OneFilmTmdb : TmdbApi {
    var searches = 0

    override suspend fun searchMulti(query: String, language: String, page: Int): MovieSearchResponseDto {
        searches++
        return MovieSearchResponseDto(
            page = 1,
            results = listOf(MovieDto(id = 7, mediaType = "movie", title = "Dune", posterPath = "/dune.jpg")),
            totalPages = 1,
        )
    }
}

private class NoWikidata : WikidataApi {
    override suspend fun searchEntities(
        search: String,
        language: String,
        uselang: String,
        action: String,
        type: String,
        format: String,
        limit: Int,
    ): WikidataSearchResponseDto = throw IOException("not asked in this test")
}

private class NoSparql : WikidataSparqlApi {
    override suspend fun query(query: String): WikidataSparqlResponseDto = throw IOException("not asked in this test")
}
