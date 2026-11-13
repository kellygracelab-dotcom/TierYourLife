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

private const val AGENT = "TierYourLife/test"

class WikimediaUserAgentInterceptorTest {
    @Test
    fun everyWikimediaHost_getsTheAppsUserAgent() {
        val urls = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Tyrannosaurus_Rex_Holotype.jpg/500px-Tyrannosaurus_Rex_Holotype.jpg",
            "https://commons.wikimedia.org/wiki/Special:FilePath/Pizza.jpg",
            "https://en.wikipedia.org/wiki/Pizza",
            "https://www.wikidata.org/w/api.php",
            "https://query.wikidata.org/sparql",
        )
        for (url in urls) {
            assertEquals(url, AGENT, sentUserAgent(url))
        }
    }

    @Test
    fun otherHosts_keepTheirOwnUserAgent() {
        val urls = listOf(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/coqa1.jpg",
            "https://notwikimedia.org/picture.jpg",
            "https://upload.wikimedia.org.evil.test/picture.jpg",
            "https://tieryourlife.web.app/img/original/published/list-1/photo",
        )
        for (url in urls) {
            assertEquals(url, "okhttp/original", sentUserAgent(url))
        }
    }

    private fun sentUserAgent(url: String): String? {
        var sent: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(WikimediaUserAgentInterceptor(AGENT))
            .addInterceptor(
                Interceptor { chain ->
                    sent = chain.request()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody())
                        .build()
                },
            )
            .build()
        client.newCall(Request.Builder().url(url).header("User-Agent", "okhttp/original").build()).execute().close()
        return sent?.header("User-Agent")
    }
}
