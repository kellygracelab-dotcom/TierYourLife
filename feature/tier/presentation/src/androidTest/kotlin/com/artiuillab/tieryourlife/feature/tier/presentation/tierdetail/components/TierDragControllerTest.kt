package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

// Exercises the second layer directly, without a real gesture: a bounds registration
// that was never cleaned up (layer 1 failing, or simply not having run yet) must still
// never be picked as a target once its id is missing from setValidTargets.
@RunWith(AndroidJUnit4::class)
class TierDragControllerTest {

    @Test
    fun aTierTarget_notInTheCurrentValidSet_isNeverChosen_evenWithBoundsStillRegistered() {
        val controller = TierDragController()
        // A stale registration standing in for a deleted tier's ghost, and a live tier
        // right below it — deliberately overlapping the same pointer position the ghost
        // already claims, the exact shape of the reported bug.
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

    // A wrapped tier row lays its tiles out in reading order, which runs the other way in
    // Arabic: index 0 sits at the right edge, not the left. The insertion index is "how many
    // tiles has the pointer passed", so it has to be counted in that same direction —
    // otherwise dropping a poster at the visible start of a row in Arabic files it at the end.
    @Test
    fun aDropInAWrappedRow_countsTilesInReadingOrder_whichRunsRightToLeftInArabic() {
        // Three 100-wide tiles in one line. Left to right on screen: 0 1 2 in English,
        // 2 1 0 in Arabic — the same rectangles, laid out by the opposite reading order.
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

        // Dropped just inside the row's own leading edge — the left edge in English, the
        // right edge in Arabic. Either way that is the very front of the row.
        val english = controller(rightToLeft = false, indicesLeftToRight = listOf(0, 1, 2))
        english.beginDrag(dragged(), rootPosition = Offset(10f, 50f))
        val droppedInEnglish = english.endDrag()

        val arabic = controller(rightToLeft = true, indicesLeftToRight = listOf(2, 1, 0))
        arabic.beginDrag(dragged(), rootPosition = Offset(290f, 50f))
        val droppedInArabic = arabic.endDrag()

        assertEquals(DropOutcome.MoveTo(itemId = 7L, toTierId = 5L, toPosition = 0), droppedInEnglish)
        assertEquals(DropOutcome.MoveTo(itemId = 7L, toTierId = 5L, toPosition = 0), droppedInArabic)
    }

    // The counterpart: the far end of the row is the right edge in English and the left edge
    // in Arabic, and both must land after all three tiles.
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
