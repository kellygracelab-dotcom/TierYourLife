package com.artiuillab.tieryourlife.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.artiuillab.tieryourlife.core.theme.color.DarkColorScheme
import com.artiuillab.tieryourlife.core.theme.color.DarkMediaColors
import com.artiuillab.tieryourlife.core.theme.color.LightColorScheme
import com.artiuillab.tieryourlife.core.theme.color.LightMediaColors
import com.artiuillab.tieryourlife.core.theme.color.LocalMediaColors
import com.artiuillab.tieryourlife.core.theme.type.Typography

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
