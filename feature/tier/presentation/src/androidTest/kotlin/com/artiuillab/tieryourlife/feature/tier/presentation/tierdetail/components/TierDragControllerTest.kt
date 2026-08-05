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
}
