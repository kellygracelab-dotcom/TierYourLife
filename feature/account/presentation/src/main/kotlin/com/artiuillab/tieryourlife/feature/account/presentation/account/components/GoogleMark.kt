package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GOOGLE_BLUE = Color(0xFF4285F4)

/**
 * A stand-in, not Google's mark.
 *
 * The four-colour G is their trademark and their branding guidelines require
 * their own asset rather than a redrawing of it. Drop the official file in here
 * before release — the button around it already carries the size and spacing
 * those guidelines ask for.
 */
@Composable
internal fun GoogleMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "G",
            style = MaterialTheme.typography.labelLarge,
            color = GOOGLE_BLUE,
        )
    }
}
