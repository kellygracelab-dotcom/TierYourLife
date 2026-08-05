package com.artiuillab.tieryourlife.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    surface = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    surface = SurfaceDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
)

// Separate from ColorScheme: these colors have no Material 3 roles, and using
// unused roles such as tertiary for them would misrepresent their meaning.
data class TierYourLifeMediaColors(
    val isDark: Boolean,
    val tilePlaceholder: Color,
    val tilePlaceholderAlt: Color,
    val tileLabel: Color,
    val unrankedRibbon: Color,
    val onTierBand: Color,
)

private val LightMediaColors = TierYourLifeMediaColors(
    isDark = false,
    tilePlaceholder = TilePlaceholderLight,
    tilePlaceholderAlt = TilePlaceholderAltLight,
    tileLabel = TileLabelLight,
    unrankedRibbon = UnrankedRibbonLight,
    onTierBand = OnTierBandLight,
)

private val DarkMediaColors = TierYourLifeMediaColors(
    isDark = true,
    tilePlaceholder = TilePlaceholderDark,
    tilePlaceholderAlt = TilePlaceholderAltDark,
    tileLabel = TileLabelDark,
    unrankedRibbon = UnrankedRibbonDark,
    onTierBand = OnTierBandDark,
)

object TierYourLifeMedia {
    val current: TierYourLifeMediaColors
        @Composable
        get() = LocalMediaColors.current
}

private val LocalMediaColors = staticCompositionLocalOf { LightMediaColors }

// Type roles the design uses that have no matching Material 3 slot — decorative or
// small text tied to this app's own tier-band/chip/tab vocabulary (docs/design-spec-turns-8-9.md,
// section 2), not general UI copy. Doesn't vary with the theme (unlike the color roles
// above), so it's exposed the same way but without a CompositionLocal behind it.
data class TierYourLifeExtraType(
    val tierBandLetter: TextStyle,
    val tierBandCaption: TextStyle,
    val tierSwatchLetter: TextStyle,
    val tabLabel: TextStyle,
    val captionUnderTitle: TextStyle,
    val chipText: TextStyle,
    val trashRemoveLabel: TextStyle,
)

private val ExtraType = TierYourLifeExtraType(
    tierBandLetter = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp),
    tierBandCaption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 12.sp),
    tierSwatchLetter = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp),
    tabLabel = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    captionUnderTitle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    chipText = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    trashRemoveLabel = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp),
)

object TierYourLifeType {
    val current: TierYourLifeExtraType = ExtraType
}

// Material You is intentionally unsupported: wallpaper colors would distort the tier scale.
@Composable
fun TierYourLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val mediaColors = if (darkTheme) DarkMediaColors else LightMediaColors

    CompositionLocalProvider(LocalMediaColors provides mediaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
