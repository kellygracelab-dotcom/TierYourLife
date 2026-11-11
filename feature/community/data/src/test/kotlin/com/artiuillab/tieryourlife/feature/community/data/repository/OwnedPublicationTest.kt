package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.ExpectedUser
import com.artiuillab.tieryourlife.core.network.ExpectedUserMismatch
import com.artiuillab.tieryourlife.core.network.IdTokenInterceptor
import com.artiuillab.tieryourlife.core.network.ProxyModule
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
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

class OwnedPublicationTest {
    private var owner: String? = "A"
    private var uploads = 0
    private var networkCalls = 0
    private var path = ""
    private var body = ""
    private var tag: ExpectedUser? = null
    private var afterUpload: () -> Unit = {}
    private var uploaded = setOf("photo")
    private val pictures = object : OwnPictures {
        override fun pictureIdOf(imageUrl: String?) = imageUrl?.takeIf { it.startsWith("file:") }?.substringAfterLast('/')
        override suspend fun sendNow(pictureIds: List<String>): Set<String> {
            uploads++
            afterUpload()
            return uploaded
        }
    }
    private val board = TierList(1, "My copy", listOf(Tier(1, "S", "#000000", "#FFFFFF", listOf(TierItem(1, "Card", "file:///photo")))), takenFrom = "original", originVersion = 123)

    private fun api(token: () -> String? = { jwt("A") }, answer: () -> String = { """{"id":"public"}""" }): CommunityApi {
        val client = OkHttpClient.Builder().addInterceptor(IdTokenInterceptor(token)).addInterceptor { chain ->
            networkCalls++
            path = chain.request().url.encodedPath
            body = Buffer().also { chain.request().body?.writeTo(it) }.readUtf8()
            tag = chain.request().tag(ExpectedUser::class.java)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(answer().toResponseBody("application/json".toMediaType())).build()
        }.build()
        return ProxyModule.proxyRetrofit("https://example.test/", client).create(CommunityApi::class.java)
    }

    @Test fun ownerMismatchRefusesBeforeAnyPrivateUploadOrNetworkRequest() = runBlocking {
        owner = "B"
        val failure = runCatching { ownedPublication(board, "A", { owner }, pictures, api()) }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals(0, uploads)
        assertEquals(0, networkCalls)
    }

    @Test fun ownerChangeDuringUploadRefusesBeforePublicPost() = runBlocking {
        afterUpload = { owner = "B" }
        val failure = runCatching { ownedPublication(board, "A", { owner }, pictures, api()) }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals(1, uploads)
        assertEquals(0, networkCalls)
    }

    @Test fun lateBTokenAfterTheOwnerCheckCannotPublishAsB() = runBlocking {
        val http = api(token = {
            owner = "B"
            jwt("B")
        })
        val failure = runCatching { ownedPublication(board, "A", { owner }, pictures, http) }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals(0, networkCalls)
    }

    @Test fun matchingActorPublishesWithLocalTagAndUnchangedPhotoVersionWire() = runBlocking {
        assertEquals("public", ownedPublication(board, "A", { owner }, pictures, api()).id)
        assertEquals(1, networkCalls)
        assertEquals("/lists", path)
        assertEquals(ExpectedUser("A"), tag)
        assertTrue(body.contains("\"pictureId\":\"photo\""))
        assertTrue(body.contains("\"sourceVersion\":123"))
        assertFalse(body.contains("expectedOwnerUid"))
        assertFalse(body.contains("ExpectedUser"))
    }

    @Test fun existingListReplacementUsesTheSameActorBoundEndpoint() = runBlocking {
        ownedPublication(board.copy(publishedId = "existing"), "A", { owner }, pictures, api())
        assertEquals("/lists/existing", path)
        assertEquals(ExpectedUser("A"), tag)
    }

    @Test fun retryReplacementCannotAcquireAnotherAccountsTokenAfterAnExistingBoardAnswer() = runBlocking {
        var tokens = 0
        val http = api(token = {
            tokens++
            jwt(if (tokens == 1) "A" else "B")
        }, answer = { """{"id":"existing","existing":true}""" })
        val failure = runCatching { ownedPublication(board, "A", { owner }, pictures, http) }.exceptionOrNull()
        assertTrue(failure is ExpectedUserMismatch)
        assertEquals("only A's first request reaches the endpoint; B's retry is rejected locally", 1, networkCalls)
    }

    @Test fun theExistingMissingCardPhotoRefusalStillPreventsPost() = runBlocking {
        uploaded = emptySet()
        val failure = runCatching { ownedPublication(board, "A", { owner }, pictures, api()) }.exceptionOrNull()
        assertEquals(PublishError.PictureRefused, (failure as PublishRefused).error)
        assertEquals(0, networkCalls)
    }

    private fun jwt(uid: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"user_id":"$uid","sub":"$uid"}""".toByteArray())
        return "header.$payload.signature"
    }
}
