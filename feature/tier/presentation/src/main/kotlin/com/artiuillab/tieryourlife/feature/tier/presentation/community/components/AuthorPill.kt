package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ChevronRightIcon

private val PILL_HEIGHT = 32.dp
private val FACE_SIZE = 18.dp
private val MIN_TOUCH_TARGET = 48.dp

/**
 * One author, drawn the same way wherever they appear, and always a way into
 * their profile. Plain text was unreachable — nobody taps a caption.
 */
@Composable
internal fun AuthorPill(
    name: String,
    photoUrl: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    testTag: String? = null,
) {
    val shape = CircleShape
    val tagged = if (testTag == null) modifier else modifier.testTag(testTag)
    Box(
        tagged.heightIn(min = if (onClick == null) PILL_HEIGHT else MIN_TOUCH_TARGET),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier
                .height(PILL_HEIGHT)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
                .padding(start = 7.dp, end = if (onClick == null) 12.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuthorFace(photoUrl = photoUrl, name = name, size = FACE_SIZE)
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                modifier = Modifier.widthIn(max = 180.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (onClick != null) {
                ChevronRightIcon(14.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A photo when there is one, the initial when there is not. */
@Composable
internal fun AuthorFace(photoUrl: String?, name: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name?.firstOrNull { it.isLetter() }?.uppercaseChar()
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else if (initial != null) {
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
