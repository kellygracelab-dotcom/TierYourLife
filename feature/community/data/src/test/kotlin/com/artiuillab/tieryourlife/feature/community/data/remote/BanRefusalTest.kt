package com.artiuillab.tieryourlife.feature.community.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BanRefusalTest {

    @Test
    fun aBanWithAnEnd_saysWhen() {
        val body = """{"error":"You cannot publish at the moment","code":"BANNED","until":1790000000000}"""

        assertEquals(BanNotice(untilMillis = 1_790_000_000_000), refusal(403, body).banNotice())
    }

    @Test
    fun aBanWithoutAnEnd_hasNoDate() {
        assertEquals(BanNotice(untilMillis = null), refusal(403, """{"code":"BANNED","until":null}""").banNotice())
    }

    // The same status answers somebody who is not signed in.
    @Test
    fun aForbiddenWithoutTheCode_isNotABan() {
        assertNull(refusal(403, """{"error":"Sign in first","code":"NOT_SIGNED_IN"}""").banNotice())
    }

    @Test
    fun anUnreadableBody_isNotABan() {
        assertNull(refusal(403, "<html>").banNotice())
    }

    @Test
    fun anythingElse_isNotABan() {
        assertNull(refusal(401, """{"code":"BANNED"}""").banNotice())
        assertNull(IOException("offline").banNotice())
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType())))
}
