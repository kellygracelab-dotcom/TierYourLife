package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityTestTags

/**
 * Whose list this is, and the one thing a reader can do about it.
 *
 * The second of the two places following lives. The profile is where somebody
 * decides, having seen everything an author has; this is the moment they most
 * want to and have least reason to leave the screen for.
 *
 * Directly under the board's own bar, and pinned there. Above it the bar's
 * status-bar inset would be over its head, and the reading order would run
 * whose it is before what it is. It stays put while the board scrolls because
 * it is the only mark that the board is not yours, and the moment that most
 * needs saying is the one where you are halfway through rearranging it.
 */
@Composable
internal fun AuthorLine(
    name: String,
    photoUrl: String?,
    follow: FollowState?,
    onOpenAuthor: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AUTHOR_LINE_HEIGHT)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AuthorFace(photoUrl = photoUrl, name = name, size = 24.dp)
        Text(
            text = stringResource(R.string.community_by_author, name),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenAuthor),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Nothing at all until the server has answered. A chip here before
        // then would offer to undo something nobody has done.
        if (follow != null) {
            AssistChip(
                onClick = onToggleFollow,
                label = {
                    Text(
                        stringResource(
                            if (follow.following) R.string.action_following else R.string.action_follow,
                        ),
                    )
                },
                colors = if (follow.following) {
                    AssistChipDefaults.assistChipColors()
                } else {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                },
                modifier = Modifier.testTag(CommunityTestTags.FOLLOW),
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private val AUTHOR_LINE_HEIGHT = 56.dp
