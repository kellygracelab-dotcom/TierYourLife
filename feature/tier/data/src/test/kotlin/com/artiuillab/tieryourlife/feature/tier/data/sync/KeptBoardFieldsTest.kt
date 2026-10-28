package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import kotlinx.serialization.json.JsonObject
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
        val board = networkJson.decodeFromString(KeptBoardDto.serializer(), incoming).toEntity(id = 1, uid = "b-1")

        val body = sent(board)

        assertEquals(networkJson.parseToJsonElement(game), body["play"])
        assertEquals(game, body["play"].toString())
    }

    @Test
    fun `an anonymous board says so, and keeps saying so`() {
        val board = networkJson.decodeFromString(KeptBoardDto.serializer(), incoming).toEntity(id = 1, uid = "b-1")

        assertEquals("true", sent(board)["anonymous"].toString())
    }

    @Test
    fun `a board that is neither sends neither field at all`() {
        val board = networkJson.decodeFromString(KeptBoardDto.serializer(), """{"uid":"b-2","revision":1,"title":"Books"}""")
            .toEntity(id = 2, uid = "b-2")

        val body = sent(board)

        assertFalse(board.anonymous)
        assertNull(board.play)
        assertFalse("anonymous" in body)
        assertFalse("play" in body)
    }

    @Test
    fun `a kept game that no longer reads is left out rather than refused`() {
        assertNull(keptPlay("{not json", networkJson))
        assertNull(keptPlay(null, networkJson))
    }
}
