package com.artiuillab.tieryourlife.core.theme.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

internal val LightMediaColors = TierYourLifeMediaColors(
    isDark = false,
    tilePlaceholder = TilePlaceholderLight,
    tilePlaceholderAlt = TilePlaceholderAltLight,
    tileLabel = TileLabelLight,
    unrankedRibbon = UnrankedRibbonLight,
    onTierBand = OnTierBandLight,
)

internal val DarkMediaColors = TierYourLifeMediaColors(
    isDark = true,
    tilePlaceholder = TilePlaceholderDark,
    tilePlaceholderAlt = TilePlaceholderAltDark,
    tileLabel = TileLabelDark,
    unrankedRibbon = UnrankedRibbonDark,
    onTierBand = OnTierBandDark,
)

internal val LocalMediaColors = staticCompositionLocalOf { LightMediaColors }

object TierYourLifeMedia {
    val current: TierYourLifeMediaColors
        @Composable
        get() = LocalMediaColors.current
}
