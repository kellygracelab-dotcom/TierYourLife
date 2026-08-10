package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class Hsl(val hue: Float, val saturation: Float, val lightness: Float) {
    fun toColor(): Color {
        if (saturation == 0f) return Color(lightness, lightness, lightness)
        val q = if (lightness < 0.5f) {
            lightness * (1f + saturation)
        } else {
            lightness + saturation - lightness * saturation
        }
        val p = 2f * lightness - q
        val h = hue / 360f

        fun hueToChannel(t: Float): Float {
            var tt = t
            if (tt < 0f) tt += 1f
            if (tt > 1f) tt -= 1f
            return when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }

        return Color(
            red = hueToChannel(h + 1f / 3f),
            green = hueToChannel(h),
            blue = hueToChannel(h - 1f / 3f),
        )
    }

    fun toHex(): String {
        val color = toColor()
        val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
        return "%02X%02X%02X".format(r, g, b)
    }
}

internal fun Color.toHsl(): Hsl {
    val r = red
    val g = green
    val b = blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val lightness = (maxC + minC) / 2f
    if (maxC == minC) return Hsl(hue = 0f, saturation = 0f, lightness = lightness)

    val delta = maxC - minC
    val saturation = if (lightness < 0.5f) delta / (maxC + minC) else delta / (2f - maxC - minC)
    val hue = 60f * when (maxC) {
        r -> ((g - b) / delta) + (if (g < b) 6f else 0f)
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    }
    return Hsl(hue = (hue + 360f) % 360f, saturation = saturation, lightness = lightness)
}

internal fun hexToHsl(hex: String): Hsl = parseTierColor(hex, fallback = Color.Gray).toHsl()

internal fun contrastRatio(foreground: Color, background: Color): Double {
    val l1 = relativeLuminance(foreground)
    val l2 = relativeLuminance(background)
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun channel(component: Float): Double {
        val c = component.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}
