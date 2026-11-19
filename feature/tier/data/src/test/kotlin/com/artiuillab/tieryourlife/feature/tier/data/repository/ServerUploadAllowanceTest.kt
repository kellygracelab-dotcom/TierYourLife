package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PictureLinksApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAnswerDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAskDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.UploadsAnswerDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.UploadsDto
import com.artiuillab.tieryourlife.feature.tier.domain.repository.UploadRoom
import com.artiuillab.tieryourlife.feature.tier.domain.repository.budget
import com.artiuillab.tieryourlife.feature.tier.domain.repository.fits
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Proxy #174: the day's limit on own pictures, asked once for a whole choice. */
class ServerUploadAllowanceTest {

    @Test
    fun room_isWhatIsLeft_andTheCountAskedIsWithinOneToAHundred() = runBlocking {
        val api = Api { count ->
            asked += count
            UploadsAnswerDto(UploadsDto(used = 1990, left = 10, limit = 2000, resetsAt = 5_000))
        }

        val room = ServerUploadAllowance(api) { 0 }.ask(150)

        assertEquals(UploadRoom.Left(10, 5_000), room)
        assertEquals(listOf<Int?>(100), api.asked)
        assertTrue(room.fits(10))
        assertFalse(room.fits(11))
    }

    @Test
    fun anAccountOverTheLimit_waitsForTomorrow_aGuestMaySignIn() = runBlocking {
        val account = refusal(429, """{"code":"UPLOAD_DAILY_LIMIT","retryAt":9000,"uploads":{"used":2000,"left":0,"limit":2000}}""")
        val guest = refusal(429, """{"code":"SIGN_IN_FOR_MORE_UPLOADS","retryAt":9000}""")

        assertEquals(UploadRoom.Full(9_000, guest = false), ServerUploadAllowance(Api { throw account }) { 0 }.ask(3))
        assertEquals(UploadRoom.Full(9_000, guest = true), ServerUploadAllowance(Api { throw guest }) { 0 }.ask(3))
        assertEquals(0, UploadRoom.Full(9_000, guest = false).budget())
    }

    @Test
    fun noAnswer_isUnknown_andEverythingGoesAsBefore() = runBlocking {
        val offline = ServerUploadAllowance(Api { throw IOException("offline") }) { 0 }.ask(null)
        val other = ServerUploadAllowance(Api { throw refusal(429, """{"code":"RATE_LIMITED"}""") }) { 0 }.ask(null)

        assertEquals(UploadRoom.Unknown, offline)
        assertEquals(UploadRoom.Unknown, other)
        assertTrue(offline.fits(500))
        assertEquals(Int.MAX_VALUE, offline.budget())
    }

    private class Api(private val answer: Api.(Int?) -> UploadsAnswerDto) : PictureLinksApi {
        val asked = mutableListOf<Int?>()
        override suspend fun link(ask: LinkAskDto): LinkAnswerDto = error("not asked")
        override suspend fun uploads(count: Int?): UploadsAnswerDto = answer(count)
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
