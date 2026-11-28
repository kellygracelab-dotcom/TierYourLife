package com.artiuillab.tieryourlife.feature.tier.data.local.image

import org.junit.Assert.assertEquals
import org.junit.Test

class StoredSizeTest {

    @Test
    fun aCameraPhoto_isKeptAtTheEdgeExactly() {
        assertEquals(1200 to 900, storedSize(4000, 3000))
        assertEquals(900 to 1200, storedSize(3000, 4000))
        assertEquals(1200 to 675, storedSize(1920, 1080))
    }

    @Test
    fun aPictureNoLongerThanTheEdge_isNeverGrown() {
        assertEquals(1024 to 1024, storedSize(1024, 1024))
        assertEquals(1200 to 800, storedSize(1200, 800))
        assertEquals(500 to 375, storedSize(500, 375))
    }

    @Test
    fun aVeryThinPicture_keepsAtLeastOnePixel() {
        assertEquals(1200 to 1, storedSize(24000, 2))
    }
}
