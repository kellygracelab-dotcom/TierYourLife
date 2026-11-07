package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WrappedDropGridTest {
    private val ids = (1L..6L).toSet()
    private val grid = WrappedDropGrid(Rect(0f, 0f, 500f, 200f), 100, 100, 0, 5, ids)

    @Test
    fun aSixthCardOnItsOwnLine_hasOneStableLastPlace() {
        for (y in listOf(101f, 125f, 150f, 199f)) {
            for (x in listOf(1f, 49f, 80f, 250f, 499f)) {
                assertEquals(5, grid.insertionIndex(Offset(x, y), liftedId = 6, rightToLeft = false))
            }
        }
    }

    @Test
    fun movingUpAndDownAcrossTheLineBoundary_followsTheCellNotTheMovingCards() {
        assertEquals(2, grid.insertionIndex(Offset(210f, 90f), 6, false))
        assertEquals(5, grid.insertionIndex(Offset(210f, 110f), 6, false))
        assertEquals(2, grid.insertionIndex(Offset(210f, 90f), 6, false))
        assertEquals(0, grid.insertionIndex(Offset(10f, 50f), 6, false))
    }

    @Test
    fun theBottomHalfOfALine_stillBelongsToThatLine() {
        assertEquals(1, grid.insertionIndex(Offset(110f, 75f), 6, false))
    }

    @Test
    fun arabicMirrorsColumnsAndKeepsTheSecondLine() {
        assertEquals(0, grid.insertionIndex(Offset(490f, 75f), 6, true))
        assertEquals(2, grid.insertionIndex(Offset(290f, 75f), 6, true))
        assertEquals(5, grid.insertionIndex(Offset(490f, 150f), 6, true))
    }

    @Test
    fun aCardFromAnotherTier_canGoAfterEveryCard() {
        assertEquals(6, grid.insertionIndex(Offset(110f, 150f), 99, false))
    }

    @Test
    fun scrollingMovesTheGridOriginWithoutChangingTheSlotUnderTheFinger() {
        val scrolled = grid.copy(bounds = Rect(0f, -100f, 500f, 100f))
        assertEquals(5, scrolled.insertionIndex(Offset(10f, 50f), 6, false))
    }

    @Test
    fun repositionedPreviewCards_cannotMakeTheControllerAlternateBetweenSlots() {
        val controller = TierDragController().apply {
            setValidTargets(listOf(7L), ids)
            registerRowBounds(7, grid.bounds)
            registerWrappedGrid(7, grid)
            beginDrag(DragPayload(6, "Sixth", null, 7, 100.dp, 100.dp), Offset(210f, 75f))
        }
        repeat(20) { frame ->
            ids.forEachIndexed { index, id ->
                val cell = (index + frame) % 6
                controller.registerTileBounds(7, id, index, Rect(cell * 100f, 0f, cell * 100f + 100f, 100f))
            }
            controller.updateDrag(Offset.Zero)
            assertEquals(2, controller.hoverIndex)
        }
        assertEquals(DropOutcome.MoveTo(6, 7, 2), controller.endDrag())
    }
}
