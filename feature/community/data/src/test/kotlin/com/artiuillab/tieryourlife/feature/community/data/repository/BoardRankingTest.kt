package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** SPEC list-flow К3: a saved copy as its reader's ranking, the cards they added at N+k. */
class BoardRankingTest {

    private fun pictures(uploaded: Set<String>) = object : OwnPictures {
        override fun pictureIdOf(imageUrl: String?) = imageUrl?.takeIf { it.startsWith("file:") }?.substringAfterLast('/')
        override suspend fun sendNow(pictureIds: List<String>) = uploaded
    }

    private fun card(id: Long, source: Int?, url: String? = null) = TierItem(id, "Card $id", url, sourceIndex = source)

    // A list of three cards in two tiers; the copy added two of its own.
    private val board = TierList(
        1, "My copy",
        listOf(
            Tier(1, "Love", "#111111", "#EEEEEE", listOf(card(10, null, "file:///mine"), card(2, 2))),
            Tier(2, "Meh", "#111111", "#EEEEEE", listOf(card(0, 0), card(11, null, "https://x/web.jpg"))),
            Tier(3, "Pool", "#111111", "#EEEEEE", listOf(card(1, 1)), isPool = true),
        ),
        takenFrom = "list", originVersion = 7,
    )

    @Test
    fun theListsCardsKeepTheirPlace_andAddedOnesFollowAtNPlusK() = runBlocking {
        val request = board.rankingRequest(listCards = 3, listTiers = 2, version = 7, images = pictures(setOf("mine")))

        assertEquals(listOf(listOf(3, 2), listOf(0, 4)), request.rows)
        assertEquals(listOf(1, null, 0), request.arrangement)
        assertEquals(listOf("Love", "Meh"), request.tierLabels)
        assertEquals(7L, request.version)
        assertEquals("mine", request.addedCards!![0].pictureId)
        assertNull(request.addedCards!![0].imageUrl)
        assertEquals("https://x/web.jpg", request.addedCards!![1].imageUrl)
    }

    @Test
    fun withNothingAdded_noAddedCardsTravel() = runBlocking {
        val plain = board.copy(tiers = board.tiers.map { tier -> tier.copy(items = tier.items.filter { it.sourceIndex != null }) })
        assertNull(plain.rankingRequest(3, 2, 7, pictures(emptySet())).addedCards)
    }

    @Test
    fun aPhotoThatWouldNotUpload_refusesRatherThanGoWithoutIt() = runBlocking {
        val refusal = runCatching { board.rankingRequest(3, 2, 7, pictures(emptySet())) }.exceptionOrNull()
        assertEquals(PublishError.PictureRefused, (refusal as PublishRefused).error)
    }

    @Test
    fun tiersThatNoLongerMatchTheList_cannotGoAsItsRanking() = runBlocking {
        val refusal = runCatching { board.rankingRequest(3, 3, 7, pictures(setOf("mine"))) }.exceptionOrNull()
        assertEquals(PublishError.RankInstead, (refusal as PublishRefused).error)
    }

    @Test
    fun theServersRefusals_inTheBoardsWords() {
        fun refused(code: Int, body: String) = HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
        assertEquals(PublishError.SourceChanged, refused(409, """{"code":"LIST_CHANGED"}""").boardRankingError())
        assertEquals(PublishError.PictureRefused, refused(422, """{"code":"PICTURE_REFUSED","because":"x","addedIndex":0}""").boardRankingError())
        assertEquals(PublishError.ListGone, refused(404, """{"code":"NOT_FOUND"}""").boardRankingError())
    }
}
