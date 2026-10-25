package com.artiuillab.tieryourlife.feature.community.presentation.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeedTestTags

private val FACE_SIZE = 36.dp
private const val FACES_SHOWN = 8

/**
 * Who the following feed is made of, above it: a row of faces and a way to
 * the list that unfollows. The faces are a reminder, not buttons; the list
 * is where a person is acted on.
 */
@Composable
internal fun FollowedStrip(followed: List<FollowedAuthor>, onManage: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag(CommunityFeedTestTags.FOLLOWED_STRIP),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.community_you_follow, followed.size),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onManage != null) {
                TextButton(onClick = onManage, modifier = Modifier.testTag(CommunityFeedTestTags.MANAGE_FOLLOWING)) {
                    Text(stringResource(R.string.community_manage_following))
                }
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            followed.take(FACES_SHOWN).forEach { person ->
                AuthorFace(photoUrl = person.photoUrl, name = person.name.ifEmpty { null }, size = FACE_SIZE)
            }
            if (followed.size > FACES_SHOWN) {
                Box(
                    Modifier
                        .size(FACE_SIZE)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+${followed.size - FACES_SHOWN}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
