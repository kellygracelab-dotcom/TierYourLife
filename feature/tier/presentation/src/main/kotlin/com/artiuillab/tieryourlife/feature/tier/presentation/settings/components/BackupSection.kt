package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsTestTags

/**
 * Two switches, a size, and one line that only appears when something is
 * wrong.
 *
 * There is no status here on purpose. A screen that says "backed up · 2
 * minutes ago" teaches people to come and check, which is exactly what a
 * working backup should never need. The reverse holds though: silence while
 * nothing has gone up for a week is the silence that costs somebody their
 * boards, so that one line is worth the interruption.
 */
@Composable
internal fun BackupSection(
    settings: BackupSettings,
    stuckSince: String?,
    onBackUpChange: (Boolean) -> Unit,
    onPicturesOnWifiOnlyChange: (Boolean) -> Unit,
) {
    Column(Modifier.testTag(SettingsTestTags.BACKUP_ROW)) {
        SwitchRow(
            label = stringResource(R.string.settings_backup_label),
            supporting = stringResource(R.string.settings_backup_sub),
            checked = settings.on,
            testTag = SettingsTestTags.BACKUP_SWITCH,
            onCheckedChange = onBackUpChange,
        )
        if (settings.on) {
            SwitchRow(
                label = stringResource(R.string.settings_backup_pictures_label),
                supporting = stringResource(
                    R.string.settings_backup_pictures_sub,
                    readableSize(settings.storedBytes),
                ),
                checked = settings.picturesOnWifiOnly,
                testTag = SettingsTestTags.BACKUP_PICTURES_SWITCH,
                onCheckedChange = onPicturesOnWifiOnlyChange,
            )
        }
        if (stuckSince != null) {
            Text(
                text = stringResource(R.string.settings_backup_stuck, stuckSince),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .testTag(SettingsTestTags.BACKUP_STUCK),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        // A privacy boundary rather than a description of a feature: what
        // leaves the phone, and where it never turns up.
        Text(
            text = stringResource(R.string.settings_backup_note),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            style = TierYourLifeType.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Asked only on the way off, and only because "off" has to mean the copy is
 * gone. Somebody turning this off has decided their boards are not going to
 * sit on somebody else's computer, and leaving them there is the opposite of
 * what they asked for.
 */
@Composable
internal fun StopBackingUpDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(SettingsTestTags.BACKUP_OFF_DIALOG),
        title = { Text(stringResource(R.string.settings_backup_off_title)) },
        text = { Text(stringResource(R.string.settings_backup_off_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(SettingsTestTags.BACKUP_OFF_CONFIRM),
            ) {
                Text(
                    text = stringResource(R.string.settings_backup_off_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_backup_off_cancel))
            }
        },
    )
}

@Composable
private fun SwitchRow(
    label: String,
    supporting: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag(testTag)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = supporting,
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Whole megabytes above a megabyte, because nobody reads the decimal and
 * "142 MB" is the number they are deciding about.
 */
internal fun readableSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    bytes > 0 -> "${(bytes + 1023) / 1024} KB"
    else -> "0 KB"
}
