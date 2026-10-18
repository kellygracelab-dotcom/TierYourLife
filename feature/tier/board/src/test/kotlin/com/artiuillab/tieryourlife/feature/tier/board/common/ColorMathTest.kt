package com.artiuillab.tieryourlife.feature.tier.board.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorMathTest {

    @Test
    fun `a tier colour survives the trip through the colour picker`() {
        for (hex in listOf("B03A32", "F1948C", "2E86C1", "27AE60", "F4D03F", "000000", "FFFFFF")) {
            assertEquals(hex, hexToHsl(hex).toHex())
        }
    }

    @Test
    fun `a grey has no hue and no saturation`() {
        val grey = Color(0.5f, 0.5f, 0.5f).toHsl()

        assertEquals(0f, grey.saturation)
        assertEquals(0.5f, grey.lightness, 0.01f)
    }

    @Test
    fun `pure red, green and blue sit a third of the wheel apart`() {
        assertEquals(0f, Color.Red.toHsl().hue, 0.01f)
        assertEquals(120f, Color.Green.toHsl().hue, 0.01f)
        assertEquals(240f, Color.Blue.toHsl().hue, 0.01f)
    }

    @Test
    fun `black on white is the widest contrast there is, either way round`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
    }

    @Test
    fun `a colour on itself has no contrast`() {
        assertEquals(1.0, contrastRatio(Color(0xFFB03A32), Color(0xFFB03A32)), 0.0001)
    }

    @Test
    fun `a hex that is not one falls back instead of crashing`() {
        assertEquals(Color.Gray, parseTierColor("#12345", fallback = Color.Gray))
        assertEquals(Color.Gray, parseTierColor("zzzzzz", fallback = Color.Gray))
        assertEquals(Color(0xFF112233), parseTierColor("#112233"))
    }
}
