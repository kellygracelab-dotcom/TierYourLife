package com.artiuillab.tieryourlife.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * These two decide whether the proxy will speak to us at all, and an evening
 * was spent on a night they quietly stopped: every screen said something
 * different and none of them said "no token".
 */
class InterceptorTest {

    @Test
    fun appCheck_sendsTheTokenItWasGiven() {
        val sent = send(AppCheckInterceptor { "token-1" })

        assertEquals("token-1", sent.header(APP_CHECK_HEADER))
    }

    // Sent bare rather than not sent: the refusal is the server's to make, and
    // the server names the reason. Dropping the call here would leave the app
    // guessing about a network that is fine.
    @Test
    fun appCheck_withNoToken_stillSendsTheRequest() {
        val sent = send(AppCheckInterceptor { null })

        assertNull(sent.header(APP_CHECK_HEADER))
        assertEquals("https://example.test/feed", sent.url.toString())
    }

    // An empty string is what a token source returns when it half-worked, and
    // an empty header reads to the server as a malformed token rather than as
    // an absent one.
    @Test
    fun appCheck_withAnEmptyToken_sendsNoHeaderAtAll() {
        val sent = send(AppCheckInterceptor { "" })

        assertNull(sent.header(APP_CHECK_HEADER))
    }

    @Test
    fun appCheck_leavesEveryOtherHeaderAlone() {
        val sent = send(
            AppCheckInterceptor { "token-1" },
            Request.Builder().url("https://example.test/feed").header("Accept", "application/json").build(),
        )

        assertEquals("application/json", sent.header("Accept"))
    }

    @Test
    fun idToken_sendsTheTokenAsABearer() {
        val sent = send(IdTokenInterceptor { "token-2" })

        assertEquals("Bearer token-2", sent.header(AUTHORIZATION_HEADER))
    }

    @Test
    fun idToken_withNoToken_stillSendsTheRequest() {
        val sent = send(IdTokenInterceptor { null })

        assertNull(sent.header(AUTHORIZATION_HEADER))
        assertEquals("https://example.test/feed", sent.url.toString())
    }

    @Test
    fun idToken_withAnEmptyToken_sendsNoHeaderAtAll() {
        val sent = send(IdTokenInterceptor { "" })

        assertNull(sent.header(AUTHORIZATION_HEADER))
    }

    // Both are installed on the same client, and each has to leave the other's
    // header where it found it.
    @Test
    fun bothTogether_eachAddsItsOwnAndKeepsTheOther() {
        val recorder = Recorder()
        OkHttpClient.Builder()
            .addInterceptor(AppCheckInterceptor { "token-1" })
            .addInterceptor(IdTokenInterceptor { "token-2" })
            .addInterceptor(recorder)
            .build()
            .newCall(Request.Builder().url("https://example.test/feed").build())
            .execute()

        val sent = requireNotNull(recorder.request)
        assertEquals("token-1", sent.header(APP_CHECK_HEADER))
        assertEquals("Bearer token-2", sent.header(AUTHORIZATION_HEADER))
    }
}

private fun send(
    interceptor: Interceptor,
    request: Request = Request.Builder().url("https://example.test/feed").build(),
): Request {
    val recorder = Recorder()
    OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .addInterceptor(recorder)
        .build()
        .newCall(request)
        .execute()
    return requireNotNull(recorder.request)
}

/** Answers every call itself, so nothing here touches a network. */
private class Recorder : Interceptor {
    var request: Request? = null
        private set

    override fun intercept(chain: Interceptor.Chain): Response {
        request = chain.request()
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(null))
            .build()
    }
}
