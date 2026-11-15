package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverGeometryTest {

    // A 1000 x 2000 portrait fitted in a 400 x 400 space: 200 x 400, in the middle.
    private val portrait = Size(1000f, 2000f)
    private val placed = fitted(portrait, Size(400f, 400f))

    @Test
    fun theWholePictureFits_inTheMiddle() {
        assertEquals(Rect(100f, 0f, 300f, 400f), placed)
    }

    @Test
    fun thePointFollowsTheFinger_overTheWholePicture() {
        // Down 40 of 400 is a tenth of the picture; right 20 of 200, a tenth of its width.
        val moved = CoverFocus.CENTRED.moved(Offset(20f, 40f), 1f, placed)

        assertEquals(0.6f, moved.x, DELTA)
        assertEquals(0.6f, moved.y, DELTA)
    }

    @Test
    fun thePointGoesSideways_evenWhereTheHeaderFrameCannot() {
        // A header frame over a portrait is its full width, yet a square tile still has room either side.
        val frame = frameOn(placed, portrait, PAGE_HEADER_ASPECT, CoverFocus.CENTRED)
        val moved = CoverFocus.CENTRED.moved(Offset(-80f, 0f), 1f, placed)

        assertEquals(placed.width, frame.width, DELTA)
        assertEquals(0.1f, moved.x, DELTA)
    }

    @Test
    fun thePointStaysOnThePicture_andAPinchBetweenOneAndFour() {
        assertEquals(CoverFocus(1f, 0f), CoverFocus.CENTRED.moved(Offset(5000f, -5000f), 1f, placed))
        assertEquals(CoverFocus.MAX_ZOOM, CoverFocus(zoom = 3f).moved(Offset.Zero, 2f, placed).zoom, DELTA)
        assertEquals(CoverFocus.MIN_ZOOM, CoverFocus(zoom = 1.5f).moved(Offset.Zero, 0.1f, placed).zoom, DELTA)
    }

    @Test
    fun aTapPutsThePointWhereItLanded() {
        val tapped = CoverFocus(zoom = 2f).placedAt(Offset(150f, 100f), placed)

        assertEquals(CoverFocus(0.25f, 0.25f, 2f), tapped)
    }

    @Test
    fun theFrameStopsAtTheEdge_whileThePointGoesOn() {
        val frame = frameOn(placed, portrait, PAGE_HEADER_ASPECT, CoverFocus(0.5f, 0f))

        assertEquals(placed.top, frame.top, DELTA)
    }

    @Test
    fun beforeThePictureIsKnown_nothingMoves() {
        val focus = CoverFocus(0.3f, 0.3f, 2f)
        val unknown = fitted(Size.Unspecified, Size(400f, 400f))

        assertEquals(focus, focus.moved(Offset(10f, 10f), 2f, unknown))
        assertEquals(focus, focus.placedAt(Offset(10f, 10f), unknown))
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
