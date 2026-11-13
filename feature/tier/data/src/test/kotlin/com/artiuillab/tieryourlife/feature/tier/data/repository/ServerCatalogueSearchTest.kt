package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataSparqlApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureAttributionDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureHitDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PicturesDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
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
    fun aHitsCredit_comesWithIt_trimmed_andABlankOneIsNone() = runBlocking {
        val api = FakePicturesApi(
            PicturesDto(
                items = listOf(
                    PictureHitDto(id = "inat:1", title = "Red fox", imageUrl = "https://i/1.jpg", credit = "  © Jane Doe, CC BY "),
                    PictureHitDto(id = "inat:2", title = "Badger", imageUrl = "https://i/2.jpg", credit = " "),
                    PictureHitDto(id = "tmdb:3", title = "Fantastic Mr. Fox", imageUrl = "https://t/3.jpg"),
                ),
            ),
        )

        val page = search(api).search("fox", "en", 1, PictureScope.Animals).getOrThrow()

        assertEquals(listOf("© Jane Doe, CC BY", null, null), page.items.map { it.credit })
    }

    @Test
    fun aHitsAttribution_comesWithIt_whateverItsSource() = runBlocking {
        val commons = PictureAttributionDto(
            source = "wikimedia",
            author = "Jane Doe",
            license = "CC BY-SA 4.0",
            link = "https://commons.wikimedia.org/wiki/File:Fox.jpg",
        )
        val api = FakePicturesApi(
            PicturesDto(
                items = listOf(
                    PictureHitDto(id = "wikidata:1", title = "Red fox", imageUrl = "https://i/1.jpg", attribution = commons),
                    PictureHitDto(id = "x:2", title = "Badger", imageUrl = "https://i/2.jpg", attribution = PictureAttributionDto("later")),
                    PictureHitDto(id = "tmdb:3", title = "Fantastic Mr. Fox", imageUrl = "https://t/3.jpg"),
                ),
            ),
        )

        val page = search(api).search("fox", "en", 1, PictureScope.Animals).getOrThrow()

        assertEquals(
            listOf(PictureAttribution("wikimedia", "Jane Doe", "CC BY-SA 4.0", link = "https://commons.wikimedia.org/wiki/File:Fox.jpg"), PictureAttribution("later"), null),
            page.items.map { it.attribution },
        )
    }

    @Test
    fun everywhere_asksAllWithTheWeb_whateverTheScope() = runBlocking {
        val api = FakePicturesApi(PicturesDto())

        search(api).search("borscht", "en", 1, PictureScope.Games, everywhere = true)

        assertEquals(Asked("borscht", "all", "en", 1, "1"), api.asked.single())
    }

    @Test
    fun aQuickSearch_asksForTheQuickPart_andHearsWhetherThereIsMore() = runBlocking {
        val api = FakePicturesApi(PicturesDto(rest = true))

        val page = search(api).search("dune", "en", 1, PictureScope.Books, quick = true).getOrThrow()

        assertTrue(page.rest)
        assertEquals("quick", api.parts.single())
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

    // Proxy 429 SEARCH_LIMIT: 1000 an hour, 3000 a UTC day.
    @Test
    fun overTheSearchLimit_thePhoneAsksItsOwnCataloguesUntilTheServerSaysAndSaysUntilWhen() = runBlocking {
        var clock = 1_000L
        val api = FakePicturesApi(PicturesDto())
        api.refuse = { tooMany("""{"error":"Too many picture searches","code":"SEARCH_LIMIT","retryAt":3600000}""", retryAfter = "3599") }
        val tmdb = OneFilmTmdb()
        val search = search(api, tmdb, now = { clock })

        val limited = search.search("dune", "en", 1, PictureScope.Books).getOrThrow()
        api.refuse = null
        clock = 3_599_999L
        val stillLimited = search.search("dune", "en", 2, PictureScope.Books).getOrThrow()

        assertEquals(listOf("tmdb:7"), limited.items.map { it.id })
        assertEquals(SearchNotice.SearchLimit(3_600_000L), limited.notice)
        assertEquals(SearchNotice.SearchLimit(3_600_000L), stillLimited.notice)
        assertEquals(1, api.asked.size)
        assertEquals(2, tmdb.searches)

        clock = 3_600_000L
        val again = search.search("dune", "en", 1, PictureScope.Books).getOrThrow()
        assertNull(again.notice)
        assertEquals(2, api.asked.size)
    }

    @Test
    fun aLimitWithoutItsHour_waitsAsLongAsRetryAfterSays_andAnyOther429IsJustAFailure() = runBlocking {
        val api = FakePicturesApi(PicturesDto())
        api.refuse = { tooMany("""{"code":"SEARCH_LIMIT"}""", retryAfter = "120") }

        val limited = search(api, now = { 5_000L }).search("dune", "en", 1).getOrThrow()

        assertEquals(SearchNotice.SearchLimit(125_000L), limited.notice)

        val other = FakePicturesApi(PicturesDto())
        other.refuse = { tooMany("""{"error":"Too many requests"}""", retryAfter = "60") }
        val search = search(other, now = { 5_000L })
        assertNull(search.search("dune", "en", 1).getOrThrow().notice)
        search.search("dune", "en", 1)
        assertEquals(2, other.asked.size)
    }

    private fun search(api: PicturesApi, tmdb: TmdbApi = OneFilmTmdb(), now: () -> Long = { 0L }) =
        ServerCatalogueSearch(api, CatalogueSearchRepositoryImpl(tmdb, NoWikidata(), NoSparql()), now)
}

private fun tooMany(body: String, retryAfter: String): HttpException {
    val raw = okhttp3.Response.Builder()
        .code(429)
        .message("Too Many Requests")
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url("https://proxy.invalid/pictures").build())
        .header("Retry-After", retryAfter)
        .build()
    return HttpException(Response.error<PicturesDto>(body.toResponseBody("application/json".toMediaType()), raw))
}

private data class Asked(val query: String, val scope: String, val language: String, val page: Int, val web: String?)

private class FakePicturesApi(private val answer: PicturesDto = PicturesDto(), private val failure: IOException? = null) : PicturesApi {
    val asked = mutableListOf<Asked>()
    val parts = mutableListOf<String?>()
    var refuse: (() -> Exception)? = null

    override suspend fun search(query: String, scope: String, language: String, page: Int, web: String?, part: String?): PicturesDto {
        asked += Asked(query, scope, language, page, web)
        parts += part
        failure?.let { throw it }
        refuse?.let { throw it() }
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
