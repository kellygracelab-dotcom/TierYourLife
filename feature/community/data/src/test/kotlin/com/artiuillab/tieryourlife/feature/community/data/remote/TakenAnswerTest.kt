package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.ProxyModule
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

/** Proxy #96: a take that does not count, from a guest or an account under a week old, answers `{counted: false}`. */
class TakenAnswerTest {

    @Test
    fun aTakeThatDoesNotCount_isStillAnAnswer() = runBlocking {
        assertEquals(Unit, api(200, """{"counted":false}""").noteTaken("list-1"))
    }

    @Test
    fun aTakeThatCounts_andTheOldEmptyAnswer_readTheSame() = runBlocking {
        assertEquals(Unit, api(200, """{"counted":true}""").noteTaken("list-1"))
        assertEquals(Unit, api(204, "").noteTaken("list-1"))
    }

    private fun api(code: Int, body: String): CommunityApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("answer")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return ProxyModule.proxyRetrofit("https://example.test/", client).create(CommunityApi::class.java)
    }
}
