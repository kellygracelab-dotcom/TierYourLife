package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.feature.tier.presentation.R

/**
 * Says what actually happened and what did not. The offer underneath is the one
 * that matters when someone is being followed around: it hides everything from
 * that person, here, without telling them.
 */
@Composable
internal fun ReportSentDialog(
    authorName: String,
    onDismiss: () -> Unit,
    onHideAuthor: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_sent_title)) },
        text = { Text(stringResource(R.string.report_sent_body)) },
        confirmButton = {
            TextButton(onClick = onHideAuthor) {
                Text(stringResource(R.string.report_hide_author, authorName))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.community_hidden)) }
        },
    )
}
