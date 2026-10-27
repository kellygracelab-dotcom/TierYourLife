package com.artiuillab.tieryourlife.feature.account.data.picture

import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import org.junit.Assert.assertEquals
import org.junit.Test

class FaceSquareTest {

    @Test
    fun withoutACrop_theMiddleSquareIsTaken() {
        assertEquals(PixelSquare(left = 250, top = 0, side = 500), faceSquare(width = 1000, height = 500, crop = null))
        assertEquals(PixelSquare(left = 0, top = 150, side = 600), faceSquare(width = 600, height = 900, crop = null))
    }

    // The same fractions on a bitmap decoded at a quarter of the size land on the same part of the picture.
    @Test
    fun aCrop_landsOnTheSamePartOfThePictureAtAnyDecodedSize() {
        val crop = FaceCrop(left = 0.5f, top = 0.25f, width = 0.25f, height = 0.5f)

        assertEquals(PixelSquare(left = 2000, top = 500, side = 1000), faceSquare(width = 4000, height = 2000, crop = crop))
        assertEquals(PixelSquare(left = 500, top = 125, side = 250), faceSquare(width = 1000, height = 500, crop = crop))
    }

    // Rounding, or a crop chosen on a slightly different decode, never reaches past an edge.
    @Test
    fun aCropHangingOverTheEdge_isPulledBackInside() {
        val crop = FaceCrop(left = 0.9f, top = 0.9f, width = 0.5f, height = 0.5f)

        assertEquals(PixelSquare(left = 500, top = 500, side = 500), faceSquare(width = 1000, height = 1000, crop = crop))
    }

    // Zoomed into a corner of a camera photo, decoding shrinks less so the face stays sharp.
    @Test
    fun aSmallCrop_shrinksLessWhileDecoding() {
        assertEquals(4, faceSampleSize(width = 4000, height = 3000))
        val corner = FaceCrop(left = 0f, top = 0f, width = 0.25f, height = 1f / 3f)
        assertEquals(1, faceSampleSize(width = 4000, height = 3000, crop = corner))
    }
}
