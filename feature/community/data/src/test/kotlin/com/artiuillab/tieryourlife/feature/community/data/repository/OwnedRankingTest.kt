package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.ExpectedUser
import com.artiuillab.tieryourlife.core.network.ExpectedUserMismatch
import com.artiuillab.tieryourlife.core.network.IdTokenInterceptor
import com.artiuillab.tieryourlife.core.network.ProxyModule
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShowRankingRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.rankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class OwnedRankingTest {
    private var networkCalls = 0
    private var sentBody = ""
    private var sentTag: ExpectedUser? = null
    private val request = ShowRankingRequestDto(listOf(0), listOf(listOf(0)), listOf("S"), version = 42)
    private fun api(token: () -> String?): CommunityApi {
        val client = OkHttpClient.Builder().addInterceptor(IdTokenInterceptor(token)).addInterceptor { chain ->
            networkCalls++
            sentTag = chain.request().tag(ExpectedUser::class.java)
            sentBody = Buffer().also { chain.request().body?.writeTo(it) }.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"uid":"A","authorName":"A","arrangement":[0],"tierLabels":["S"],"updatedAtMs":1,"created":true}""".toResponseBody("application/json".toMediaType())).build()
        }.build()
        return ProxyModule.proxyRetrofit("https://example.test/", client).create(CommunityApi::class.java)
    }

    @Test fun anOwnerAlreadyChangedCannotBeginTheRankingRequest() = runBlocking {
        val failure = runCatching { rankingForOwner("source", request, "A", { "B" }, api { jwt("B") }) }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals(0, networkCalls)
    }

    @Test fun lateBearerSelectionOfBCannotPutAsBAfterAPassedTheEarlyCheck() = runBlocking {
        val failure = runCatching { rankingForOwner("source", request, "A", { "A" }, api { jwt("B") }) }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals(0, networkCalls)
    }

    @Test fun matchingActorKeepsTheVersionWireAndOnlyAddsALocalTag() = runBlocking {
        rankingForOwner("source", request, "A", { "A" }, api { jwt("A") })
        assertEquals(1, networkCalls)
        assertEquals(ExpectedUser("A"), sentTag)
        assertTrue(sentBody.contains("\"version\":42"))
        assertFalse(sentBody.contains("expectedOwnerUid"))
        assertFalse(sentBody.contains("ExpectedUser"))
    }

    @Test fun unboundLegacyRankingCallRemainsUntagged() = runBlocking {
        rankingForOwner("source", request, null, { "B" }, api { jwt("B") })
        assertEquals(1, networkCalls)
        assertEquals(null, sentTag)
    }

    @Test fun localActorRefusalUsesExistingSignInErrorsWithoutChangingOtherMappers() {
        assertEquals(RankingRefusal.NotSignedIn, ExpectedUserMismatch().rankingRefusal())
        assertEquals(PublishError.NotSignedIn, ExpectedUserMismatch().asPublishError())
    }

    private fun jwt(uid: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"user_id":"$uid","sub":"$uid"}""".toByteArray())
        return "header.$payload.signature"
    }
}
