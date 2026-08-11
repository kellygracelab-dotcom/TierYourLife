package com.artiuillab.tieryourlife.feature.tier.presentation.trash.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashTestTags

@Composable
internal fun RemoveConfirmDialog(entry: TrashEntry, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val body = when (entry) {
        is TrashEntry.DeletedList -> stringResource(
            R.string.remove_dialog_body_list,
            entry.title,
            pluralStringResource(R.plurals.list_items_count, entry.itemCount, entry.itemCount),
        )

        is TrashEntry.DeletedItem -> stringResource(R.string.remove_dialog_body_item, entry.title)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TrashTestTags.REMOVE_DIALOG),
        title = { Text(stringResource(R.string.remove_dialog_title), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(TrashTestTags.REMOVE_CONFIRM),
            ) { Text(stringResource(R.string.action_remove)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TrashTestTags.REMOVE_CANCEL),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun EmptyTrashDialog(entryCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val entryCountText = pluralStringResource(R.plurals.trash_entry_count, entryCount, entryCount)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TrashTestTags.EMPTY_TRASH_DIALOG),
        title = {
            Text(stringResource(R.string.empty_trash_dialog_title), style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(
                text = stringResource(R.string.empty_trash_dialog_body, entryCountText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(TrashTestTags.EMPTY_TRASH_CONFIRM),
            ) { Text(stringResource(R.string.action_empty_trash)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TrashTestTags.EMPTY_TRASH_CANCEL),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
