package com.artiuillab.tieryourlife.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/** Another phone erased the account: every write after that comes back as this one refusal, from any feature. */
class AccountErasedInterceptorTest {

    private val heard = mutableListOf<String>()

    @Test
    fun anErasedAccount_isReportedByTheUidItsTokenNamed() {
        val answer = call(401, """{"error":"This account was deleted","code":"ACCOUNT_ERASED"}""", token = tokenFor("ann"))

        assertEquals(listOf("ann"), heard)
        assertEquals("the caller still reads the refusal", """{"error":"This account was deleted","code":"ACCOUNT_ERASED"}""", answer)
    }

    // A stale token is put right by signing in again; it says nothing about the account.
    @Test
    fun anyOtherUnauthorised_isNotAnErase() {
        call(401, """{"error":"Sign in","code":"UNAUTHENTICATED"}""", token = tokenFor("ann"))
        call(401, """{"code":"APP_UNVERIFIED"}""", token = tokenFor("ann"))
        call(401, "not json", token = tokenFor("ann"))

        assertEquals(emptyList<String>(), heard)
    }

    @Test
    fun theSameCodeOnAnotherStatus_isNotAnErase() {
        call(403, """{"code":"ACCOUNT_ERASED"}""", token = tokenFor("ann"))

        assertEquals(emptyList<String>(), heard)
    }

    @Test
    fun withoutAReadableToken_nobodyIsReported() {
        call(401, """{"code":"ACCOUNT_ERASED"}""", token = null)
        call(401, """{"code":"ACCOUNT_ERASED"}""", token = "not-a-jwt")

        assertEquals(emptyList<String>(), heard)
    }

    @Test
    fun theUid_isReadFromSubWhenUserIdIsMissing() {
        assertEquals("bob", uidOfBearer("Bearer " + jwt("""{"sub":"bob"}""")))
        assertNull(uidOfBearer(jwt("""{"sub":"bob"}""")))
    }

    private fun call(code: Int, body: String, token: String?): String {
        val client = OkHttpClient.Builder()
            .addInterceptor(IdTokenInterceptor { token })
            .addInterceptor(AccountErasedInterceptor { heard += it })
            .addInterceptor(Answer(code, body))
            .build()
        return client.newCall(Request.Builder().url("https://example.test/boards").post("{}".toRequestBody("application/json".toMediaType())).build())
            .execute()
            .use { requireNotNull(it.body).string() }
    }
}

private fun tokenFor(uid: String) = jwt("""{"iss":"https://securetoken.google.com/x","user_id":"$uid","sub":"$uid"}""")

private fun jwt(payload: String): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    return listOf("""{"alg":"RS256"}""", payload, "signature").joinToString(".") { encoder.encodeToString(it.toByteArray()) }
}

private class Answer(private val code: Int, private val body: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("refused")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}
