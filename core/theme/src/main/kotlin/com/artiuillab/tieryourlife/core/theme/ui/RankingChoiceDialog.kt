package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.R

/** Both actions keep progress; public visibility always requires the explicit action. */
@Composable
fun RankingChoiceDialog(
    placed: Int,
    total: Int,
    signedIn: Boolean,
    busy: Boolean,
    onPrivate: () -> Unit,
    onPublic: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("ranking_choice_dialog"),
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.ranking_choice_title)) },
        text = {
            Text(
                pluralStringResource(R.plurals.ranking_choice_body, total, placed, total) + "\n\n" +
                    stringResource(if (signedIn) R.string.ranking_choice_storage_account else R.string.ranking_choice_storage_guest),
            )
        },
        confirmButton = {
            TextButton(onClick = onPublic, enabled = !busy, modifier = Modifier.testTag("ranking_choice_public")) {
                Text(stringResource(if (signedIn) R.string.ranking_choice_public else R.string.ranking_choice_sign_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onPrivate, enabled = !busy, modifier = Modifier.testTag("ranking_choice_private")) {
                Text(stringResource(R.string.ranking_choice_private))
            }
        },
    )
}
