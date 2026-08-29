package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

internal const val REPORT_NOTE_MAX_LENGTH = 500

/**
 * The expectation is set before the button is pressed, not after: one person
 * reads these by hand, and a promise of removal we cannot keep is worse than
 * saying so plainly.
 */
@Composable
internal fun ReportDialog(
    onDismiss: () -> Unit,
    onSend: (ReportReason, String?) -> Unit,
) {
    var reason by remember { mutableStateOf<ReportReason?>(null) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_title)) },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag(TierListsTestTags.REPORT_DIALOG),
            ) {
                Column(Modifier.selectableGroup()) {
                    ReportReason.entries.forEach { option ->
                        ReasonRow(
                            option = option,
                            selected = option == reason,
                            onSelect = { reason = option },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Always visible rather than unfolding on "Something else": a
                // field that appears reads as a demand for justification.
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= REPORT_NOTE_MAX_LENGTH) note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TierListsTestTags.REPORT_NOTE),
                    label = { Text(stringResource(R.string.report_note_label)) },
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.report_by_hand),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { reason?.let { onSend(it, note.trim().takeIf(String::isNotEmpty)) } },
                enabled = reason != null,
                modifier = Modifier.testTag(TierListsTestTags.REPORT_SEND),
            ) {
                Text(stringResource(R.string.report_action_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ReasonRow(option: ReportReason, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag(TierListsTestTags.reportReason(option))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(option.labelRes()),
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.Sexual -> R.string.report_reason_sexual
    ReportReason.Violence -> R.string.report_reason_violence
    ReportReason.Hate -> R.string.report_reason_hate
    ReportReason.Spam -> R.string.report_reason_spam
    ReportReason.Other -> R.string.report_reason_other
}

@TierYourLifeDevicePreviews
@Composable
private fun ReportDialogPreview() = TierYourLifeTheme {
    ReportDialog(onDismiss = {}, onSend = { _, _ -> })
}
