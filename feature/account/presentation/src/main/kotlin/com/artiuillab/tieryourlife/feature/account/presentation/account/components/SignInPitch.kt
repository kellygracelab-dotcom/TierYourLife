package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags

private val BUTTON_HEIGHT = 52.dp

private val REASONS = listOf(
    R.string.account_reason_devices,
    R.string.account_reason_purchases,
    R.string.account_reason_publish,
)

@Composable
internal fun SignInPitch(
    signingIn: Boolean,
    boardCount: Int,
    backUpBoards: Boolean,
    onBackUpBoardsChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.account_sign_in_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.account_optional),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            REASONS.forEachIndexed { index, reason ->
                Reason(text = stringResource(reason), testTag = AccountTestTags.reason(index))
            }
        }

        // Asked here rather than afterwards in Settings, because this is the
        // moment somebody is deciding what an account is for. Turned off now
        // it simply never starts -- nothing has gone up yet, so there is
        // nothing to warn about and nothing to undo.
        Spacer(Modifier.height(28.dp))
        BackUpSwitch(
            boardCount = boardCount,
            checked = backUpBoards,
            onCheckedChange = onBackUpBoardsChange,
        )

        Spacer(Modifier.height(28.dp))
        FilledTonalButton(
            onClick = onSignIn,
            enabled = !signingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .testTag(AccountTestTags.SIGN_IN),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            GoogleMark()
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.account_action_sign_in_google))
        }
        TextButton(
            onClick = onNotNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .testTag(AccountTestTags.NOT_NOW),
        ) {
            Text(stringResource(R.string.account_action_not_now))
        }
    }
}

@Composable
private fun BackUpSwitch(boardCount: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag(AccountTestTags.BACK_UP_BOARDS)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = stringResource(R.string.account_backup_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.account_backup_sub,
                    pluralStringResource(R.plurals.account_board_count, boardCount, boardCount),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Reason(text: String, testTag: String) {
    Row(modifier = Modifier.testTag(testTag), verticalAlignment = Alignment.Top) {
        CheckIcon(20.dp, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            modifier = Modifier.padding(top = 1.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
