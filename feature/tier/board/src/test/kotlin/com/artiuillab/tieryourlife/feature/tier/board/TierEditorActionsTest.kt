package com.artiuillab.tieryourlife.feature.tier.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TierEditorActionsTest {
    private val ids = listOf(1L, 2L, 3L)

    @Test
    fun aMiddleTier_movesOnePlaceEachWay() {
        var order: List<Long>? = null
        val actions = tierEditorActions(ids, tierId = 2L, onReorderTiers = { order = it }, onRemove = {})

        actions.onMoveUp!!.invoke()
        assertEquals(listOf(2L, 1L, 3L), order)
        actions.onMoveDown!!.invoke()
        assertEquals(listOf(1L, 3L, 2L), order)
    }

    @Test
    fun theEnds_haveNoMovePastThem() {
        assertNull(tierEditorActions(ids, tierId = 1L, onReorderTiers = {}, onRemove = {}).onMoveUp)
        assertNull(tierEditorActions(ids, tierId = 3L, onReorderTiers = {}, onRemove = {}).onMoveDown)
    }

    @Test
    fun removing_goesToTheGivenAction() {
        var removed = false
        tierEditorActions(ids, tierId = 2L, onReorderTiers = {}, onRemove = { removed = true }).onRemove()
        assertEquals(true, removed)
    }
}
