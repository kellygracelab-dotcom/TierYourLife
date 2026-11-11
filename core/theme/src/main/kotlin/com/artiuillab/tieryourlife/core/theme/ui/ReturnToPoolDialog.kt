package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.R

@Composable
fun ReturnToPoolDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("community_return_to_pool_dialog"),
        title = { Text(stringResource(R.string.community_return_to_pool_title)) },
        text = { Text(stringResource(R.string.community_return_to_pool_explanation)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("community_confirm_return_to_pool")) {
                Text(stringResource(R.string.community_return_to_pool_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
