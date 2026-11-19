package com.artiuillab.tieryourlife.core.theme.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The proxy's `functions/test/theme-vectors.json`, copied unchanged: every role of the three ready colours'
 * tables and of twelve own hues, light and dark, within 1 on each channel, as the site has them.
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
            val primary = preset.jsonObject.getValue("palettes").jsonObject.getValue("primary").jsonObject
            assertEquals(name, ReadyColours.hueOf(name), primary.getValue("hue").jsonPrimitive.double.roundToInt())
            check(name, preset.jsonObject) { dark -> checkNotNull(readyColorScheme(name, dark)) }
        }
    }

    @Test
    fun ownHues_areTheVectors() {
        val hues = vectors.getValue("ownHues").jsonArray
        assertTrue(hues.size >= 12)
        for (own in hues) {
            val hue = own.jsonObject.getValue("hue").jsonPrimitive.int
            check("hue $hue", own.jsonObject) { dark -> hueColorScheme(hue, dark) }
        }
    }

    @Test
    fun theSeed_isTheVectors() {
        for (own in vectors.getValue("ownHues").jsonArray) {
            val hue = own.jsonObject.getValue("hue").jsonPrimitive.int
            assertClose("seed of $hue", own.jsonObject.getValue("seed").jsonPrimitive.content, hueSeed(hue))
        }
    }

    private fun check(what: String, expected: JsonObject, schemeOf: (dark: Boolean) -> ColorScheme) {
        for (dark in listOf(false, true)) {
            val roles = expected.getValue(if (dark) "dark" else "light").jsonObject
            val scheme = schemeOf(dark)
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
