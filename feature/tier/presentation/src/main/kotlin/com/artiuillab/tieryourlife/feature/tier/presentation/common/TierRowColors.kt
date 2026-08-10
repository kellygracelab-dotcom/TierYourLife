package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia

// Design-system rule: a row fill is the band color at 12% over the surface.
private const val ROW_TINT_ALPHA = 0.12f

internal const val ROW_HOVER_TINT_ALPHA = 0.24f

internal fun parseTierColor(hex: String, fallback: Color = Color.Unspecified): Color {
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

internal fun rowTintFor(band: Color, surface: Color, alpha: Float = ROW_TINT_ALPHA): Color =
    band.copy(alpha = alpha).compositeOver(surface)

internal data class TierRowColors(
    val band: Color,
    val rowTint: Color,
    val onBand: Color,
)

@Composable
internal fun tierRowColors(colorLight: String, colorDark: String): TierRowColors {
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
