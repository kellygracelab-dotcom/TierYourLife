package com.artiuillab.tieryourlife.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
