package com.artiuillab.tieryourlife.feature.tier.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverFrameTest {

    private fun assertFrame(expected: CoverFrame, actual: CoverFrame) {
        assertEquals(expected.left, actual.left, DELTA)
        assertEquals(expected.top, actual.top, DELTA)
        assertEquals(expected.width, actual.width, DELTA)
        assertEquals(expected.height, actual.height, DELTA)
    }

    @Test
    fun `without a focus a wide picture is cut in the middle`() {
        assertFrame(CoverFrame(400.0, 0.0, 1200.0, 600.0), coverFrame(2000f, 600f, aspect = 2f, focus = null))
    }

    @Test
    fun `a tall picture keeps a head near the top`() {
        // 1000x2000 portrait, a 2:1 header: 1000x500, centred on the face at 15% down.
        assertFrame(CoverFrame(0.0, 50.0, 1000.0, 500.0), coverFrame(1000f, 2000f, aspect = 2f, focus = CoverFocus(0.5f, 0.15f)))
    }

    @Test
    fun `a focus near an edge never shows past it`() {
        assertFrame(CoverFrame(0.0, 0.0, 1000.0, 500.0), coverFrame(1000f, 2000f, aspect = 2f, focus = CoverFocus(0f, 0f)))
        assertFrame(CoverFrame(0.0, 1500.0, 1000.0, 500.0), coverFrame(1000f, 2000f, aspect = 2f, focus = CoverFocus(1f, 1f)))
    }

    @Test
    fun `zoom takes a smaller part around the focus`() {
        assertFrame(CoverFrame(650.0, 400.0, 300.0, 300.0), coverFrame(1000f, 1000f, aspect = 1f, focus = CoverFocus(0.8f, 0.55f, zoom = 10f / 3f)))
    }

    @Test
    fun `a focus out of range is brought in, as finely as the site keeps it`() {
        assertEquals(CoverFocus(1f, 0f, 4f), CoverFocus(1.4f, -0.2f, 9f).inRange())
        assertEquals(CoverFocus(0.1235f, 0.5f, 1.25f), CoverFocus(0.123456f, 0.5f, 1.254f).inRange())
    }

    @Test
    fun `the fingerprint spells it in whole steps, as the site does`() {
        assertEquals(listOf("1235", "2000", "125"), CoverFocus(0.1235f, 0.2f, 1.25f).fingerprintParts)
        assertEquals(listOf("10000", "0", "400"), CoverFocus(1f, 0f, 4f).fingerprintParts)
    }

    private companion object {
        const val DELTA = 0.01
    }
}
