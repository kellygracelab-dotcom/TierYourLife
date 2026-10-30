package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.ui.CategoryIcon
import com.artiuillab.tieryourlife.core.theme.ui.DeleteOutlineIcon
import com.artiuillab.tieryourlife.core.theme.ui.FlagIcon
import com.artiuillab.tieryourlife.core.theme.ui.FormatListNumberedIcon
import com.artiuillab.tieryourlife.core.theme.ui.GridViewIcon
import com.artiuillab.tieryourlife.core.theme.ui.HideIcon
import com.artiuillab.tieryourlife.core.theme.ui.LeaderboardIcon
import com.artiuillab.tieryourlife.core.theme.ui.PersonIcon
import com.artiuillab.tieryourlife.core.theme.ui.VisibilityIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R

/**
 * Mild to sharp, in that order. Hiding sits next to reporting on purpose: most
 * of the time "I would rather not see this" is not an accusation, and making
 * people file one to get it off their screen is how a report queue fills with
 * things nobody meant to report.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListActionsSheet(
    title: String,
    /** Null for a list that stands without an author: no face, no byline. */
    authorName: String?,
    authorPhotoUrl: String?,
    onDismiss: () -> Unit,
    onOpenAuthor: (() -> Unit)?,
    /** Null on this person's own list, as is [onReport]: neither means anything there. */
    onHide: (() -> Unit)?,
    onReport: (() -> Unit)?,
    /** On a topic's page: the list is fine, it is just not of this topic. Nothing hides; a moderator moves it. */
    onWrongTopic: (() -> Unit)? = null,
    /** On somebody else's list the reader has arranged: show their ranking to others, or, for a guest, sign in to. */
    onShowMyRanking: (() -> Unit)? = null,
    signInToShowRanking: Boolean = false,
    /** On this person's own list: the list of all their public ones. */
    onOpenMyPublished: (() -> Unit)? = null,
    /** On their own list with its board on this phone: out again without the name, or with it when [anonymous]. */
    onPublishAnonymously: (() -> Unit)? = null,
    anonymous: Boolean = false,
    /** On their own list: down for everyone. */
    onTakeDown: (() -> Unit)? = null,
    /** On a list's page, first: the readers' placings of it, and how many there are. */
    onOpenRankings: (() -> Unit)? = null,
    rankingCount: Int = 0,
) {
    AdaptiveSheet(
        onDismiss = onDismiss,
        width = SheetWidth.Choosing,
        maxHeight = 560.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .testTag(CommunityTestTags.LIST_ACTIONS_SHEET),
        ) {
            SheetHead(title = title, authorName = authorName, authorPhotoUrl = authorPhotoUrl)

            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            if (onOpenRankings != null) {
                ActionRow(
                    text = stringResource(R.string.community_action_rankings, rankingCount),
                    testTag = CommunityTestTags.ACTION_RANKINGS,
                    onClick = onOpenRankings,
                    icon = { size, tint -> LeaderboardIcon(size, tint) },
                )
            }
            if (onOpenAuthor != null && authorName != null) {
                ActionRow(
                    text = stringResource(R.string.community_action_view_author, authorName),
                    testTag = CommunityTestTags.ACTION_VIEW_AUTHOR,
                    onClick = onOpenAuthor,
                    icon = { size, tint -> PersonIcon(size, tint) },
                )
            }
            if (onWrongTopic != null) {
                ActionRow(
                    text = stringResource(R.string.community_action_not_this_topic),
                    testTag = CommunityTestTags.ACTION_WRONG_TOPIC,
                    onClick = onWrongTopic,
                    icon = { size, tint -> CategoryIcon(size, tint) },
                )
            }
            if (onShowMyRanking != null) {
                ActionRow(
                    text = stringResource(if (signInToShowRanking) R.string.community_sign_in_to_show_ranking else R.string.community_show_my_ranking),
                    testTag = CommunityTestTags.ACTION_SHOW_RANKING,
                    onClick = onShowMyRanking,
                    icon = { size, tint -> FormatListNumberedIcon(size, tint) },
                )
            }
            if (onOpenMyPublished != null) {
                ActionRow(
                    text = stringResource(R.string.my_published_title),
                    testTag = CommunityTestTags.ACTION_YOUR_PUBLIC_LISTS,
                    onClick = onOpenMyPublished,
                    icon = { size, tint -> GridViewIcon(size, tint) },
                )
            }
            if (onPublishAnonymously != null) {
                ActionRow(
                    text = stringResource(if (anonymous) R.string.community_action_publish_named else R.string.community_action_publish_anonymously),
                    testTag = CommunityTestTags.ACTION_PUBLISH_ANONYMOUSLY,
                    onClick = onPublishAnonymously,
                    icon = { size, tint -> VisibilityIcon(size, tint, off = !anonymous) },
                )
            }
            if (onTakeDown != null) {
                ActionRow(
                    text = stringResource(R.string.moderation_action_take_down),
                    testTag = CommunityTestTags.ACTION_TAKE_DOWN,
                    onClick = onTakeDown,
                    icon = { size, tint -> DeleteOutlineIcon(size, tint) },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (onHide != null) {
                ActionRow(
                    text = stringResource(R.string.community_action_hide),
                    testTag = CommunityTestTags.ACTION_HIDE,
                    onClick = onHide,
                    icon = { size, tint -> HideIcon(size, tint) },
                )
            }
            if (onReport != null) {
                ActionRow(
                    text = stringResource(R.string.community_action_report),
                    testTag = CommunityTestTags.ACTION_REPORT,
                    onClick = onReport,
                    icon = { size, tint -> FlagIcon(size, tint) },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** The list's title over the sheet, with its author's face and name when it has an author. */
@Composable
private fun SheetHead(title: String, authorName: String?, authorPhotoUrl: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (authorName != null) {
            AuthorFace(photoUrl = authorPhotoUrl, name = authorName, size = 32.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (authorName != null) {
                Text(
                    text = stringResource(R.string.community_by_author, authorName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    text: String,
    testTag: String,
    onClick: () -> Unit,
    icon: @Composable (Dp, Color) -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(20.dp, color)
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
