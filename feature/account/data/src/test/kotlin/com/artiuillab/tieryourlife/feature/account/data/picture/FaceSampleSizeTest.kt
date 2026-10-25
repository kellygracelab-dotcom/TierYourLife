package com.artiuillab.tieryourlife.feature.account.data.picture

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceSampleSizeTest {

    // Shrinking by four leaves the shorter edge at 750, still wider than the
    // face; by eight it would be 375 and the face would have to be blown up.
    @Test
    fun aCameraPhoto_isShrunkAsFarAsTheFaceAllows() {
        assertEquals(4, faceSampleSize(width = 4000, height = 3000))
    }

    @Test
    fun aPictureAlreadyNearTheFaceSize_isDecodedWhole() {
        assertEquals(1, faceSampleSize(width = 600, height = 800))
        assertEquals(1, faceSampleSize(width = FACE_EDGE_PX, height = FACE_EDGE_PX))
    }

    @Test
    fun theShorterEdge_decides() {
        assertEquals(2, faceSampleSize(width = 8000, height = 1100))
    }
}
