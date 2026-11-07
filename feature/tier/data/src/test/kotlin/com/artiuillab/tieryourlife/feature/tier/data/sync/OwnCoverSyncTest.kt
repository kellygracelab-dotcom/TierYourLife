package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** QA A13: a cover of one's own photo is kept in the account by its id, as a card's picture is. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OwnCoverSyncTest : SyncEngineBench() {

    private class Recording(private val held: Map<String, ByteArray> = emptyMap()) : Pictures {
        val sent = mutableListOf<String>()

        override suspend fun put(pictureId: String, bytes: ByteArray): Boolean {
            sent += pictureId
            return true
        }

        override suspend fun get(pictureId: String): ByteArray? = held[pictureId]
    }

    private suspend fun coverOf(uid: String, cover: String?) {
        database.tierDao().setCover(dao.boardByUid(uid)!!.id, cover, null)
    }

    private fun body(request: KeepBoardRequestDto): JsonObject =
        networkJson.parseToJsonElement(networkJson.encodeToString(KeepBoardRequestDto.serializer(), request)).jsonObject

    @Test
    fun aCoverOfOnesOwnPhoto_goesUp_andTheBoardNamesItById() = runBlocking {
        val uid = givenLocalBoard("Films")
        coverOf(uid, images.write("cover-1", byteArrayOf(1, 2, 3)))
        val vault = Recording()

        engine(vault = vault, connection = Unmetered).sync()

        val request = boards.kept.single().second
        assertEquals("cover-1", request.coverPictureId)
        assertNull(request.coverImageUrl)
        assertTrue("cover-1" in vault.sent)
    }

    @Test
    fun aWebCoverOrNone_sendsTheCoverPictureAsNull() = runBlocking {
        val web = givenLocalBoard("Films")
        coverOf(web, "https://i/cover.jpg")
        val none = givenLocalBoard("Books")

        engine().sync()

        val sent = boards.kept.toMap()
        val webBody = body(sent.getValue(web))
        assertEquals(JsonNull, webBody["coverPictureId"])
        assertEquals(JsonPrimitive("https://i/cover.jpg"), webBody["coverImageUrl"])
        assertEquals(JsonNull, body(sent.getValue(none))["coverPictureId"])
    }

    @Test
    fun aBoardArrivingWithACoverPicture_fetchesIt_andIsNotSentBack() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4).copy(coverPictureId = "cover-1")
        val vault = Recording(held = mapOf("cover-1" to byteArrayOf(1, 2, 3)))
        val engine = engine(vault = vault, connection = Unmetered)

        engine.sync()
        val second = engine.sync()

        assertEquals(images.pathFor("cover-1"), dao.boardByUid("remote-1")!!.coverImageUrl)
        assertTrue(images.holds("cover-1"))
        assertEquals(0, second.carried)
        assertTrue(boards.kept.isEmpty())
        assertFalse("cover-1" in vault.sent)
    }

    @Test
    fun takingTheAccountsVersion_keepsTheCoverPicture() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4).copy(coverPictureId = "cover-1")
        val engine = engine()
        engine.sync()
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games, renamed on the site", revision = 5, fingerprint = "theirs-5")
            .copy(coverPictureId = "cover-1")

        engine.sync()

        val board = dao.boardByUid("remote-1")!!
        assertEquals("Games, renamed on the site", board.title)
        assertEquals(images.pathFor("cover-1"), board.coverImageUrl)
    }

    @Test
    fun aVersionWithNoCoverAtAll_clearsIt() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4).copy(coverPictureId = "cover-1")
        val engine = engine()
        engine.sync()
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 5, fingerprint = "theirs-5")

        engine.sync()

        assertNull(dao.boardByUid("remote-1")!!.coverImageUrl)
    }
}
