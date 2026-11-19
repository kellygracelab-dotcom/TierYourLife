package com.artiuillab.tieryourlife.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.artiuillab.tieryourlife.core.theme.color.LocalMediaColors
import com.artiuillab.tieryourlife.core.theme.color.ThemeColour
import com.artiuillab.tieryourlife.core.theme.color.colorSchemeFor
import com.artiuillab.tieryourlife.core.theme.color.mediaColorsFor
import com.artiuillab.tieryourlife.core.theme.type.Typography

/**
 * Lavender unless the person chose another colour (artboards 58–59). The wallpaper's colours come only when
 * asked for: tier colours are each list's own and never follow the theme.
 */
@Composable
fun TierYourLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colour: ThemeColour = ThemeColour.Lavender,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = remember(colour, darkTheme, context) { colorSchemeFor(colour, darkTheme, context) }
    val mediaColors = remember(colour, darkTheme, colorScheme) { mediaColorsFor(colour, darkTheme, colorScheme) }

    CompositionLocalProvider(LocalMediaColors provides mediaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
