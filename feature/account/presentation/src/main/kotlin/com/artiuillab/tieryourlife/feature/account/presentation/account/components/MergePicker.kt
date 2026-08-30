package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags
import com.artiuillab.tieryourlife.feature.account.presentation.account.MergeKeep
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice

private val BUTTON_HEIGHT = 52.dp

/**
 * Asked once, and only in the case that genuinely has two answers: somebody
 * built boards without an account, then signed into one they had used before.
 * An account with nothing on it takes this phone's boards silently, because
 * there is nothing to weigh them against.
 *
 * Neither option deletes anything, and the footnote says so out loud. The
 * difference is only which set is in use afterwards -- the other one is either
 * renamed or in the trash, and both can be walked back.
 */
@Composable
internal fun MergePicker(
    choice: MergeChoice,
    keep: MergeKeep,
    onKeepChange: (MergeKeep) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag(AccountTestTags.MERGE)) {
        Text(
            text = stringResource(R.string.merge_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.merge_body,
                boards(choice.accountBoards),
                boards(choice.localBoards),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Option(
                title = stringResource(R.string.merge_keep_title),
                supporting = stringResource(R.string.merge_keep_sub, boards(choice.total)),
                recommended = true,
                selected = keep == MergeKeep.Everything,
                testTag = AccountTestTags.MERGE_KEEP_EVERYTHING,
                onSelect = { onKeepChange(MergeKeep.Everything) },
            )
            Option(
                title = stringResource(R.string.merge_remote_title),
                supporting = stringResource(R.string.merge_remote_sub, boards(choice.localBoards)),
                recommended = false,
                selected = keep == MergeKeep.AccountOnly,
                testTag = AccountTestTags.MERGE_ACCOUNT_ONLY,
                onSelect = { onKeepChange(MergeKeep.AccountOnly) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.merge_footnote),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .testTag(AccountTestTags.MERGE_CONTINUE),
        ) {
            Text(stringResource(R.string.merge_action_continue))
        }
    }
}

@Composable
private fun Option(
    title: String,
    supporting: String,
    recommended: Boolean,
    selected: Boolean,
    testTag: String,
    onSelect: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .testTag(testTag),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = selected, onClick = null)
            Column(Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (recommended) {
                        Spacer(Modifier.width(8.dp))
                        RecommendedBadge()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecommendedBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.merge_recommended),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun boards(count: Int): String =
    pluralStringResource(R.plurals.account_board_count, count, count)
