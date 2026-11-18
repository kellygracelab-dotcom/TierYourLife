package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureAttributionDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureHitDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PicturesDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchRefusal
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
import java.util.Locale

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
    fun aServerThatCannotBeReached_isAFailure_noCatalogueAskedBehindItsBack() = runBlocking {
        val result = search(FakePicturesApi(failure = IOException("offline"))).search("dune", "en", 1, PictureScope.Books)

        assertTrue(result.exceptionOrNull() is IOException)
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
    fun overTheSearchLimit_theSearchStopsUntilTheServerSays_andSaysUntilWhen() = runBlocking {
        var clock = 1_000L
        val api = FakePicturesApi(PicturesDto())
        api.refuse = { refused(429, """{"error":"Too many picture searches","code":"SEARCH_LIMIT","retryAt":3600000}""", retryAfter = "3599") }
        val search = search(api, now = { clock })

        val limited = search.search("dune", "en", 1, PictureScope.Books).exceptionOrNull()
        api.refuse = null
        clock = 3_599_999L
        val stillLimited = search.search("dune", "en", 2, PictureScope.Books).exceptionOrNull()

        assertEquals(3_600_000L, (limited as SearchRefusal.Limit).retryAt)
        assertEquals(3_600_000L, (stillLimited as SearchRefusal.Limit).retryAt)
        assertEquals(1, api.asked.size)

        clock = 3_600_000L
        assertTrue(search.search("dune", "en", 1, PictureScope.Books).isSuccess)
        assertEquals(2, api.asked.size)
    }

    @Test
    fun aLimitWithoutItsHour_waitsAsLongAsRetryAfterSays_andAnyOther429IsJustAFailure() = runBlocking {
        val api = FakePicturesApi(PicturesDto())
        api.refuse = { refused(429, """{"code":"SEARCH_LIMIT"}""", retryAfter = "120") }

        val limited = search(api, now = { 5_000L }).search("dune", "en", 1).exceptionOrNull()

        assertEquals(125_000L, (limited as SearchRefusal.Limit).retryAt)

        val other = FakePicturesApi(PicturesDto())
        other.refuse = { refused(429, """{"error":"Too many requests"}""", retryAfter = "60") }
        val search = search(other, now = { 5_000L })
        assertTrue(search.search("dune", "en", 1).exceptionOrNull() is HttpException)
        search.search("dune", "en", 1)
        assertEquals(2, other.asked.size)
    }

    // Proxy 503 SOURCE_BUSY: nothing found while a catalogue the search needs is busy.
    @Test
    fun aBusyCatalogue_isSaid_withWhenToAskAgain_byItsBodyOrItsHeader() = runBlocking {
        val api = FakePicturesApi(PicturesDto())
        api.refuse = { refused(503, """{"code":"SOURCE_BUSY","retryAt":9000,"busy":["anilist"]}""", retryAfter = "4") }
        val busy = search(api, now = { 1_000L }).search("naruto", "en", 1, PictureScope.Anime).exceptionOrNull()
        assertEquals(9_000L, (busy as SearchRefusal.Busy).retryAt)

        api.refuse = { refused(503, """{"code":"SOURCE_BUSY"}""", retryAfter = "4") }
        val byHeader = search(api, now = { 1_000L }).search("naruto", "en", 1, PictureScope.Anime).exceptionOrNull()
        assertEquals(5_000L, (byHeader as SearchRefusal.Busy).retryAt)

        api.refuse = { refused(503, """{"code":"UNAVAILABLE"}""", retryAfter = "4") }
        assertTrue(search(api).search("naruto", "en", 1).exceptionOrNull() is HttpException)
    }

    @Test
    fun aGapLeftByABusyCatalogue_comesWithTheAnswer_andWhenToFillIt() = runBlocking {
        val hit = PictureHitDto(id = "tmdb:1", title = "Naruto", imageUrl = "https://t/1.jpg", source = "tmdb", kind = "tv")
        val api = FakePicturesApi(PicturesDto(items = listOf(hit), busy = listOf("anilist"), retryAt = 9_000))

        val page = search(api).search("naruto", "en", 1, PictureScope.Anime).getOrThrow()

        assertTrue(page.busy)
        assertEquals(9_000L, page.retryAt)
        assertEquals("tv", page.items.single().kind)
        assertEquals("tmdb", page.items.single().source)

        val whole = search(FakePicturesApi(PicturesDto(items = listOf(hit.copy(kind = "", source = ""))))).search("naruto", "en", 1).getOrThrow()
        assertFalse(whole.busy)
        assertNull(whole.retryAt)
        assertNull(whole.items.single().kind)
        assertNull(whole.items.single().source)
    }

    @Test
    fun theLanguage_isTheSitesTag_soBothShareTheServersAnswer() = runBlocking {
        val api = FakePicturesApi(PicturesDto())
        val american = ServerCatalogueSearch(api, { 0L }, { Locale.US })

        american.search("dune", null, 1)
        american.search("dune", "pt-BR", 1)
        american.search("dune", "uk", 1)
        ServerCatalogueSearch(api, { 0L }, { Locale.forLanguageTag("pt-PT") }).search("dune", null, 1)
        ServerCatalogueSearch(api, { 0L }, { Locale.ITALIAN }).search("dune", null, 1)
        ServerCatalogueSearch(api, { 0L }, { Locale.forLanguageTag("es-MX") }).search("dune", null, 1)

        assertEquals(listOf("en", "pt-BR", "uk", "pt-BR", "en", "es"), api.asked.map { it.language })
        assertEquals("ja", searchLanguage("ja", Locale.US))
    }

    // Stage 1 of the picture search: the list's section always, whatever the scope, and its topic when it has one.
    @Test
    fun theListsSectionAndTopic_goWithTheSearch_whateverTheScope() = runBlocking {
        val api = FakePicturesApi(PicturesDto())
        val search = search(api)

        search.search("sushi", "en", 1, PictureScope.All, kind = ListKind(section = "food", topic = "t-dishes"))
        search.search("dune", "en", 1, PictureScope.Books, everywhere = true, kind = ListKind(section = "books"))
        search.search("arsenal", "en", 1, PictureScope.All, kind = ListKind(section = " ", topic = ""))
        search.search("matrix", "en", 1)

        assertEquals(listOf("food" to "t-dishes", "books" to null, null to null, null to null), api.kinds)
    }

    @Test
    fun anAnswerFoundUnderOtherWords_saysWhich_andAPlantIsReadAsAnyKind() = runBlocking {
        val plant = PictureHitDto(id = "inat:1", title = "Rose", imageUrl = "https://i/1.jpg", source = "inaturalist", kind = "plant")
        val api = FakePicturesApi(
            networkJson.decodeFromString<PicturesDto>(
                """{"items":[],"hasMore":false,"searchedAs":{"text":"Interstellar","how":"spelling"}}""",
            ).copy(items = listOf(plant)),
        )

        val page = search(api).search("Intersteller", "en", 1).getOrThrow()

        assertEquals("Interstellar", page.searchedAs)
        assertEquals("plant", page.items.single().kind)
        assertNull(search(FakePicturesApi(PicturesDto(items = listOf(plant)))).search("rose", "en", 1).getOrThrow().searchedAs)
    }

    @Test
    fun theNewFieldsOfAHit_doNotStopTheAnswerBeingRead() = runBlocking {
        val body = """{"items":[{"id":"w:1","title":"Kyiv","imageUrl":"https://w/1.jpg","source":"wikidata","kind":"thing",""" +
            """"matchedName":"Киев","confidence":"medium","doubts":["otherName","whatever"]}],"hasMore":false}"""
        val page = search(FakePicturesApi(networkJson.decodeFromString<PicturesDto>(body))).search("Киев", "en", 1).getOrThrow()

        assertEquals("Kyiv", page.items.single().title)
    }

    private fun search(api: PicturesApi, now: () -> Long = { 0L }) = ServerCatalogueSearch(api, now) { Locale.US }
}

private fun refused(code: Int, body: String, retryAfter: String): HttpException {
    val raw = okhttp3.Response.Builder()
        .code(code)
        .message("Refused")
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

    val kinds = mutableListOf<Pair<String?, String?>>()

    override suspend fun search(
        query: String,
        scope: String,
        language: String,
        page: Int,
        web: String?,
        part: String?,
        section: String?,
        topic: String?,
    ): PicturesDto {
        asked += Asked(query, scope, language, page, web)
        parts += part
        kinds += section to topic
        failure?.let { throw it }
        refuse?.let { throw it() }
        return answer
    }
}
