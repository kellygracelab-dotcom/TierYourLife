package com.artiuillab.tieryourlife.feature.tier.data.remote

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PictureSearchTimeouts
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class PictureSearchTimeoutsTest {

    @Test
    fun theQuickPart_waits20Seconds_andTheWholeSearch45_notTheClientsUsual10() {
        val waited = mutableListOf<Int>()
        // What the account's own interceptors pass on goes after these waits, as the call to the server does.
        val client = PictureSearchTimeouts.on(OkHttpClient())
            .newBuilder()
            .addInterceptor { chain ->
                    waited += chain.readTimeoutMillis()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody())
                        .build()
                }
            .build()

        client.newCall(Request.Builder().url("https://proxy.invalid/lists/pictures?q=dune&part=quick").build()).execute().close()
        client.newCall(Request.Builder().url("https://proxy.invalid/lists/pictures?q=dune").build()).execute().close()

        assertEquals(listOf(20_000, 45_000), waited)
    }
}
