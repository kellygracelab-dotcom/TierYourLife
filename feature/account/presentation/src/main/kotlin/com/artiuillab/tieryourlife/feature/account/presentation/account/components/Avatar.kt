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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * The photo is Google's, served from their CDN — nothing about it is stored here.
 * Without one the initial stands in, taken from the name rather than the email so
 * the letter always matches the word sitting next to it.
 */
@Composable
internal fun Avatar(photoUrl: String?, name: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name?.firstOrNull { it.isLetter() }?.uppercaseChar()
        when {
            !photoUrl.isNullOrBlank() -> AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
            )

            initial != null -> Text(
                text = initial.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            else -> PersonIcon(size / 2, MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private val AVATAR_LARGE = 88.dp

@Composable
internal fun ProfileAvatar(photoUrl: String?, name: String?, modifier: Modifier = Modifier) =
    Avatar(photoUrl, name, AVATAR_LARGE, modifier)
