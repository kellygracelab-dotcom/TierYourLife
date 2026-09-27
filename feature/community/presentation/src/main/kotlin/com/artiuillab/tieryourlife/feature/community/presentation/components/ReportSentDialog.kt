package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.feature.community.presentation.R

/**
 * Says what actually happened and what did not. The offer underneath is the one
 * that matters when someone is being followed around: it hides everything from
 * that person, here, without telling them.
 */
@Composable
fun ReportSentDialog(
    /** Null for a list that stands without an author: there is nobody to hide wholesale. */
    authorName: String?,
    onDismiss: () -> Unit,
    onHideAuthor: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_sent_title)) },
        text = { Text(stringResource(R.string.report_sent_body)) },
        confirmButton = {
            if (authorName != null) {
                TextButton(onClick = onHideAuthor) {
                    Text(stringResource(R.string.report_hide_author, authorName))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
            }
        },
        dismissButton = {
            if (authorName != null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
            }
        },
    )
}
