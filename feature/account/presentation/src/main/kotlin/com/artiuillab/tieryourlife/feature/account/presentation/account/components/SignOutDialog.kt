package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags

/**
 * Asked only because the boards are kept somewhere now.
 *
 * Two paragraphs rather than one sentence, because they answer two different
 * fears. The first is "am I about to lose my work" -- no, it stays in both
 * places, and only what happens next stops travelling. The second is "am I
 * about to lose what I paid for", and it is short because the answer is
 * simply no.
 */
@Composable
internal fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(AccountTestTags.SIGN_OUT_DIALOG),
        title = { Text(stringResource(R.string.account_sign_out_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.account_sign_out_body_1),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.account_sign_out_body_2),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        // Cancel first, and the leaving one in error: the dialog should not
        // read as a nudge towards the door.
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(AccountTestTags.SIGN_OUT_CONFIRM),
            ) {
                Text(
                    text = stringResource(R.string.account_action_sign_out),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.account_action_cancel))
            }
        },
    )
}
