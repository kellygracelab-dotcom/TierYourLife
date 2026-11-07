package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.rankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Proxy #97 fetches pictures from other sites itself, and has two limits on them. */
class WebPicturesRefusalTest {

    private val today = refusal(429, """{"error":"Too many pictures from other sites today","code":"WEB_PICTURES_DAILY_LIMIT"}""")
    private val tooMany = refusal(413, """{"error":"Too many pictures from other sites","code":"TOO_LARGE"}""")

    @Test
    fun publishing_namesBothLimits() {
        assertEquals(PublishError.WebPicturesToday, today.asPublishError())
        assertEquals(PublishError.TooManyWebPictures, tooMany.asPublishError())
    }

    @Test
    fun anyOtherSizeLimit_isStillTooLarge() {
        assertEquals(PublishError.TooLarge, refusal(413, """{"error":"Too many own pictures","code":"TOO_LARGE"}""").asPublishError())
        assertEquals(PublishError.TooLarge, refusal(413, "").asPublishError())
    }

    @Test
    fun anotherTooManyRequests_isNotTheDailyLimit() {
        assertEquals(PublishError.Unknown, refusal(429, """{"code":"RATE_LIMITED"}""").asPublishError())
    }

    @Test
    fun showingARanking_namesBothLimits() {
        assertEquals(RankingRefusal.WebPicturesToday, today.rankingRefusal())
        assertEquals(RankingRefusal.TooManyWebPictures, tooMany.rankingRefusal())
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
