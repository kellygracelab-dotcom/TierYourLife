package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CLOSE = 1e-4f

class FaceFrameTest {

    // Left alone, a wide picture shows its middle square: half its width, all its height.
    @Test
    fun anUntouchedWidePicture_cropsToItsMiddleSquare() {
        val crop = FaceFrame().crop(aspect = 2f)

        assertEquals(0.25f, crop.left, CLOSE)
        assertEquals(0f, crop.top, CLOSE)
        assertEquals(0.5f, crop.width, CLOSE)
        assertEquals(1f, crop.height, CLOSE)
        assertTrue(FaceFrame().untouched)
    }

    // A poster is taller than wide: all its width, the middle two thirds of its height.
    @Test
    fun anUntouchedPoster_cropsToItsMiddleSquare() {
        val crop = FaceFrame().crop(aspect = 2f / 3f)

        assertEquals(0f, crop.left, CLOSE)
        assertEquals(1f / 6f, crop.top, CLOSE)
        assertEquals(1f, crop.width, CLOSE)
        assertEquals(2f / 3f, crop.height, CLOSE)
    }

    // Dragged as far right as it goes, the picture shows its left edge; no further.
    @Test
    fun draggingPastTheEdge_stopsWhereThePictureStillCoversTheFrame() {
        val frame = FaceFrame().moved(panX = 5f, panY = 0f, zoomBy = 1f, aspect = 2f)

        assertEquals(0.5f, frame.offsetX, CLOSE)
        assertEquals(0f, frame.crop(aspect = 2f).left, CLOSE)
        assertFalse(frame.untouched)
    }

    // A square picture at its natural size has nowhere to move.
    @Test
    fun aSquarePicture_cannotMoveUntilZoomed() {
        val still = FaceFrame().moved(panX = 0.3f, panY = -0.3f, zoomBy = 1f, aspect = 1f)
        assertEquals(FaceFrame(), still)

        val zoomed = FaceFrame().moved(panX = 0f, panY = 0f, zoomBy = 2f, aspect = 1f)
        val crop = zoomed.crop(aspect = 1f)
        assertEquals(0.25f, crop.left, CLOSE)
        assertEquals(0.5f, crop.width, CLOSE)
    }

    // Zoomed in and moved, the crop is the square under the frame, in the picture's own fractions.
    @Test
    fun zoomedAndMoved_cropsTheSquareUnderTheFrame() {
        val frame = FaceFrame().moved(panX = 0f, panY = 0f, zoomBy = 2f, aspect = 1f)
            .moved(panX = -0.25f, panY = 0.5f, zoomBy = 1f, aspect = 1f)

        val crop = frame.crop(aspect = 1f)

        assertEquals(0.375f, crop.left, CLOSE)
        assertEquals(0f, crop.top, CLOSE)
        assertEquals(0.5f, crop.width, CLOSE)
        assertEquals(0.5f, crop.height, CLOSE)
    }

    @Test
    fun zoom_staysBetweenTheWholePictureAndTheLimit() {
        assertEquals(1f, FaceFrame().moved(0f, 0f, zoomBy = 0.2f, aspect = 1f).zoom, CLOSE)
        assertEquals(MAX_FACE_ZOOM, FaceFrame().moved(0f, 0f, zoomBy = 50f, aspect = 1f).zoom, CLOSE)
    }

    // Zooming back out pulls a moved picture back so the frame stays covered.
    @Test
    fun zoomingOut_pullsTheOffsetBackInside() {
        val moved = FaceFrame().moved(0f, 0f, zoomBy = 3f, aspect = 1f).moved(panX = 1f, panY = 0f, zoomBy = 1f, aspect = 1f)
        val back = moved.moved(0f, 0f, zoomBy = 1f / 3f, aspect = 1f)

        assertEquals(0f, back.offsetX, CLOSE)
    }
}
