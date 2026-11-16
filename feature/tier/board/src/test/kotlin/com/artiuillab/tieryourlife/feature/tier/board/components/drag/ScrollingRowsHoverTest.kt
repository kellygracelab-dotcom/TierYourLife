package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The list scrolling on its own near an edge: the rows move, the finger holding the card does not. */
class ScrollingRowsHoverTest {

    private val pool = 100L
    private val finger = Offset(160f, 452f)

    private fun controller() = TierDragController().apply {
        setValidTargets(listOf(1L, 2L, 20L), listOf(500L))
        registerRowBounds(1L, Rect(0f, 400f, 320f, 450f))
        registerRowBounds(2L, Rect(0f, 458f, 320f, 508f))
        beginDrag(DragPayload(500L, "Interstellar", null, pool, 52.dp, 76.dp), finger)
    }

    @Test
    fun aFingerBetweenTwoRows_holdsNothing_untilARowScrollsUnderIt() {
        val controller = controller()
        assertNull(controller.hoveredTierId)

        controller.registerRowBounds(20L, Rect(0f, 420f, 320f, 470f))

        assertEquals(20L, controller.hoveredTierId)
    }

    @Test
    fun aRowScrollingAwayFromUnderTheFinger_isNoLongerTheOneHeld() {
        val controller = controller()
        controller.registerRowBounds(2L, Rect(0f, 440f, 320f, 490f))
        assertEquals(2L, controller.hoveredTierId)

        controller.registerRowBounds(2L, Rect(0f, 300f, 320f, 350f))

        assertNull(controller.hoveredTierId)
    }
}
