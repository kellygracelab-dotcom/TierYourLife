package com.artiuillab.tieryourlife.core.theme.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The proxy's `functions/test/theme-vectors.json`, copied unchanged: every role of the three ready colours and
 * twelve own hues, light and dark, within 1 on each channel, as the site builds them.
 */
class ThemeVectorsTest {

    private val vectors: JsonObject = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResource("theme-vectors.json")).readText(),
    ).jsonObject

    private val tolerance = vectors.getValue("tolerance").jsonPrimitive.int

    @Test
    fun theReadyColours_areTheVectors() {
        val presets = vectors.getValue("presets").jsonObject
        assertEquals(setOf("matcha", "peach", "sky"), presets.keys)
        for ((name, preset) in presets) {
            val hue = preset.jsonObject.getValue("hue").jsonPrimitive.int
            assertEquals(name, ReadyColours.hueOf(name), hue)
            check("$name ($hue)", hue, preset.jsonObject)
        }
    }

    @Test
    fun ownHues_areTheVectors() {
        val hues = vectors.getValue("ownHues").jsonArray
        assertTrue(hues.size >= 12)
        for (own in hues) {
            val hue = own.jsonObject.getValue("hue").jsonPrimitive.int
            check("hue $hue", hue, own.jsonObject)
        }
    }

    @Test
    fun theSeed_isTheVectors() {
        for (own in vectors.getValue("ownHues").jsonArray) {
            val hue = own.jsonObject.getValue("hue").jsonPrimitive.int
            assertClose("seed of $hue", own.jsonObject.getValue("seed").jsonPrimitive.content, hueSeed(hue))
        }
    }

    private fun check(what: String, hue: Int, expected: JsonObject) {
        for (dark in listOf(false, true)) {
            val roles = expected.getValue(if (dark) "dark" else "light").jsonObject
            val scheme = hueColorScheme(hue, dark)
            assertEquals("$what: every role", 36, roles.size)
            for ((role, hex) in roles) {
                assertClose("$what ${if (dark) "dark" else "light"} $role", hex.jsonPrimitive.content, scheme.role(role))
            }
        }
    }

    private fun assertClose(what: String, hex: String, actual: Color) {
        val want = hex.removePrefix("#").toLong(16).toInt()
        val got = actual.toArgb()
        val apart = listOf(16, 8, 0).maxOf { shift -> abs((want shr shift and 0xFF) - (got shr shift and 0xFF)) }
        assertTrue("$what: want $hex, got #%06X".format(got and 0xFFFFFF), apart <= tolerance)
    }

    private fun ColorScheme.role(name: String): Color = when (name) {
        "primary" -> primary
        "onPrimary" -> onPrimary
        "primaryContainer" -> primaryContainer
        "onPrimaryContainer" -> onPrimaryContainer
        "inversePrimary" -> inversePrimary
        "secondary" -> secondary
        "onSecondary" -> onSecondary
        "secondaryContainer" -> secondaryContainer
        "onSecondaryContainer" -> onSecondaryContainer
        "tertiary" -> tertiary
        "onTertiary" -> onTertiary
        "tertiaryContainer" -> tertiaryContainer
        "onTertiaryContainer" -> onTertiaryContainer
        "error" -> error
        "onError" -> onError
        "errorContainer" -> errorContainer
        "onErrorContainer" -> onErrorContainer
        "background" -> background
        "onBackground" -> onBackground
        "surface" -> surface
        "onSurface" -> onSurface
        "surfaceVariant" -> surfaceVariant
        "onSurfaceVariant" -> onSurfaceVariant
        "surfaceTint" -> surfaceTint
        "surfaceDim" -> surfaceDim
        "surfaceBright" -> surfaceBright
        "surfaceContainerLowest" -> surfaceContainerLowest
        "surfaceContainerLow" -> surfaceContainerLow
        "surfaceContainer" -> surfaceContainer
        "surfaceContainerHigh" -> surfaceContainerHigh
        "surfaceContainerHighest" -> surfaceContainerHighest
        "inverseSurface" -> inverseSurface
        "inverseOnSurface" -> inverseOnSurface
        "outline" -> outline
        "outlineVariant" -> outlineVariant
        "scrim" -> scrim
        else -> error("A role the vectors have and the scheme does not: $name")
    }
}
