package com.artiuillab.tieryourlife.feature.tier.board.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WhiteLetterTest {

    @Test
    fun `A's orange and B's gold darken as on the site, until a white letter reads`() {
        assertEquals(parseTierColor("#A55B20"), parseTierColor("#C06A25").darkenedForWhite())
        listOf("#C06A25", "#B88A00", "#F4D03F").forEach { hex ->
            assertTrue(hex, whiteContrast(parseTierColor(hex).darkenedForWhite()) >= WHITE_LETTER_FROM)
        }
    }

    @Test
    fun `B's old olive is drawn as the gold, with a white letter at 3 to 1 or more`() {
        assertEquals(parseTierColor("#B88A00"), parseTierColor("#A98B1F"))
        assertEquals(parseTierColor("#B88A00"), parseTierColor("a98b1f"))
        assertTrue(whiteContrast(parseTierColor("#B88A00")) >= 3f)
        assertEquals(parseTierColor("#D8C05A"), parseTierColor("#D8C05A"))
    }

    @Test
    fun `a colour dark enough already stays as it is`() {
        assertEquals(parseTierColor("#B03A32"), parseTierColor("#B03A32").darkenedForWhite())
        assertEquals(parseTierColor("#3C6E99"), parseTierColor("#3C6E99").darkenedForWhite())
    }

    @Test
    fun `under a dark letter the light band is kept`() {
        val dark = TierRowColors(band = parseTierColor("#E9A867"), rowTint = Color.Gray, onBand = parseTierColor("#121318"))
        assertSame(dark, dark.forLetterOnButton())

        val light = TierRowColors(band = parseTierColor("#C06A25"), rowTint = Color.Gray, onBand = Color.White)
        assertEquals(parseTierColor("#A55B20"), light.forLetterOnButton().band)
    }
}
