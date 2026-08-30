package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which windows get which layout.
 *
 * Every case here is a real device in a real position, because the mistake
 * this class exists to stop is a layout that asks about devices: a phone in
 * split view is not a tablet, and a folding phone is three of these within a
 * second.
 */
class WindowShapeTest {

    @Test
    fun `an upright phone is narrow`() {
        assertEquals(WindowShape.Narrow, WindowShape.of(412.dp, 915.dp))
        assertEquals(WindowShape.Narrow, WindowShape.of(320.dp, 720.dp))
    }

    // Wide enough for the rail, and that is the one rule the two form factors
    // share: a rail takes less width than a row of tabs. A big phone on its
    // side even measures as wide as a tablet -- the column beside a board is
    // held back by a separate height condition rather than by this.
    @Test
    fun `a phone on its side has room for the rail`() {
        assertEquals(WindowShape.Medium, WindowShape.of(800.dp, 360.dp))
        assertEquals(WindowShape.Wide, WindowShape.of(880.dp, 360.dp))
    }

    @Test
    fun `a tablet is wide either way up`() {
        assertEquals(WindowShape.Wide, WindowShape.of(1280.dp, 800.dp))
        // Portrait: still wide, which is what keeps the rail and drops the
        // index -- the index has its own height condition.
        assertEquals(WindowShape.Wide, WindowShape.of(1024.dp, 1280.dp))
    }

    @Test
    fun `a small tablet is medium`() {
        assertEquals(WindowShape.Medium, WindowShape.of(600.dp, 960.dp))
    }

    // Measured off a Z Flip 7: 352 x 339dp. Nothing else in this app is
    // anywhere near square.
    @Test
    fun `a folding phone's cover is its own thing`() {
        assertEquals(WindowShape.Cover, WindowShape.of(352.dp, 339.dp))
    }

    // The case a width alone would get wrong. A small phone held sideways is
    // still a phone: it can be typed on, and treating it as a cover would take
    // away everything somebody went there to do.
    @Test
    fun `a small phone on its side is not a cover`() {
        assertEquals(WindowShape.Narrow, WindowShape.of(560.dp, 300.dp))
    }

    @Test
    fun `a very short upright window is not a cover either`() {
        assertEquals(WindowShape.Narrow, WindowShape.of(360.dp, 800.dp))
    }

    // Without a height there is nothing to compare, so nothing is ever
    // mistaken for a cover.
    @Test
    fun `an unknown height never reads as a cover`() {
        assertEquals(WindowShape.Narrow, WindowShape.of(352.dp))
    }

    @Test
    fun `only the two wider shapes carry a rail`() {
        assertFalse(WindowShape.Narrow.hasRail)
        assertFalse(WindowShape.Cover.hasRail)
        assertTrue(WindowShape.Medium.hasRail)
        assertTrue(WindowShape.Wide.hasRail)
    }

    @Test
    fun `only the widest holds a list beside a board`() {
        assertFalse(WindowShape.Medium.holdsTwoPanes)
        assertTrue(WindowShape.Wide.holdsTwoPanes)
    }

    @Test
    fun `only the cover refuses to be worked on`() {
        assertTrue(WindowShape.Cover.isGlanceable)
        assertFalse(WindowShape.Narrow.isGlanceable)
        assertFalse(WindowShape.Wide.isGlanceable)
    }
}
