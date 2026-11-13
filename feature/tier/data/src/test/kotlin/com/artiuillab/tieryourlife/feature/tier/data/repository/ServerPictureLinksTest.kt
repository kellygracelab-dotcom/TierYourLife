package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PictureLinksApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAnswerDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAskDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureAttributionDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Proxy CONTRACT.md, `POST /lists/pictures/link`, as the picture picker hears it. */
class ServerPictureLinksTest {

    private val signedIn = FakeAccountRepository(Account.SignedIn(email = "a@example.org", photoUrl = null, uid = "u1"))

    @Test
    fun aPictureTheServerKept_comesDownToThePhone_withWhereItIsFrom() = runBlocking {
        val api = FakeLinksApi { LinkAnswerDto(pictureId = "p_1", attribution = PictureAttributionDto("web"), credit = " © Studio ") }
        val broughtHome = mutableListOf<Pair<String, String>>()
        val links = ServerPictureLinks(api, signedIn, { account, id ->
            broughtHome += account to id
            "/files/tier_images/$id"
        }, { 0L })

        val answer = links.fetch("  https://i.example.net/p/8812.jpg ")

        assertEquals(LinkPicture.Found("/files/tier_images/p_1", PictureAttribution("web"), "© Studio"), answer)
        assertEquals(listOf("u1" to "p_1"), broughtHome)
        assertEquals(listOf(LinkAskDto("https://i.example.net/p/8812.jpg")), api.asked)
    }

    @Test
    fun aCatalogueAddress_staysAsItIs_withTheCataloguesAttribution() = runBlocking {
        val api = FakeLinksApi {
            LinkAnswerDto(imageUrl = "https://image.tmdb.org/t/p/w500/a.jpg", attribution = PictureAttributionDto("tmdb", link = "https://www.themoviedb.org/movie/348"))
        }

        val answer = links(api).fetch("https://image.tmdb.org/t/p/w500/a.jpg")

        assertEquals(
            LinkPicture.Found("https://image.tmdb.org/t/p/w500/a.jpg", PictureAttribution("tmdb", link = "https://www.themoviedb.org/movie/348"), null),
            answer,
        )
    }

    @Test
    fun aPictureThatWouldNotComeDown_orAnAnswerWithNoPicture_isAFailure() = runBlocking {
        val kept = FakeLinksApi { LinkAnswerDto(pictureId = "p_1") }
        assertEquals(LinkPicture.Refused(LinkRefusal.Failed), ServerPictureLinks(kept, signedIn, { _, _ -> null }, { 0L }).fetch("https://x.org/a.jpg"))
        assertEquals(LinkPicture.Refused(LinkRefusal.Failed), links(FakeLinksApi { LinkAnswerDto(pictureId = "../a") }).fetch("https://x.org/a.jpg"))
        assertEquals(LinkPicture.Refused(LinkRefusal.Failed), links(FakeLinksApi { LinkAnswerDto(imageUrl = "http://x.org/a.jpg") }).fetch("https://x.org/a.jpg"))
    }

    @Test
    fun eachRefusal_isSaidByWhatSomebodyCanDoAboutIt() = runBlocking {
        val said = mapOf(
            "NOT_A_PICTURE" to LinkRefusal.NotAPicture,
            "UNREACHABLE" to LinkRefusal.Unreachable,
            "BAD_URL" to LinkRefusal.Unreachable,
            "NOT_ALLOWED" to LinkRefusal.Unreachable,
            "TOO_BIG" to LinkRefusal.TooBig,
            "PICTURE_REFUSED" to LinkRefusal.Refused,
            "SOMETHING_NEW" to LinkRefusal.Failed,
        )
        said.forEach { (code, reason) ->
            val api = FakeLinksApi { throw refusal(422, """{"code":"$code","quota":{"left":3,"limit":400,"resetsAt":5}}""") }
            assertEquals(code, LinkPicture.Refused(reason), links(api).fetch("https://x.org/a"))
        }
    }

    @Test
    fun theDaysLimit_namesWhenMoreCome_fromTheServersTimeOrItsHeader() = runBlocking {
        val withTime = FakeLinksApi { throw refusal(429, """{"code":"LINK_LIMIT","retryAt":7200000}""", retryAfter = "60") }
        assertEquals(LinkPicture.Limited(7_200_000L), links(withTime).fetch("https://x.org/a.jpg"))

        val withHeader = FakeLinksApi { throw refusal(429, """{"code":"LINK_LIMIT"}""", retryAfter = "60") }
        assertEquals(LinkPicture.Limited(1_000L + 60_000L), links(withHeader, now = 1_000L).fetch("https://x.org/a.jpg"))
    }

    @Test
    fun aGuest_isAskedToSignIn_withoutTheServerBeingAsked() = runBlocking {
        val api = FakeLinksApi { LinkAnswerDto(pictureId = "p_1") }

        val answer = ServerPictureLinks(api, FakeAccountRepository(Account.Guest), { _, _ -> "/x" }, { 0L }).fetch("https://x.org/a.jpg")

        assertEquals(LinkPicture.SignInNeeded, answer)
        assertTrue(api.asked.isEmpty())
    }

    @Test
    fun anAccountTheServerDoesNotKnowAsSignedIn_isAskedToSignIn_andNoNetworkIsAFailure() = runBlocking {
        val refused = FakeLinksApi { throw refusal(403, """{"code":"NOT_SIGNED_IN"}""") }
        assertEquals(LinkPicture.SignInNeeded, links(refused).fetch("https://x.org/a.jpg"))

        val offline = FakeLinksApi { throw IOException("offline") }
        assertEquals(LinkPicture.Refused(LinkRefusal.Failed), links(offline).fetch("https://x.org/a.jpg"))
    }

    private fun links(api: PictureLinksApi, now: Long = 0L) = ServerPictureLinks(api, signedIn, { _, id -> "/files/$id" }, { now })
}

private class FakeLinksApi(private val answer: () -> LinkAnswerDto) : PictureLinksApi {
    val asked = mutableListOf<LinkAskDto>()

    override suspend fun link(ask: LinkAskDto): LinkAnswerDto {
        asked += ask
        return answer()
    }
}

private fun refusal(code: Int, body: String, retryAfter: String? = null): HttpException {
    val raw = okhttp3.Response.Builder()
        .code(code)
        .message("Refused")
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url("https://proxy.invalid/lists/pictures/link").build())
        .apply { retryAfter?.let { header("Retry-After", it) } }
        .build()
    return HttpException(Response.error<LinkAnswerDto>(body.toResponseBody("application/json".toMediaType()), raw))
}
