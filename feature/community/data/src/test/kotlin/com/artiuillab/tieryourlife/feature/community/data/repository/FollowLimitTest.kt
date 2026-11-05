package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.TooManyFollowed
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FollowLimitTest {

    @Test
    fun theLimit_isItsOwnFailure() {
        val failure = failureOf(refusal(409, """{"error":"Following too many people","code":"TOO_MANY_FOLLOWING"}"""))

        assertTrue(failure is TooManyFollowed)
    }

    @Test
    fun anyOtherRefusal_comesThroughAsItWas() {
        val yourself = refusal(409, """{"code":"YOURSELF"}""")
        val guest = refusal(403, """{"code":"NOT_SIGNED_IN"}""")

        assertEquals(yourself, failureOf(yourself))
        assertEquals(guest, failureOf(guest))
    }

    @Test
    fun aFollowThatWent_isItsAnswer() = runBlocking {
        assertEquals(1, refusedOverTheLimit { 1 })
    }

    private fun failureOf(refusal: HttpException): Throwable? =
        runCatching { runBlocking { refusedOverTheLimit<Unit> { throw refusal } } }.exceptionOrNull()

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
