package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreeWayMergeTest {

    private val merge = ThreeWayMerge { null }
    private val board = TierListEntity(id = 7, title = "Films", uid = "board")
    private val tiers = listOf(tier(1, "s", 0, "S"), tier(2, "a", 1, "A"))
    private val base = side(card("alien", 1, 0), card("doom", 1, 1), card("quake", 2, 0))
    private val agreed = merge.agreedOf("agreed", base)

    @Test
    fun aCardRemovedThere_andUntouchedHere_goes() {
        val merged = merge.merge(agreed, base, side(card("alien", 1, 0), card("quake", 2, 0)))!!

        assertEquals(mapOf("s" to listOf("alien"), "a" to listOf("quake")), merged.placed())
    }

    @Test
    fun aCardRemovedThere_butEditedHere_keepsBoth() {
        val ours = side(card("alien", 1, 0), card("doom", 1, 1, title = "Doom II"), card("quake", 2, 0))

        assertNull(merge.merge(agreed, ours, side(card("alien", 1, 0), card("quake", 2, 0))))
    }

    @Test
    fun cardsAddedOnBothSides_allStay_eachWhereItWasAdded() {
        val ours = side(card("alien", 1, 0), card("hl", 1, 1), card("doom", 1, 2), card("quake", 2, 0))
        val theirs = side(card("alien", 1, 0), card("doom", 1, 1), card("tetris", 1, 2), card("quake", 2, 0))

        val merged = merge.merge(agreed, ours, theirs)!!

        assertEquals(mapOf("s" to listOf("alien", "hl", "doom", "tetris"), "a" to listOf("quake")), merged.placed())
    }

    @Test
    fun aCardMovedThere_andRenamedHere_isMovedAndRenamed() {
        val ours = side(card("alien", 1, 0), card("doom", 1, 1, title = "Doom II"), card("quake", 2, 0))
        val theirs = side(card("alien", 1, 0), card("quake", 2, 0), card("doom", 2, 1))

        val merged = merge.merge(agreed, ours, theirs)!!

        assertEquals(mapOf("s" to listOf("alien"), "a" to listOf("quake", "doom")), merged.placed())
        assertEquals("Doom II", merged.tiers.flatMap { it.items }.single { it.uid == "doom" }.title)
    }

    @Test
    fun theTitleChangedThere_andATierRenamedHere_areBoth() {
        val ours = BoardSide(board, listOf(tier(1, "s", 0, "Best"), tiers[1]), base.items)
        val theirs = BoardSide(board.copy(title = "Films I've seen"), tiers, base.items)

        val merged = merge.merge(agreed, ours, theirs)!!

        assertEquals("Films I've seen", merged.board.title)
        assertEquals(listOf("Best", "A"), merged.tiers.map { it.tier.label })
    }

    @Test
    fun theSameFieldChangedOnBothSides_keepsBoth() {
        assertNull(merge.merge(agreed, BoardSide(board.copy(title = "Mine"), tiers, base.items), BoardSide(board.copy(title = "Theirs"), tiers, base.items)))
    }

    private fun MergedBoard.placed() = tiers.associate { it.tier.uid to it.items.map { item -> item.uid } }

    private fun side(vararg items: TierItemEntity) = BoardSide(board, tiers, items.toList())

    private fun tier(id: Long, uid: String, position: Int, label: String) =
        TierEntity(id = id, tierListId = 7, position = position, label = label, colorLight = "#111111", colorDark = "#EEEEEE", uid = uid)

    private fun card(uid: String, tierId: Long, position: Int, title: String = uid) =
        TierItemEntity(tierId = tierId, position = position, title = title, imageUrl = null, uid = uid)
}
