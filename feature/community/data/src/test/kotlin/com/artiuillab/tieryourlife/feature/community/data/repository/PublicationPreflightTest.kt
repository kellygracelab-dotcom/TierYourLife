package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.ProxyModule
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.rankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class PublicationPreflightTest {
    private var sentPath = ""
    private var sentBody = ""
    private val pictures = object : OwnPictures {
        override fun pictureIdOf(imageUrl: String?): String? = imageUrl?.takeIf { it.startsWith("file:") }?.substringAfterLast('/')
        override suspend fun sendNow(pictureIds: List<String>): Set<String> = error("Preflight must never upload")
    }

    private fun api(code: Int, answer: String): CommunityApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            sentPath = chain.request().url.encodedPath
            sentBody = Buffer().also { chain.request().body?.writeTo(it) }.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("answer")
                .body(answer.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return ProxyModule.proxyRetrofit("https://example.test/", client).create(CommunityApi::class.java)
    }

    @Test fun preflightKeepsPositionalBindingsAndOwnPhotoNamesWithoutAnUpload() = runBlocking {
        val board = TierList(1, "Films", listOf(Tier(1, "S", "#111111", "#EEEEEE", listOf(TierItem(8, "A", "file:///photo", sourceIndex = 1)))), takenFrom = "original", originVersion = 123)
        assertEquals(PublicationKind.Ranking("original", 123), publicationPreflight(board, api(200, """{"kind":"ranking","listId":"original","version":123}"""), pictures))
        assertEquals("/lists/classify", sentPath)
        val body = networkJson.parseToJsonElement(sentBody).jsonObject
        assertEquals("123", body.getValue("sourceVersion").jsonPrimitive.content)
        val card = body.getValue("items").jsonArray.single().jsonObject
        assertEquals("1", card.getValue("sourceIndex").jsonPrimitive.content)
        assertEquals("photo", card.getValue("pictureId").jsonPrimitive.content)
    }

    @Test fun publicArrayPositionsDistinguishDuplicateCards() {
        val dto = PublishedListDto("original", "Films", authorName = "A", items = listOf(PublishedItemDto("Same"), PublishedItemDto("Same")))
        assertEquals(listOf(0, 1), dto.toDomain().items.map { it.sourceIndex })
    }

    @Test fun aNewAttributedListAlsoPinsTheSourceSnapshotBehindTheConfirmation() = runBlocking {
        val legacy = TierList(1, "Changed title", emptyList(), takenFrom = "original")
        val result = publicationPreflight(legacy, api(200, """{"kind":"list","basedOn":{"id":"original","title":"Original","authorName":"Author"},"version":456}"""), pictures)
        assertEquals(456L, (result as PublicationKind.NewList).sourceVersion)
        assertEquals("original", result.basedOn?.id)
    }

    @Test fun aChangedSourceHasItsOwnPublicationAndRankingRefusals() {
        val failure = HttpException(retrofit2.Response.error<Unit>(409, """{"code":"LIST_CHANGED"}""".toResponseBody("application/json".toMediaType())))
        assertEquals(PublishError.SourceChanged, failure.asPublishError())
        val ranking = HttpException(retrofit2.Response.error<Unit>(409, """{"code":"LIST_CHANGED"}""".toResponseBody("application/json".toMediaType())))
        assertEquals(RankingRefusal.ListChanged, ranking.rankingRefusal())
        assertTrue(failure.asPublishError() != PublishError.TooManyLists)
    }
}
