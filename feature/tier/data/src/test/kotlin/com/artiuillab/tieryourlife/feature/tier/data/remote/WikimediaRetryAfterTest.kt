package com.artiuillab.tieryourlife.feature.tier.data.remote

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikimediaUserAgentInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

private const val THUMB = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Stegosaurus_ungulatus.jpg/500px-Stegosaurus_ungulatus.jpg"

class WikimediaRetryAfterTest {
    private val waits = mutableListOf<Long>()
    private var calls = 0

    @Test
    fun aShortRetryAfter_isWaitedOnceAndAskedAgain() {
        val code = fetch(THUMB, listOf(tooMany("1"), ok()))

        assertEquals(200, code)
        assertEquals(2, calls)
        assertEquals(listOf(1000L), waits)
    }

    @Test
    fun aSecondRefusal_isPassedOnWithoutAThirdTry() {
        val code = fetch(THUMB, listOf(tooMany("1"), tooMany("1"), ok()))

        assertEquals(429, code)
        assertEquals(2, calls)
    }

    @Test
    fun aLongMissingOrDatedRetryAfter_isNotWaitedFor() {
        for (retryAfter in listOf("120", null, "Fri, 13 Nov 2026 15:11:05 GMT", "-1")) {
            calls = 0
            val code = fetch(THUMB, listOf(tooMany(retryAfter), ok()))

            assertEquals(retryAfter, 429, code)
            assertEquals(retryAfter, 1, calls)
        }
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun otherHosts_areNeverRetried() {
        val code = fetch("https://images.igdb.com/igdb/image/upload/t_cover_big/coqa1.jpg", listOf(tooMany("1"), ok()))

        assertEquals(429, code)
        assertEquals(1, calls)
    }

    private fun fetch(url: String, answers: List<(Request) -> Response>): Int {
        val client = OkHttpClient.Builder()
            .addInterceptor(WikimediaUserAgentInterceptor("TierYourLife/test", pause = { waits += it }))
            .addInterceptor(Interceptor { chain -> answers[calls++](chain.request()) })
            .build()
        return client.newCall(Request.Builder().url(url).build()).execute().use { it.code }
    }

    private fun tooMany(retryAfter: String?): (Request) -> Response = { request ->
        answer(request, 429).apply { retryAfter?.let { header("Retry-After", it) } }.build()
    }

    private fun ok(): (Request) -> Response = { request -> answer(request, 200).build() }

    private fun answer(request: Request, code: Int) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("")
        .body("".toResponseBody())
}
