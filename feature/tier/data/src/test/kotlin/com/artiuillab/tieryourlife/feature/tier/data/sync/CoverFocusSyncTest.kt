package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.mapper.coverFocus
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.CoverFocusDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The part of the cover that shows travels with the board, as `coverFocus`; the centre goes as an explicit null. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CoverFocusSyncTest : SyncEngineBench() {

    private suspend fun cover(uid: String, picture: String?, focus: CoverFocus? = null) {
        val id = dao.boardByUid(uid)!!.id
        database.tierDao().setCover(id, picture, null)
        database.tierDao().setCoverFocus(id, focus?.x, focus?.y, focus?.zoom)
    }

    @Test
    fun aMovedCover_goesUpWithItsFocus() = runBlocking {
        val uid = givenLocalBoard("Films")
        cover(uid, "https://i/portrait.jpg", CoverFocus(0.5f, 0.15f, 2f))

        engine().sync()

        assertEquals(CoverFocusDto(0.5f, 0.15f, 2f), boards.kept.single().second.coverFocus)
    }

    @Test
    fun aCentredCover_saysSoWithANull_soTheAccountDropsAnOldFocus() = runBlocking {
        val uid = givenLocalBoard("Films")
        cover(uid, "https://i/portrait.jpg")

        engine().sync()

        val request = boards.kept.single().second
        val body = networkJson.parseToJsonElement(networkJson.encodeToString(KeepBoardRequestDto.serializer(), request)).jsonObject
        assertEquals(JsonNull, body["coverFocus"])
    }

    @Test
    fun aFocusFromTheAccount_comesDownWithTheBoard() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)
            .copy(coverImageUrl = "https://i/wide.jpg", coverFocus = CoverFocusDto(0.8f, 0.4f, 1.5f))

        engine().sync()

        assertEquals(CoverFocus(0.8f, 0.4f, 1.5f), dao.boardByUid("remote-1")!!.coverFocus())
    }

    @Test
    fun aFocusOutOfRange_orBesideNoCover_isNotKept() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)
            .copy(coverImageUrl = "https://i/wide.jpg", coverFocus = CoverFocusDto(1.7f, -1f, 9f))
        boards.server["remote-2"] = theirBoard("remote-2", title = "Books", revision = 2)
            .copy(coverFocus = CoverFocusDto(0.2f, 0.2f, 2f))

        engine().sync()

        assertEquals(CoverFocus(1f, 0f, 4f), dao.boardByUid("remote-1")!!.coverFocus())
        assertNull(dao.boardByUid("remote-2")!!.coverFocus())
    }

    @Test
    fun movingTheCover_changesTheFingerprint_soTheBoardSyncsAgain() = runBlocking {
        val uid = givenLocalBoard("Films")
        cover(uid, "https://i/portrait.jpg")
        val centred = BoardFingerprint.of(dao.boardByUid(uid)!!, emptyList(), emptyList()) { null }

        cover(uid, "https://i/portrait.jpg", CoverFocus(0.5f, 0.15f, 1f))
        val moved = BoardFingerprint.of(dao.boardByUid(uid)!!, emptyList(), emptyList()) { null }

        assertNotEquals(centred, moved)
    }

    @Test
    fun aNewPicture_startsFromTheCentre_theSamePictureKeepsItsFocus() = runBlocking {
        val uid = givenLocalBoard("Films")
        cover(uid, "https://i/portrait.jpg", CoverFocus(0.5f, 0.15f, 2f))
        val id = dao.boardByUid(uid)!!.id

        database.tierDao().setCover(id, "https://i/portrait.jpg", null)
        assertEquals(CoverFocus(0.5f, 0.15f, 2f), dao.boardByUid(uid)!!.coverFocus())

        database.tierDao().setCover(id, "https://i/other.jpg", null)
        assertNull(dao.boardByUid(uid)!!.coverFocus())
    }
}
