package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Following, from somebody who follows nobody yet.
 *
 * A screen rather than a wall: hiding the tab from a guest would be the only
 * way to never tell anybody the feature exists. What it needs to show is who
 * there is, so the line of explanation sits above a list of people rather than
 * alone in the middle of an empty page.
 */
@Composable
internal fun FollowingNobody(
    authors: List<SuggestedAuthor>,
    loading: Boolean,
    followed: Set<String>,
    onFollow: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().testTag(TierListsTestTags.FOLLOWING_NOBODY)) {
        Text(
            text = stringResource(R.string.home_following_nobody),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        Text(
            text = stringResource(R.string.home_following_nobody_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(authors, key = { it.uid }) { author ->
                AuthorRow(
                    author = author,
                    following = author.uid in followed,
                    onFollow = { onFollow(author.uid) },
                    onOpen = { onOpenAuthor(author.uid) },
                )
            }
        }
    }
}

@Composable
private fun AuthorRow(
    author: SuggestedAuthor,
    following: Boolean,
    onFollow: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag(TierListsTestTags.SUGGESTED_AUTHOR),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Face(author)
            Column(Modifier.weight(1f)) {
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.tier_lists_rankings_count,
                        author.takeCount,
                        author.takeCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Filled while it is an offer, outlined once it has been taken up:
            // the row stays where it is, so the change of shape is the only
            // thing saying the tap landed.
            if (following) {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.action_following))
                }
            } else {
                Button(onClick = onFollow) {
                    Text(stringResource(R.string.action_follow))
                }
            }
        }
    }
}

@Composable
private fun Face(author: SuggestedAuthor) {
    val shape = CircleShape
    if (author.photoUrl == null) {
        Surface(
            modifier = Modifier.size(40.dp).clip(shape),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = shape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = author.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        return
    }
    AsyncImage(
        model = author.photoUrl,
        contentDescription = null,
        modifier = Modifier.size(40.dp).clip(shape),
    )
}
