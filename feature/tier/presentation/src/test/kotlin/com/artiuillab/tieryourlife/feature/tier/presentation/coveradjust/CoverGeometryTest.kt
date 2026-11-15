package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverGeometryTest {

    // A 1000 x 2000 portrait under a 400 x 300 frame: the widest 4:3 crop is 1000 x 750, drawn at 0.4.
    private val portrait = Size(1000f, 2000f)
    private val frame = Size(400f, 300f)

    @Test
    fun theFocusMovesAgainstTheFinger() {
        // Up by 40 screen pixels is 100 of the picture's: the frame now shows lower down.
        val moved = CoverFocus.CENTRED.moved(Offset(0f, -40f), 1f, portrait, frame)

        assertEquals(0.5f, moved.x, DELTA)
        assertEquals((1000f + 100f) / 2000f, moved.y, DELTA)
    }

    @Test
    fun theFrameStopsAtTheEdge_andTheFocusWithIt() {
        val moved = CoverFocus.CENTRED.moved(Offset(0f, 5000f), 1f, portrait, frame)

        // The crop's top at 0: its centre is half its height down.
        assertEquals(375f / 2000f, moved.y, DELTA)
        // A full-width crop cannot move sideways at all.
        assertEquals(0.5f, CoverFocus.CENTRED.moved(Offset(300f, 0f), 1f, portrait, frame).x, DELTA)
    }

    @Test
    fun aPinchIsHeldBetweenOneAndFour() {
        assertEquals(CoverFocus.MAX_ZOOM, CoverFocus(zoom = 3f).moved(Offset.Zero, 2f, portrait, frame).zoom, DELTA)
        assertEquals(CoverFocus.MIN_ZOOM, CoverFocus(zoom = 1.5f).moved(Offset.Zero, 0.1f, portrait, frame).zoom, DELTA)
    }

    @Test
    fun beforeThePictureIsKnown_nothingMoves() {
        val focus = CoverFocus(0.3f, 0.3f, 2f)

        assertEquals(focus, focus.moved(Offset(10f, 10f), 2f, Size.Unspecified, frame))
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
