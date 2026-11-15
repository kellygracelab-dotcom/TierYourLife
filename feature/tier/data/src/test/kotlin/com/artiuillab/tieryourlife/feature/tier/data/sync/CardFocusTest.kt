package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.mapper.focus
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.CoverFocusDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Which part of a card's picture shows: kept with the card, gone with its picture, handed to the collage on the board's tile and carried by sync as `pictureFocus`. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CardFocusTest : SyncEngineBench() {

    private var lastUid = ""

    private suspend fun boardWith(vararg pictures: String?): Long {
        val board = TierListEntity(title = "Fruit", ownerUid = someone)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = pictures.mapIndexed { index, picture -> TierItemEntity(tierId = 0, position = index, title = "Card $index", imageUrl = picture) },
                ),
            ),
        )
        lastUid = board.uid
        return dao.boardByUid(board.uid)!!.id
    }

    private suspend fun cards(boardId: Long) = dao.itemsOf(boardId).sortedBy { it.position }

    @Test
    fun aMovedPicture_isKeptWithTheCard() = runBlocking {
        val id = boardWith("https://i/mango.jpg")
        val card = cards(id).single()

        database.tierDao().setItemFocus(card.id, 0.4f, 0.2f, 1.5f)

        assertEquals(CoverFocus(0.4f, 0.2f, 1.5f), cards(id).single().focus())
    }

    @Test
    fun aNewPicture_startsFromItsDefault() = runBlocking {
        val id = boardWith("https://i/mango.jpg")
        val card = cards(id).single()
        database.tierDao().setItemFocus(card.id, 0.4f, 0.2f, 1.5f)

        database.tierDao().updateTierItemImage(card.id, "/files/tier_images/photo-1")

        assertNull(cards(id).single().focus())
    }

    @Test
    fun aCardWithoutAPicture_takesNoFocus() = runBlocking {
        val id = boardWith(null)
        val card = cards(id).single()

        database.tierDao().setItemFocus(card.id, 0.4f, 0.2f, 1.5f)

        assertNull(cards(id).single().focusX)
    }

    @Test
    fun theBoardsTile_knowsWhereEachMovedPictureIsCut() = runBlocking {
        val id = boardWith("https://i/mango.jpg", "https://i/kiwi.jpg")
        val (mango, _) = cards(id)
        database.tierDao().setItemFocus(mango.id, 0.4f, 0.2f, 1.5f)

        val rows = database.tierDao().getSummaryRows(someone, previewLimit = 6)

        assertEquals(mapOf("https://i/mango.jpg" to CoverFocus(0.4f, 0.2f, 1.5f)), rows.focuses[id])
    }

    @Test
    fun aMovedCard_goesUpWithItsFocus_andTheRestSayNull() = runBlocking {
        val id = boardWith("https://i/mango.jpg", "https://i/kiwi.jpg")
        database.tierDao().setItemFocus(cards(id).first().id, 0.4f, 0.2f, 1.5f)

        engine().sync()

        val request = boards.kept.single().second
        assertEquals(CoverFocusDto(0.4f, 0.2f, 1.5f), request.items.single { it.imageUrl == "https://i/mango.jpg" }.pictureFocus)
        val body = networkJson.encodeToJsonElement(KeepBoardRequestDto.serializer(), request).jsonObject
        val kiwi = body.getValue("items").jsonArray.map { it.jsonObject }.single { it["imageUrl"] == JsonPrimitive("https://i/kiwi.jpg") }
        assertEquals("left out, the account would keep an old focus", JsonNull, kiwi["pictureFocus"])
    }

    @Test
    fun aCardsFocusFromTheAccount_comesDown_inRange_andOnlyWithAPicture() = runBlocking {
        val board = theirBoard("remote-1", title = "Games", revision = 4)
        boards.server["remote-1"] = board.copy(
            items = listOf(
                KeptItemDto(uid = "a", tierUid = "remote-1-tier", position = 0, title = "A", imageUrl = "https://i/a.jpg", pictureFocus = CoverFocusDto(0.2f, 0.3f, 1.5f)),
                KeptItemDto(uid = "b", tierUid = "remote-1-tier", position = 1, title = "B", imageUrl = "https://i/b.jpg", pictureFocus = CoverFocusDto(1.7f, -1f, 9f)),
                KeptItemDto(uid = "c", tierUid = "remote-1-tier", position = 2, title = "C", pictureFocus = CoverFocusDto(0.2f, 0.3f, 1.5f)),
            ),
        )

        engine().sync()

        val here = cards(dao.boardByUid("remote-1")!!.id)
        assertEquals(listOf(CoverFocus(0.2f, 0.3f, 1.5f), CoverFocus(1f, 0f, 4f), null), here.map { it.focus() })
        assertNull(here.last().focusX)
    }

    @Test
    fun movingACard_changesTheFingerprint_soTheBoardSyncsAgain() = runBlocking {
        val id = boardWith("https://i/mango.jpg")
        val uid = lastUid
        val before = fingerprintOf(uid)

        database.tierDao().setItemFocus(cards(id).single().id, 0.5f, 0.5f, 1f)

        assertNotEquals("a centre chosen on purpose counts too", before, fingerprintOf(uid))
    }
}
