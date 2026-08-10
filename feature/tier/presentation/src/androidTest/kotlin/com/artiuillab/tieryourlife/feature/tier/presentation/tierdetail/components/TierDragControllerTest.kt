package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDragControllerTest {

    @Test
    fun aTierTarget_notInTheCurrentValidSet_isNeverChosen_evenWithBoundsStillRegistered() {
        val controller = TierDragController()
        controller.registerRowBounds(tierId = 99L, bounds = Rect(0f, 0f, 1000f, 1000f))
        controller.setValidTargets(tierIds = listOf(2L), itemIds = emptyList())

        controller.beginDrag(
            payload = DragPayload(itemId = 1L, title = "x", imageUrl = null, sourceTierId = 2L, width = 44.dp, height = 64.dp),
            rootPosition = Offset(500f, 500f),
        )

        assertNull("a target whose id isn't in the valid set must never be hovered", controller.hoveredTierId)
    }

    @Test
    fun aTierTarget_inTheCurrentValidSet_isStillChosenNormally() {
        val controller = TierDragController()
        controller.registerRowBounds(tierId = 2L, bounds = Rect(0f, 0f, 1000f, 1000f))
        controller.setValidTargets(tierIds = listOf(2L), itemIds = emptyList())

        controller.beginDrag(
            payload = DragPayload(itemId = 1L, title = "x", imageUrl = null, sourceTierId = 2L, width = 44.dp, height = 64.dp),
            rootPosition = Offset(500f, 500f),
        )

        assertEquals(2L, controller.hoveredTierId)
    }

    @Test
    fun aDropInAWrappedRow_countsTilesInReadingOrder_whichRunsRightToLeftInArabic() {
        fun controller(rightToLeft: Boolean, indicesLeftToRight: List<Int>) =
            TierDragController().apply {
                registerRowBounds(tierId = 5L, bounds = Rect(0f, 0f, 300f, 100f))
                indicesLeftToRight.forEachIndexed { slot, index ->
                    registerTileBounds(
                        tierId = 5L,
                        itemId = 100L + index,
                        index = index,
                        bounds = Rect(slot * 100f, 0f, slot * 100f + 100f, 100f),
                    )
                }
                setValidTargets(
                    tierIds = listOf(5L),
                    itemIds = indicesLeftToRight.map { 100L + it },
                    rightToLeft = rightToLeft,
                )
            }

        val english = controller(rightToLeft = false, indicesLeftToRight = listOf(0, 1, 2))
        english.beginDrag(dragged(), rootPosition = Offset(10f, 50f))
        val droppedInEnglish = english.endDrag()

        val arabic = controller(rightToLeft = true, indicesLeftToRight = listOf(2, 1, 0))
        arabic.beginDrag(dragged(), rootPosition = Offset(290f, 50f))
        val droppedInArabic = arabic.endDrag()

        assertEquals(DropOutcome.MoveTo(itemId = 7L, toTierId = 5L, toPosition = 0), droppedInEnglish)
        assertEquals(DropOutcome.MoveTo(itemId = 7L, toTierId = 5L, toPosition = 0), droppedInArabic)
    }

    @Test
    fun aDropAtTheFarEndOfAWrappedRow_landsLastInEitherReadingOrder() {
        fun controller(rightToLeft: Boolean, indicesLeftToRight: List<Int>) =
            TierDragController().apply {
                registerRowBounds(tierId = 5L, bounds = Rect(0f, 0f, 300f, 100f))
                indicesLeftToRight.forEachIndexed { slot, index ->
                    registerTileBounds(
                        tierId = 5L,
                        itemId = 100L + index,
                        index = index,
                        bounds = Rect(slot * 100f, 0f, slot * 100f + 100f, 100f),
                    )
                }
                setValidTargets(
                    tierIds = listOf(5L),
                    itemIds = indicesLeftToRight.map { 100L + it },
                    rightToLeft = rightToLeft,
                )
            }

        val english = controller(rightToLeft = false, indicesLeftToRight = listOf(0, 1, 2))
        english.beginDrag(dragged(), rootPosition = Offset(290f, 50f))

        val arabic = controller(rightToLeft = true, indicesLeftToRight = listOf(2, 1, 0))
        arabic.beginDrag(dragged(), rootPosition = Offset(10f, 50f))

        assertEquals(DropOutcome.MoveTo(itemId = 7L, toTierId = 5L, toPosition = 3), english.endDrag())
        assertEquals(DropOutcome.MoveTo(itemId = 7L, toTierId = 5L, toPosition = 3), arabic.endDrag())
    }

    private fun dragged() = DragPayload(
        itemId = 7L,
        title = "dragged",
        imageUrl = null,
        sourceTierId = 9L,
        width = 44.dp,
        height = 64.dp,
    )
}
