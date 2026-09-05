package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * A section heading does nothing, so it must not wear the accent colour.
 * Caps are a bonus for alphabets that have them; Arabic and Japanese rely on
 * the colour and tracking.
 */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    // The locale of this composition rather than the process default: the app
    // has its own language setting, and Turkish turns i into İ, not I.
    val locale = LocalResources.current.configuration.locales[0]
    Text(
        text = text.uppercase(locale),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
