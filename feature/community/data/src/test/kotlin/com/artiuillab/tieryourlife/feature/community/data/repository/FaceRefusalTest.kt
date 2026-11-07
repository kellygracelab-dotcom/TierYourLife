package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.FaceRefused
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FaceRefusalTest {

    @Test
    fun aPictureSafeSearchTurnedDown_isItsOwnFailure() {
        assertTrue(failureOf(refusal(422, """{"error":"Refused","code":"PICTURE_REFUSED"}""")) is FaceRefused)
    }

    @Test
    fun anyOtherRefusal_comesThroughAsItWas() {
        val unavailable = refusal(503, """{"code":"UNAVAILABLE"}""")

        assertEquals(unavailable, failureOf(unavailable))
    }

    private fun failureOf(refusal: HttpException): Throwable? =
        runCatching { runBlocking { refusedAsAFace<Unit> { throw refusal } } }.exceptionOrNull()

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
