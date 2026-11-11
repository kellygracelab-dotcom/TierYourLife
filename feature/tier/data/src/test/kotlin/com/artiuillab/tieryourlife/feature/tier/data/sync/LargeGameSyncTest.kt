package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Room-backed recovery after a legacy client acknowledged a game it could not decode. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LargeGameSyncTest : SyncEngineBench() {
    @Test
    fun anAcknowledgedRevision_restoresItsUnreadGameOnce_thenExplicitlyClearsIt() = runBlocking {
        val uid = givenLocalBoard("Films")
        val engine = engine()
        engine.sync()
        boards.kept.clear()
        boards.server[uid] = largeBoard(uid)
        var downloads = 0
        boards.onBoard = { downloads++ }

        assertEquals(0, engine.sync().refused)
        val recovered = dao.boardByUid(uid)!!
        assertEquals(largeBoard(uid).play.toString(), recovered.play)
        assertEquals(91, dao.itemsOf(recovered.id).size)
        val record = dao.allSyncRecords().single()
        assertEquals("game-v1:retained", record.remoteFingerprint)
        assertEquals(fingerprintOf(uid), record.fingerprint)
        val firstDownloads = downloads
        engine.sync()
        assertEquals(firstDownloads, downloads)
        assertTrue(boards.kept.isEmpty())

        database.tierDao().setPlay(recovered.id, null)
        assertEquals(0, engine.sync().refused)
        val request = boards.kept.single().second
        assertEquals(JsonNull, request.play)
        val wire = networkJson.encodeToString(KeepBoardRequestDto.serializer(), request)
        assertTrue(networkJson.parseToJsonElement(wire).jsonObject.containsKey("play"))
        assertEquals(JsonNull, networkJson.parseToJsonElement(wire).jsonObject["play"])
    }

    @Test
    fun aLocalEdit_keepsBothAndRenamesAllGameReferencesInTheConflictCopy() = runBlocking {
        val uid = givenLocalBoard("Films")
        val engine = engine()
        engine.sync()
        dao.renameBoard(uid, "My edited title")
        boards.server[uid] = largeBoard(uid)

        assertEquals(0, engine.sync().refused)
        assertEquals("My edited title", dao.boardByUid(uid)!!.title)
        val copy = dao.allBoards().single { it.uid != uid }
        val game = networkJson.parseToJsonElement(copy.play!!).jsonObject
        val ids = game.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
        val actualIds = dao.itemsOf(copy.id).map { it.uid }.toSet()
        assertEquals(91, ids.size)
        assertEquals(91, ids.toSet().size)
        assertEquals(actualIds, ids.toSet())
        assertFalse(ids.any { it.startsWith("remote-card-") })
        assertEquals(dao.tiersOf(copy.id).map { it.uid }, game.getValue("scope").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(largeBoard(uid).play!!.jsonObject["history"], game["history"])
        assertNotNull(copy.play)
    }

    private fun largeBoard(uid: String) = theirBoard(uid, "Films", 1, "game-v1:retained").let { board ->
        board.copy(
            items = (0 until 91).map { KeptItemDto("remote-card-$it", tierUid = "$uid-tier", position = it, title = "Card $it") },
            play = JsonObject(
                mapOf(
                    "kind" to JsonPrimitive("swiss"), "count" to JsonPrimitive(91), "seed" to JsonPrimitive(123),
                    "refine" to JsonPrimitive(true), "round" to JsonPrimitive(2), "startedAt" to JsonPrimitive(1_800_000_000_000L),
                    "ids" to JsonArray((0 until 91).map { JsonPrimitive("remote-card-$it") }),
                    "history" to JsonArray((0 until 53).map { JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(1), JsonPrimitive(0))) }),
                    "scope" to JsonArray(listOf(JsonPrimitive("$uid-tier"))), "keepTier" to JsonPrimitive(true),
                ),
            ),
        )
    }
}
