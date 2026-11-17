package com.artiuillab.tieryourlife.feature.tier.board.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia

private const val ROW_TINT_ALPHA = 0.12f

const val ROW_HOVER_TINT_ALPHA = 0.24f

fun parseTierColor(hex: String, fallback: Color = Color.Unspecified): Color {
    val body = hex.removePrefix("#")
    if (body.length != 6 && body.length != 8) return fallback

    var value = 0L
    for (char in body) {
        val digit = when (char) {
            in '0'..'9' -> char - '0'
            in 'a'..'f' -> char - 'a' + 10
            in 'A'..'F' -> char - 'A' + 10
            else -> return fallback
        }
        value = value * 16 + digit
    }

    return if (body.length == 6) Color(value or 0xFF000000L) else Color(value)
}

fun rowTintFor(band: Color, surface: Color, alpha: Float = ROW_TINT_ALPHA): Color =
    band.copy(alpha = alpha).compositeOver(surface)

data class TierRowColors(
    val band: Color,
    val rowTint: Color,
    val onBand: Color,
)

@Composable
fun tierRowColors(colorLight: String, colorDark: String): TierRowColors {
    val media = TierYourLifeMedia.current
    val surface = MaterialTheme.colorScheme.surface
    val hex = if (media.isDark) colorDark else colorLight
    val band = parseTierColor(hex, fallback = media.unrankedRibbon)

    return TierRowColors(
        band = band,
        rowTint = rowTintFor(band, surface),
        onBand = media.onTierBand,
    )
}

/**
 * A tier's colour as a button under a white letter (artboard 18): A's orange and
 * B's ochre left the letter near 3.9 and 3.3 to 1. The colour is darkened, its
 * hue kept, until the letter reaches 5 to 1. A dark letter, the dark theme's,
 * keeps the colour as it is.
 */
fun TierRowColors.forLetterOnButton(): TierRowColors =
    if (onBand.luminance() < WHITE_LETTER) this else copy(band = band.darkenedForWhite())

internal fun Color.darkenedForWhite(): Color {
    if (whiteContrast(this) >= WHITE_LETTER_FROM) return this
    // As the site does it: each channel scaled down in steps of 2 %, so the hue stays.
    for (step in 1 until DARKEN_STEPS) {
        val k = 1f - step * DARKEN_STEP
        val darker = Color(red = toByte(red * k), green = toByte(green * k), blue = toByte(blue * k), alpha = alpha)
        if (whiteContrast(darker) >= WHITE_LETTER_FROM) return darker
    }
    return Color.Black.copy(alpha = alpha)
}

/** WCAG contrast of a white letter on [color], 1 to 21. */
internal fun whiteContrast(color: Color): Float = (WHITE_LUMINANCE + FLARE) / (color.luminance() + FLARE)

private fun toByte(channel: Float): Float = Math.round(channel * BYTE) / BYTE

internal const val WHITE_LETTER_FROM = 5f
private const val WHITE_LETTER = 0.5f
private const val WHITE_LUMINANCE = 1f
private const val FLARE = 0.05f
private const val DARKEN_STEP = 0.02f
private const val DARKEN_STEPS = 50
private const val BYTE = 255f
