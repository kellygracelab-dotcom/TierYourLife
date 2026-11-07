package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Four 100 px cards to a line; the tier holds five, so the fifth sits alone on the second line. */
class GridHoverTest {

    private val tierId = 7L
    private val itemIds = (1L..5L).toList()

    private fun controllerWith(onFirstLine: List<Long>, lifted: Long): TierDragController =
        TierDragController().apply {
            setValidTargets(listOf(tierId), itemIds)
            registerRowBounds(tierId, Rect(0f, 0f, 400f, 200f))
            onFirstLine.forEachIndexed { column, id ->
                val left = column * 100f
                registerTileBounds(tierId, id, itemIds.indexOf(id), Rect(left, 0f, left + 100f, 100f))
            }
            registerTileBounds(tierId, lifted, itemIds.indexOf(lifted), Rect(0f, 0f, 100f, 100f))
        }

    private fun payload(itemId: Long) = DragPayload(itemId, "card", null, tierId, 100.dp, 100.dp)

    @Test
    fun theLoneCardOfTheSecondLine_heldOverItsOwnPlace_staysAtTheEnd() {
        val controller = controllerWith(onFirstLine = listOf(1L, 2L, 3L, 4L), lifted = 5L)

        controller.beginDrag(payload(5L), Offset(50f, 150f))

        assertEquals(4, controller.hoverIndex)
    }

    @Test
    fun aFirstLineCard_heldWhereOnlyTheSlotIs_goesToTheEnd() {
        val controller = controllerWith(onFirstLine = listOf(2L, 3L, 4L, 5L), lifted = 1L)

        controller.beginDrag(payload(1L), Offset(50f, 150f))

        assertEquals(4, controller.hoverIndex)
    }

    @Test
    fun overTheFirstLine_theSlotStillFollowsTheColumns() {
        val controller = controllerWith(onFirstLine = listOf(1L, 2L, 3L, 4L), lifted = 5L)

        controller.beginDrag(payload(5L), Offset(120f, 50f))

        assertEquals(1, controller.hoverIndex)
    }
}
