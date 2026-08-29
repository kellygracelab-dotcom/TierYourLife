package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * The one way this app names a group of rows. A section heading does nothing,
 * so it must not wear the accent colour -- that belongs to the things that do:
 * a switch that is on, a chip that is chosen, a button that acts.
 *
 * Caps are a bonus for the alphabets that have them. Arabic and Japanese have
 * no case at all, so the colour and the tracking carry the signal there, and
 * uppercasing simply passes the text through.
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
