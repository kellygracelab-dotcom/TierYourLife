package com.artiuillab.tieryourlife.feature.community.presentation.propose

import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureFrameTest {

    private val wide = PictureSize(3200, 1500)
    private val tall = PictureSize(1500, 2000)

    @Test
    fun aFreshFrame_cutsTheWidest16By10_fromTheMiddle() {
        val cut = PictureFrame().crop(wide)

        // 1500 high at 16:10 is 2400 of the 3200 wide.
        assertEquals(0.75f, cut.width, DELTA)
        assertEquals(1f, cut.height, DELTA)
        assertEquals(0.125f, cut.left, DELTA)
        assertEquals(0f, cut.top, DELTA)
    }

    @Test
    fun aTallPicture_isCutAcrossItsWholeWidth() {
        val cut = PictureFrame().crop(tall)

        assertEquals(1f, cut.width, DELTA)
        assertEquals(1500f / 1.6f / 2000f, cut.height, DELTA)
        assertEquals(0f, cut.left, DELTA)
        assertEquals((1f - cut.height) / 2f, cut.top, DELTA)
    }

    @Test
    fun dragging_stopsAtThePicturesEdge() {
        val farLeft = PictureFrame().moved(panX = 10f, panY = 10f, zoomBy = 1f, picture = wide)
        val cut = farLeft.crop(wide)

        assertEquals(0f, cut.left, DELTA)
        assertEquals(0f, cut.top, DELTA)
        assertEquals(0f, farLeft.offsetY, DELTA)
    }

    @Test
    fun zooming_stopsWhereTheCutWouldGoUnder960() {
        val zoomed = PictureFrame().moved(panX = 0f, panY = 0f, zoomBy = 10f, picture = wide)

        assertEquals(2.5f, zoomed.zoom, DELTA)
        assertEquals(960f, zoomed.crop(wide).width * wide.width, 1f)
    }

    @Test
    fun aHugePicture_zoomsNoFurtherThanFourTimes() {
        val huge = PictureSize(12000, 7500)

        assertEquals(MAX_PICTURE_ZOOM, PictureFrame.maxZoom(huge), DELTA)
    }

    @Test
    fun aPictureJustBigEnough_cannotBeZoomedAtAll() {
        val exact = PictureSize(960, 600)

        assertEquals(1f, PictureFrame().moved(0f, 0f, 3f, exact).zoom, DELTA)
        assertEquals(960, PictureFrame.widestCut(exact))
    }

    @Test
    fun zoomingOut_neverUncoversTheFrame() {
        val zoomed = PictureFrame().moved(panX = 0.3f, panY = 0f, zoomBy = 2f, picture = wide)
        val back = zoomed.moved(panX = 0f, panY = 0f, zoomBy = 0.1f, picture = wide)

        assertEquals(1f, back.zoom, DELTA)
        val cut = back.crop(wide)
        assertTrue(cut.left >= -DELTA)
        assertTrue(cut.left + cut.width <= 1f + DELTA)
    }

    private companion object {
        const val DELTA = 1e-4f
    }
}
