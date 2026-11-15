package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.mapper.focus
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Which part of a card's picture shows: kept with the card, gone with its picture, and handed to the collage on the board's tile. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CardFocusTest : SyncEngineBench() {

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
}
