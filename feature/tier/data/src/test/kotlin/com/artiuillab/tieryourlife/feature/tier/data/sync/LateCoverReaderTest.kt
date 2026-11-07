package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * QA A13, review of #257: a phone that took a board with a build that did not
 * know `coverPictureId` holds no cover and a record saying it agrees. Updated,
 * its next edit must not clear the cover in the account.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LateCoverReaderTest : SyncEngineBench() {

    private val updated = FakeAppPreferences(coverIdsLearned = false)

    /** Taken by the old reader, which read past the cover the account has. */
    private suspend fun takenByTheOldBuild(): String {
        boards.server[UID] = theirBoard(UID, title = "Games", revision = 4)
        engine(preferences = FakeAppPreferences()).sync()
        boards.server[UID] = boards.server.getValue(UID).copy(coverPictureId = "cover-1")
        return UID
    }

    private suspend fun renameTheCard(uid: String) {
        val card = dao.itemsOf(dao.boardByUid(uid)!!.id).single()
        dao.settleItemTitle(card.id, card.title, "Doom II")
    }

    private fun body(request: KeepBoardRequestDto): JsonObject =
        networkJson.parseToJsonElement(networkJson.encodeToString(KeepBoardRequestDto.serializer(), request)).jsonObject

    @Test
    fun aPhoneThatUpdatesLate_takesTheCover_andItsNextEditKeepsIt() = runBlocking {
        val uid = takenByTheOldBuild()
        val engine = engine(preferences = updated)

        engine.sync()
        renameTheCard(uid)
        engine.sync()

        assertEquals(images.pathFor("cover-1"), dao.boardByUid(uid)!!.coverImageUrl)
        assertTrue(updated.coverIdsLearned(someone))
        val sent = boards.kept.single().second
        assertEquals("Doom II", sent.items.single().title)
        assertEquals(JsonPrimitive("cover-1"), sent.coverPictureId)
    }

    @Test
    fun aBoardEditedBeforeThePass_keepsTheAccountsCoverToo() = runBlocking {
        val uid = takenByTheOldBuild()
        renameTheCard(uid)

        engine(preferences = updated).sync()

        assertEquals(images.pathFor("cover-1"), dao.boardByUid(uid)!!.coverImageUrl)
        val sent = boards.kept.single().second
        assertEquals("Doom II", sent.items.single().title)
        assertEquals(JsonPrimitive("cover-1"), sent.coverPictureId)
    }

    @Test
    fun aPassCutShort_sendsNoNullOverTheCover() = runBlocking {
        val uid = takenByTheOldBuild()
        renameTheCard(uid)
        boards.onBoard = { throw IOException("offline") }

        engine(preferences = updated).sync()

        assertFalse(updated.coverIdsLearned(someone))
        assertFalse("coverPictureId" in body(boards.kept.single().second))
    }

    @Test
    fun onceLearned_removingAnOwnCover_clearsItInTheAccount() = runBlocking {
        val uid = givenLocalBoard("Films")
        database.tierDao().setCover(dao.boardByUid(uid)!!.id, images.write("cover-1", byteArrayOf(1, 2, 3)), null)
        val engine = engine(preferences = updated)
        engine.sync()
        assertEquals(JsonPrimitive("cover-1"), boards.kept.last().second.coverPictureId)

        database.tierDao().setCover(dao.boardByUid(uid)!!.id, null, null)
        engine.sync()

        assertEquals(JsonNull, body(boards.kept.last().second)["coverPictureId"])
    }

    private companion object {
        const val UID = "remote-1"
    }
}
