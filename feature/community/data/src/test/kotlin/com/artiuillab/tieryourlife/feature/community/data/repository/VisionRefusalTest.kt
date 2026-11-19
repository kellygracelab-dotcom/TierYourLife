package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.FaceRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.PicturesCheckedToday
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportsToday
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Proxy #173: a photograph SafeSearch cannot judge, and the day's limits on checks and reports, each said as itself. */
class VisionRefusalTest {

    private val unchecked = refusal(422, """{"error":"Refused","code":"PICTURE_REFUSED","because":"unchecked"}""")
    private val adult = refusal(422, """{"error":"Refused","code":"PICTURE_REFUSED","because":"adult"}""")
    private val checkedToday = refusal(429, """{"error":"Too many","code":"VISION_DAILY_LIMIT","retryAt":1763600000000}""")
    private val reportsToday = refusal(429, """{"error":"Too many","code":"REPORT_DAILY_LIMIT","retryAt":1763600000000}""")

    @Test
    fun publishing_tellsAPictureThatCannotBeChecked_fromOneRefused() {
        assertEquals(PublishError.PictureUnchecked, unchecked.asPublishError())
        assertEquals(PublishError.PictureRefused, adult.asPublishError())
        assertEquals(PublishError.PictureRefused, refusal(422, """{"code":"PICTURE_REFUSED"}""").asPublishError())
    }

    @Test
    fun publishing_namesTheDaysChecks() {
        assertEquals(PublishError.PicturesCheckedToday, checkedToday.asPublishError())
    }

    @Test
    fun aRanking_saysTheSame() {
        assertEquals(PublishError.PictureUnchecked, unchecked.boardRankingError())
        assertEquals(PublishError.PictureRefused, adult.boardRankingError())
        assertEquals(PublishError.PicturesCheckedToday, checkedToday.boardRankingError())
    }

    @Test
    fun aFace_saysWhichItWas() {
        val notJudged = faceFailure(unchecked)
        val refused = faceFailure(adult)

        assertTrue(notJudged is FaceRefused && notJudged.unchecked)
        assertTrue(refused is FaceRefused && !refused.unchecked)
        assertTrue(faceFailure(checkedToday) is PicturesCheckedToday)
    }

    @Test
    fun aReport_overTheDaysLimit_isItsOwnRefusal() {
        assertTrue(reportsToday.named() is ReportsToday)
        val another = refusal(429, """{"code":"RATE_LIMITED"}""")
        assertSame(another, another.named())
        assertFalse(checkedToday.named() is ReportsToday)
    }

    private fun faceFailure(refusal: HttpException): Throwable? =
        runCatching { runBlocking { refusedAsAFace<Unit> { throw refusal } } }.exceptionOrNull()

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
