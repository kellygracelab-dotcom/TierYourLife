package com.artiuillab.tieryourlife.feature.community.data.picture

import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The cut a section's picture becomes: 16:10, 1600 × 1000 at most, as wide as it is below that, never under 960 × 600. */
class SectionPictureCutTest {

    @Test
    fun aBigPicture_isExportedAt1600By1000() {
        val cut = sectionCut(4000, 3000, PictureCrop(0f, 0f, 1f, 4000f / 1.6f / 3000f))

        assertEquals(4000, cut?.width)
        assertEquals(2500, cut?.height)
        assertEquals(1600, cut?.exportWidth)
        assertEquals(1000, cut?.exportHeight)
    }

    @Test
    fun aSmallerCut_keepsItsOwnWidth() {
        val cut = sectionCut(1200, 900, PictureCrop(0f, 0.08f, 1f, 750f / 900f))

        assertEquals(1200, cut?.exportWidth)
        assertEquals(750, cut?.exportHeight)
    }

    @Test
    fun aCutNarrowerThan960_isRefused() {
        assertNull(sectionCut(900, 600, PictureCrop(0f, 0f, 1f, 1f)))
    }

    @Test
    fun aShrunkDecode_isMeasuredOnThePictureItself() {
        val cut = sectionCut(1000, 750, PictureCrop(0f, 0f, 1f, 625f / 750f), scale = 4)

        assertEquals(1600, cut?.exportWidth)
    }

    @Test
    fun aCutHangingOverAnEdge_isHeldInside() {
        val cut = sectionCut(2000, 1250, PictureCrop(0.9f, 0.9f, 0.5f, 0.5f))!!

        assertEquals(2000 - cut.width, cut.left)
        assertEquals(1250 - cut.height, cut.top)
    }

    @Test
    fun decodingShrinksOnlyAsFarAsTheWidestExportAllows() {
        assertEquals(1, sectionSampleSize(3000, PictureCrop(0f, 0f, 1f, 1f)))
        assertEquals(2, sectionSampleSize(3200, PictureCrop(0f, 0f, 1f, 1f)))
        assertEquals(4, sectionSampleSize(6400, PictureCrop(0f, 0f, 1f, 1f)))
    }
}
