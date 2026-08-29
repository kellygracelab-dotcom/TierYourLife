package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CategoryIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Mild to sharp, in that order. Hiding sits next to reporting on purpose: most
 * of the time "I would rather not see this" is not an accusation, and making
 * people file one to get it off their screen is how a report queue fills with
 * things nobody meant to report.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListActionsSheet(
    title: String,
    authorName: String,
    authorPhotoUrl: String?,
    onDismiss: () -> Unit,
    onOpenAuthor: (() -> Unit)?,
    onHide: () -> Unit,
    onReport: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .testTag(TierListsTestTags.LIST_ACTIONS_SHEET),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuthorFace(photoUrl = authorPhotoUrl, name = authorName, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.community_by_author, authorName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            if (onOpenAuthor != null) {
                ActionRow(
                    text = stringResource(R.string.community_action_view_author, authorName),
                    testTag = TierListsTestTags.ACTION_VIEW_AUTHOR,
                    onClick = onOpenAuthor,
                )
            }
            ActionRow(
                text = stringResource(R.string.community_action_hide),
                testTag = TierListsTestTags.ACTION_HIDE,
                onClick = onHide,
            )
            ActionRow(
                text = stringResource(R.string.community_action_report),
                testTag = TierListsTestTags.ACTION_REPORT,
                onClick = onReport,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActionRow(
    text: String,
    testTag: String,
    onClick: () -> Unit,
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
        CategoryIcon(20.dp, color)
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
