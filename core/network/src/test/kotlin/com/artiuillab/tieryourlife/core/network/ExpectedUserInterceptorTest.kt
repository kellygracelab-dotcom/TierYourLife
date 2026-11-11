package com.artiuillab.tieryourlife.core.network

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ExpectedUserInterceptorTest {
    private var networkCalls = 0
    private var sent: Request? = null

    private fun client(code: Int = 200, token: () -> String?) = OkHttpClient.Builder()
        .addInterceptor(IdTokenInterceptor(token))
        .addInterceptor { chain ->
            networkCalls++
            sent = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("answer").body("".toResponseBody()).build()
        }.build()

    private fun request(expected: String? = "A") = Request.Builder().url("https://example.test/lists")
        .apply { if (expected != null) tag(ExpectedUser::class.java, ExpectedUser(expected)) }.build()

    @Test fun tokenAcquisitionThatSelectsBLateCannotSendTheActionConfirmedByA() {
        val action = request("A")
        var selectedAccount = "A"
        val http = client {
            selectedAccount = "B"
            jwt(selectedAccount)
        }
        val failure = runCatching { http.newCall(action).execute().close() }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals("B", selectedAccount)
        assertEquals(0, networkCalls)
    }

    @Test fun matchingSyntheticBearerKeepsTheExpectedActorAndExistingHeaders() {
        val bearer = jwt("A")
        client { bearer }.newCall(request().newBuilder().header("Accept", "application/json").build()).execute().close()
        assertEquals(1, networkCalls)
        assertEquals("Bearer $bearer", sent!!.header("Authorization"))
        assertEquals("application/json", sent!!.header("Accept"))
        assertEquals(ExpectedUser("A"), sent!!.tag(ExpectedUser::class.java))
    }

    @Test fun absentEmptyAndUnreadableBearerCannotSendATaggedAction() {
        for (token in listOf(null, "", "not-a-jwt")) {
            assertTrue(runCatching { client { token }.newCall(request()).execute().close() }.exceptionOrNull() is ExpectedUserMismatch)
        }
        assertEquals(0, networkCalls)
    }

    @Test fun untaggedExistingRequestsKeepTheirBearerOrGuestReadBehavior() {
        client { jwt("B") }.newCall(request(expected = null)).execute().close()
        client { null }.newCall(request(expected = null)).execute().close()
        assertEquals(2, networkCalls)
        assertEquals(null, sent!!.header("Authorization"))
    }

    @Test fun subjectClaimAlsoBindsASyntheticBearer() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"A"}""".toByteArray())
        client { "header.$payload.signature" }.newCall(request()).execute().close()
        assertEquals(1, networkCalls)
    }

    @Test fun a401RetryCannotAcquireBAndResendAPrivatePayload() {
        var refreshedToken = jwt("A")
        val http = client(code = 401) { refreshedToken }
        val action = request("A").newBuilder().post("synthetic-private-layout-A".toRequestBody()).build()
        val retry = http.newCall(action).execute().use { refused ->
            assertEquals(401, refused.code)
            assertEquals(1, networkCalls)
            assertEquals(ExpectedUser("A"), refused.request.tag(ExpectedUser::class.java))
            refused.request.newBuilder().build()
        }
        // No automatic Authenticator is installed in production. A fresh call after a token refresh still carries A's intent.
        refreshedToken = jwt("B")
        val failure = runCatching { http.newCall(retry).execute().close() }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals("only A's initial 401 reached the endpoint; B receives no retry payload", 1, networkCalls)
        assertEquals("Bearer ${jwt("A")}", sent!!.header("Authorization"))
    }

    private fun jwt(uid: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"user_id":"$uid","sub":"$uid"}""".toByteArray())
        return "header.$payload.signature"
    }
}
