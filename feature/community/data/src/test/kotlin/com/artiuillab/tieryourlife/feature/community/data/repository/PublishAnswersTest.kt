package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** What the proxy's answers to a publish mean for the board that sent it. */
class PublishAnswersTest {

    private val request = PublishListRequestDto(title = "Films", category = "film_tv", tiers = emptyList(), items = emptyList(), anonymous = true)

    private class Calls {
        val sent = mutableListOf<String>()
        val bodies = mutableListOf<PublishListRequestDto>()
        var publishAnswer = PublishedIdDto(id = "p1")

        suspend fun publish(request: PublishListRequestDto): PublishedIdDto {
            sent += "POST /lists"
            bodies += request
            return publishAnswer
        }

        suspend fun republish(id: String, request: PublishListRequestDto): PublishedIdDto {
            sent += "POST /lists/$id"
            bodies += request
            return PublishedIdDto(id = id, topicId = "t-now")
        }
    }

    // A lost answer, then a retry: the server names the list it made and writes nothing, so the board goes over it.
    @Test
    fun `an existing list is sent the board again before the board counts as published`() = runBlocking {
        val calls = Calls().apply { publishAnswer = PublishedIdDto(id = "p7", topicId = "t-old", existing = true) }

        val answer = sendOut(publishedId = null, request = request, publish = calls::publish, republish = calls::republish)

        assertEquals(listOf("POST /lists", "POST /lists/p7"), calls.sent)
        assertEquals(listOf(request, request), calls.bodies)
        assertEquals("p7", answer.id)
        assertEquals("t-now", answer.topicId)
        assertFalse(answer.existing)
    }

    @Test
    fun `a new list is published once`() = runBlocking {
        val calls = Calls()

        val answer = sendOut(publishedId = null, request = request, publish = calls::publish, republish = calls::republish)

        assertEquals(listOf("POST /lists"), calls.sent)
        assertEquals("p1", answer.id)
    }

    @Test
    fun `a board already out is sent over its own id`() = runBlocking {
        val calls = Calls()

        sendOut(publishedId = "p3", request = request, publish = calls::publish, republish = calls::republish)

        assertEquals(listOf("POST /lists/p3"), calls.sent)
    }

    // A signed-in person was told to sign in with Google.
    @Test
    fun `replacing another account's list is a list gone, not a sign-in`() {
        assertEquals(PublishError.ListGone, refusal(403, """{"error":"Not yours","code":"NOT_YOURS"}""").asPublishError())
        assertEquals(PublishError.NotSignedIn, refusal(403, """{"error":"Sign in","code":"NOT_SIGNED_IN"}""").asPublishError())
        assertEquals(PublishError.Banned(untilMillis = 5L), refusal(403, """{"code":"BANNED","until":5}""").asPublishError())
    }

    @Test
    fun `a list that is not there to replace is gone`() {
        assertEquals(PublishError.ListGone, refusal(404, """{"error":"No such list","code":"NOT_FOUND"}""").asPublishError())
    }

    @Test
    fun `a section that takes no lists is said, and other invalid bodies stay unknown`() {
        assertEquals(PublishError.SectionUnavailable, refusal(400, """{"error":"bad","code":"INVALID","detail":"category"}""").asPublishError())
        assertEquals(PublishError.TopicUnavailable, refusal(400, """{"code":"TOPIC_UNAVAILABLE"}""").asPublishError())
        assertEquals(PublishError.Unknown, refusal(400, """{"code":"INVALID","detail":"title"}""").asPublishError())
        assertEquals(PublishError.Offline, IOException("offline").asPublishError())
    }

    // Proxy #96 counts again inside the transaction, so parallel publishes past 50 get the same answer.
    @Test
    fun `the list limit is said, whichever check refused it`() {
        assertEquals(PublishError.TooManyLists, refusal(409, """{"error":"Too many published lists","code":"TOO_MANY_LISTS"}""").asPublishError())
        assertEquals(PublishError.TooManyPendingTopics, refusal(409, """{"code":"TOO_MANY_PENDING_TOPICS"}""").asPublishError())
    }

    @Test
    fun `a cursor that names a list taken down is told apart from other conflicts`() {
        assertTrue(refusal(409, """{"error":"That page is gone","code":"CURSOR_GONE"}""").isCursorGone())
        assertFalse(refusal(409, """{"code":"TOO_MANY_FOLLOWING"}""").isCursorGone())
        assertFalse(refusal(404, """{"code":"CURSOR_GONE"}""").isCursorGone())
        assertFalse(IOException("offline").isCursorGone())
    }

    // A moderator moved the list: the answer's topic is not the board's, and the board learns it.
    @Test
    fun `a topic other than the board's is learned, the board's own is not asked about again`() {
        val board = TierList(id = 1, title = "Films", tiers = emptyList(), topicId = "t-old", topicName = "Old")

        assertEquals("t-new", board.topicToLearn(PublishedIdDto(id = "p", topicId = "t-new")))
        assertNull(board.topicToLearn(PublishedIdDto(id = "p", topicId = "t-old")))
        assertNull(board.topicToLearn(PublishedIdDto(id = "p")))
        assertEquals("t-mod", board.copy(topicId = null, topicName = null).topicToLearn(PublishedIdDto(id = "p", topicId = "t-mod")))
    }

    @Test
    fun `a new name that became a topic and a ranking learn nothing`() {
        val board = TierList(id = 1, title = "Films", tiers = emptyList(), newTopicName = "Horror")

        assertNull(board.topicToLearn(PublishedIdDto(id = "p", topicId = "t-horror")))
        assertNull(board.copy(newTopicName = null).topicToLearn(PublishedIdDto(id = "p", topicId = "t", ranking = true)))
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType())))
}
