package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags

internal const val NICKNAME_MAX_LENGTH = 24

@Composable
internal fun NicknameDialog(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(current.take(NICKNAME_MAX_LENGTH)) }
    val trimmed = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_nickname_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.account_nickname_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= NICKNAME_MAX_LENGTH) name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AccountTestTags.NICKNAME_FIELD),
                    label = { Text(stringResource(R.string.account_nickname_field)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    supportingText = {
                        Text(
                            stringResource(R.string.account_nickname_counter, name.length, NICKNAME_MAX_LENGTH),
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.account_nickname_snapshot_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed) },
                enabled = trimmed.isNotEmpty(),
                modifier = Modifier.testTag(AccountTestTags.NICKNAME_SAVE),
            ) {
                Text(stringResource(R.string.account_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.account_action_cancel)) }
        },
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun NicknameDialogPreview() = TierYourLifeTheme {
    NicknameDialog(current = "Danylo", onDismiss = {}, onSave = {})
}
