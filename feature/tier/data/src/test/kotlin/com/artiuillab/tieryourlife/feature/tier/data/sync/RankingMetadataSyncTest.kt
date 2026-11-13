package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptOriginDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RankingMetadataSyncTest : SyncEngineBench() {
    @Test fun anUnreadStateMarkerRestoresProvenanceOnceAndSendsItBackAfterAnEdit() = runBlocking {
        val uid = givenLocalBoard("Films")
        val engine = engine()
        engine.sync()
        boards.kept.clear()
        val incoming = theirBoard(uid, "Films", 1, "state-v1:retained")
        boards.server[uid] = incoming.copy(
            origin = KeptOriginDto("original", 123), takenFrom = "original", rankingChoice = "private",
            items = incoming.items.map { it.copy(sourceIndex = 1) },
        )
        var reads = 0
        boards.onBoard = { reads++ }
        assertEquals(0, engine.sync().refused)
        val restored = dao.boardByUid(uid)!!
        assertEquals("original", restored.takenFrom)
        assertEquals(123L, restored.originVersion)
        assertEquals("private", restored.rankingChoice)
        assertEquals(1, dao.itemsOf(restored.id).single().sourceIndex)
        val firstReads = reads
        engine.sync()
        assertEquals(firstReads, reads)
        assertTrue(boards.kept.isEmpty())
        dao.renameBoard(uid, "My renamed private board")
        engine.sync()
        val sent = boards.kept.single().second
        assertEquals(KeptOriginDto("original", 123), sent.origin)
        assertEquals("private", sent.rankingChoice)
        assertEquals(1, sent.items.single().sourceIndex)
    }

    @Test fun anOrdinaryBoardOmitsEmptyMetadata() = runBlocking {
        givenLocalBoard("Ordinary")
        engine().sync()
        val wire = networkJson.encodeToString(KeepBoardRequestDto.serializer(), boards.kept.single().second)
        val json = networkJson.parseToJsonElement(wire).jsonObject
        assertFalse(json.containsKey("origin"))
        assertFalse(json.containsKey("takenFrom"))
        assertFalse(json.containsKey("rankingChoice"))
    }
}
