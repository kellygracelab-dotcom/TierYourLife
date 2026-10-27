package com.artiuillab.tieryourlife.feature.community.presentation.explore

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R

/** Under the tabs, when the last fetch failed and what shows is over an hour old (§2.6). */
@Composable
internal fun OfflineStrip(sinceMillis: Long?, nowMillis: Long, retrying: Boolean, onRetry: () -> Unit) {
    AnimatedVisibility(
        visible = sinceMillis != null,
        enter = slideInVertically(tween(200)) { -it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200)),
    ) {
        val since = sinceMillis ?: return@AnimatedVisibility
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(start = 16.dp, end = 4.dp)
                .testTag(ExploreTestTags.OFFLINE_STRIP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CloudOffIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            val ago = DateUtils.getRelativeTimeSpanString(since, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
            Text(
                text = stringResource(R.string.explore_offline, ago),
                modifier = Modifier.weight(1f),
                style = Body13Style,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onRetry, enabled = !retrying) { Text(stringResource(R.string.explore_retry)) }
        }
    }
}

/** No list kept and none arrived (§2.7): what happened, and the one thing to do. Tiles above still work. */
@Composable
internal fun FeedFailedBlock(retrying: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 16.dp, end = 16.dp)
            .testTag(ExploreTestTags.FAILED),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CloudOffIcon(20.dp, MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.explore_failed_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            stringResource(R.string.explore_failed_body),
            modifier = Modifier.padding(top = 6.dp),
            style = Body13Style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onRetry, enabled = !retrying, modifier = Modifier.padding(top = 8.dp)) {
            RefreshIcon(18.dp, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.explore_try_again))
        }
    }
}

/** Newer lists waiting while the person reads further down (§2.5). */
@Composable
internal fun NewListsPill(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(200)) { -it / 5 } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(200)) { -it / 5 } + fadeOut(tween(200)),
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 3.dp,
            modifier = Modifier
                .height(40.dp)
                .testTag(ExploreTestTags.NEW_LISTS_PILL),
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ArrowUpwardIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.explore_new_lists), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** The three sizes the follow button comes in (§1.6). */
internal enum class FollowSize(val height: Dp, val minWidth: Dp) {
    Regular(RegularHeight, RegularMinWidth),
    Topic(TopicHeight, TopicMinWidth),
    Compact(CompactHeight, 0.dp),
}

private val RegularHeight = 40.dp
private val RegularMinWidth = 120.dp
private val TopicHeight = 36.dp
private val TopicMinWidth = 104.dp
private val CompactHeight = 32.dp

/** Follow, tonal; Following, outlined with a check. Both answer at once; the caller reverts on failure. */
@Composable
internal fun FollowToggle(
    following: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: FollowSize = FollowSize.Regular,
) {
    val sized = modifier
        .height(size.height)
        .widthIn(min = size.minWidth)
    val padding = PaddingValues(horizontal = if (size == FollowSize.Compact) 12.dp else 20.dp)
    val label = if (size == FollowSize.Regular) MaterialTheme.typography.titleSmall else Body13Style.copy(fontWeight = FontWeight.Medium)
    if (following) {
        OutlinedButton(
            onClick = onClick,
            modifier = sized.testTag(ExploreTestTags.FOLLOWING_BUTTON),
            contentPadding = padding,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            CheckIcon(if (size == FollowSize.Compact) 16.dp else 18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.explore_following), style = label, maxLines = 1)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = sized.testTag(ExploreTestTags.FOLLOW_BUTTON),
            contentPadding = padding,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(stringResource(R.string.explore_follow), style = label, maxLines = 1)
        }
    }
}
