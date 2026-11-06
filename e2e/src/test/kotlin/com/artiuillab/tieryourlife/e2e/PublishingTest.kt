package com.artiuillab.tieryourlife.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.domain.model.ListGone
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PublishingTest {

    private val phones = mutableListOf<Phone>()

    private fun phone(identity: Identity = Emulators.newAccount()) = Phone(identity).also { phones += it }

    @After
    fun closePhones() = phones.forEach(Phone::close)

    @Test
    fun aSignedInAccount_publishesABoard_andTheBoardIsLinked() = runBlocking<Unit> {
        val phone = phone()
        val id = phone.newBoard("Films of the eighties")

        val published = phone.publish(id)

        assertEquals(listOf(201), phone.answersTo("POST", "/lists").map { it.status })
        assertEquals(published.id, phone.board(id).publishedId)
        val opened = phone.community.open(published.id).getOrThrow()
        assertEquals("Films of the eighties", opened.summary.title)
        assertEquals(listOf("Alien", "Heat", "Ronin"), opened.items.map { it.title }.sorted())
        assertTrue(opened.summary.mine)

        phone.sync.sync()

        assertEquals(published.id, phone.boardsApi.board(phone.board(id).uid).publishedId)
    }

    @Test
    fun theSameBoardPublishedAgain_namesTheListItAlreadyHas() = runBlocking<Unit> {
        val phone = phone()
        val id = phone.newBoard("Games")
        val first = phone.publish(id)
        // As if the first answer had never arrived: the board does not know its list.
        phone.boards.setPublished(id, null, null)

        val second = phone.publish(id)

        val again = phone.answersTo("POST", "/lists").last()
        assertEquals(200, again.status)
        assertEquals("true", networkJson.parseToJsonElement(again.body).jsonObject["existing"]?.jsonPrimitive?.content)
        assertEquals(first.id, second.id)
        val mine = phone.community.myPublished().getOrThrow()
        assertEquals(listOf(first.id), mine.map { it.id })
        assertEquals(phone.board(id).uid, mine.single().boardUid)
    }

    @Test
    fun aBoardThatLostItsPublishAnswer_findsItsListOnTheNextSync() = runBlocking<Unit> {
        val phone = phone()
        val id = phone.newBoard("Albums")
        val published = phone.publish(id)
        phone.boards.setPublished(id, null, null)

        phone.sync.sync()

        assertEquals(published.id, phone.board(id).publishedId)
    }

    @Test
    fun aGuest_isAskedToSignInFirst() = runBlocking<Unit> {
        val phone = phone(Emulators.guest())
        val id = phone.newBoard("Films")

        val refused = phone.community.publish(phone.board(id)).exceptionOrNull()

        assertEquals(PublishError.NotSignedIn, (refused as PublishRefused).error)
        assertEquals(listOf(403), phone.answersTo("POST", "/lists").map { it.status })
    }

    @Test
    fun takingTheTopicOff_leavesTheListWithout() = runBlocking<Unit> {
        val topicId = approvedTopic(ListCategory.Anime.id)
        val phone = phone()
        val id = phone.newBoard("Ghibli films", category = ListCategory.Anime.id)
        phone.boards.setTopic(id, topicId, "Ghibli", null)
        val published = phone.publish(id)
        assertEquals(topicId, published.topicId)

        phone.boards.setTopic(id, null, null, null)
        val republished = phone.publish(id)

        assertEquals(published.id, republished.id)
        assertEquals(listOf(200), phone.answersTo("POST", "/lists/${published.id}").map { it.status })
        assertNull(republished.topicId)
        assertNull(phone.community.open(published.id).getOrThrow().summary.topicId)
    }

    @Test
    fun anUnpublishedList_isGone_andTheBoardIsPrivate() = runBlocking<Unit> {
        val phone = phone()
        val id = phone.newBoard("Books")
        val published = phone.publish(id)

        // What the board screen's switch does on the way back.
        phone.community.unpublish(published.id).getOrThrow()
        phone.boards.setPublished(id, null, null)

        assertEquals(listOf(204), phone.answersTo("DELETE", "/lists/${published.id}").map { it.status })
        assertTrue(phone.community.open(published.id).exceptionOrNull() is ListGone)
        assertTrue(phone.community.myPublished().getOrThrow().isEmpty())
        phone.sync.sync()
        assertNull(phone.board(id).publishedId)
    }

    @Test
    fun aListTakenDown_leavesTheBoardPrivateOnEveryPhone() = runBlocking<Unit> {
        val account = Emulators.newAccount()
        val first = phone(account)
        val second = phone(Emulators.signIn(account))
        val id = first.newBoard("Dishes", category = ListCategory.Food.id)
        val published = first.publish(id)
        first.sync.sync()
        second.sync.sync()
        val uid = first.board(id).uid
        assertEquals(published.id, second.boardByUid(uid)?.publishedId)

        phone(Emulators.owner()).community.takeDown(published.id, ban = null).getOrThrow()
        first.sync.sync()
        second.sync.sync()

        assertNull(first.board(id).publishedId)
        assertNull(second.boardByUid(uid)?.publishedId)
    }

    private fun approvedTopic(section: String): String {
        val owner = Emulators.owner()
        // The proxy makes the built-in sections on their first read.
        Emulators.call(owner, "GET", "lists/sections")
        val (status, body) = Emulators.call(
            owner,
            "POST",
            "lists/topics",
            buildJsonObject {
                put("name", "E2E ${UUID.randomUUID().toString().take(8)}")
                put("section", section)
            },
        )
        check(status == 201) { "The owner could not make a topic ($status): $body" }
        return networkJson.parseToJsonElement(body).jsonObject.getValue("id").jsonPrimitive.content
    }
}
