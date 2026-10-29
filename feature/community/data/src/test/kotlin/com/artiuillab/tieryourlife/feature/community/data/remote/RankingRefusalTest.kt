package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShowRankingRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShownRankingDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toShown
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** `PUT lists/{id}/rankings/mine`: what is sent, what comes back, and each refusal as the screen is told it. */
class RankingRefusalTest {

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun aGuest_isToldToSignIn() {
        assertEquals(RankingRefusal.NotSignedIn, refusal(403, """{"code":"NOT_SIGNED_IN"}""").rankingRefusal())
    }

    @Test
    fun aBan_andOnesOwnList_areTheirOwnRefusals() {
        assertEquals(RankingRefusal.Banned, refusal(403, """{"code":"BANNED","until":1}""").rankingRefusal())
        assertEquals(RankingRefusal.OwnList, refusal(409, """{"code":"OWN_LIST"}""").rankingRefusal())
    }

    @Test
    fun anythingElse_isAFailureToTryAgain() {
        assertEquals(RankingRefusal.Failed, refusal(400, """{"code":"INVALID"}""").rankingRefusal())
        assertEquals(RankingRefusal.Failed, refusal(404, "not json").rankingRefusal())
        assertEquals(RankingRefusal.Failed, IOException("offline").rankingRefusal())
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun anUnplacedCard_travelsAsNull() {
        val body = json.encodeToString(ShowRankingRequestDto.serializer(), ShowRankingRequestDto(listOf(1, null, 0), listOf("S", "A")))

        assertEquals("""{"arrangement":[1,null,0],"tierLabels":["S","A"]}""", body)
    }

    @Test
    fun theAnswer_isThisReadersOwn_andSaysWhetherItIsNew() {
        val answer = json.decodeFromString(
            ShownRankingDto.serializer(),
            """{"uid":"me","authorName":"Ann","arrangement":[0,null],"tierLabels":["S","A"],"updatedAtMs":5,"created":true}""",
        ).toShown()

        assertTrue(answer.created)
        assertTrue(answer.ranking.mine)
        assertEquals(listOf(0, null), answer.ranking.arrangement)
    }
}
