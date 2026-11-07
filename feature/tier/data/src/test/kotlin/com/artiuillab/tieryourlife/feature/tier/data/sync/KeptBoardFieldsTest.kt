package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The account stores a board as it is written, so whatever came down with it
 * has to go back up: the anonymous flag when it is set, and the saved game
 * untouched.
 */
class KeptBoardFieldsTest {

    private val game = """{"kind":"swiss","ids":["a","b","c"],"count":3,"seed":-17,"refine":false,""" +
        """"history":[[0,1,0],[1,2,null]],"round":2,"startedAt":1793146000000}"""

    private val incoming = """{"uid":"b-1","revision":4,"title":"Films","displayMode":"WRAP","anonymous":true,"play":$game}"""

    private fun sent(board: com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity): JsonObject {
        val request = KeepBoardRequestDto(
            fingerprint = "f",
            coverPictureId = null,
            title = board.title,
            displayMode = board.displayMode,
            anonymous = true.takeIf { board.anonymous },
            play = keptPlay(board.play, networkJson),
            tiers = emptyList(),
            items = emptyList(),
        )
        return networkJson.parseToJsonElement(networkJson.encodeToString(KeepBoardRequestDto.serializer(), request)).jsonObject
    }

    @Test
    fun `a game that came down goes back up exactly as it came`() {
        val board = networkJson.decodeFromString(KeptBoardDto.serializer(), incoming).toEntity(id = 1, uid = "b-1", pathFor = { it })

        val body = sent(board)

        assertEquals(networkJson.parseToJsonElement(game), body["play"])
        assertEquals(game, body["play"].toString())
    }

    @Test
    fun `an anonymous board says so, and keeps saying so`() {
        val board = networkJson.decodeFromString(KeptBoardDto.serializer(), incoming).toEntity(id = 1, uid = "b-1", pathFor = { it })

        assertEquals("true", sent(board)["anonymous"].toString())
    }

    @Test
    fun `a board that is neither sends neither field at all`() {
        val board = networkJson.decodeFromString(KeptBoardDto.serializer(), """{"uid":"b-2","revision":1,"title":"Books"}""")
            .toEntity(id = 2, uid = "b-2", pathFor = { it })

        val body = sent(board)

        assertFalse(board.anonymous)
        assertNull(board.play)
        assertFalse("anonymous" in body)
        assertFalse("play" in body)
    }

    @Test
    fun `a card's credit comes down with it, and a card without one sends no key`() {
        val board = networkJson.decodeFromString(
            KeptBoardDto.serializer(),
            """{"uid":"b-3","revision":1,"title":"Animals","items":[""" +
                """{"uid":"c-1","tierUid":"t-1","position":0,"title":"Red fox","credit":"© Jane Doe, CC BY"},""" +
                """{"uid":"c-2","tierUid":"t-1","position":1,"title":"Badger"}]}""",
        )

        assertEquals(listOf("© Jane Doe, CC BY", null), board.items.map { it.credit })
        val sent = board.items.map { networkJson.parseToJsonElement(networkJson.encodeToString(KeptItemDto.serializer(), it)).jsonObject }
        assertEquals(JsonPrimitive("© Jane Doe, CC BY"), sent[0]["credit"])
        assertFalse("credit" in sent[1])
    }

    @Test
    fun `a kept game that no longer reads is left out rather than refused`() {
        assertNull(keptPlay("{not json", networkJson))
        assertNull(keptPlay(null, networkJson))
    }
}
