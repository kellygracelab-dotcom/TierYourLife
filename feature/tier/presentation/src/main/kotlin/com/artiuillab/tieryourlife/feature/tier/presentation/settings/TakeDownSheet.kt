package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.presentation.R

/** Tall enough that nothing lands beside "forever" by accident. */
private val CHOICE_HEIGHT = 52.dp

/**
 * One sheet, not two steps: the list going and the person answering for it
 * are one decision. Forever sits below a rule, in the colour of a mistake,
 * and asks again -- the only choice here with no end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TakeDownSheet(
    authorName: String,
    busy: Boolean,
    onTakeDown: (BanLength?) -> Unit,
    onDismiss: () -> Unit,
) {
    // A month rather than nothing: a gentle default makes a slipped
    // double-tap the least damaging thing it could be.
    var chosen by remember { mutableStateOf<BanLength?>(BanLength.Month) }
    var askingForever by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag(ModerationTestTags.TAKE_DOWN_SHEET)) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.moderation_take_down_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.moderation_take_down_body),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FINITE.forEach { length ->
                Choice(
                    label = stringResource(length.labelRes),
                    selected = chosen == length,
                    testTag = ModerationTestTags.banChoice(length),
                ) { chosen = length }
            }
            Choice(
                label = stringResource(R.string.ban_none),
                selected = chosen == null,
                testTag = ModerationTestTags.BAN_NONE,
            ) { chosen = null }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Its own row and colour: nothing about it reachable by aiming at something else.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(CHOICE_HEIGHT)
                    .clickable(enabled = !busy) { askingForever = true }
                    .testTag(ModerationTestTags.BAN_FOREVER),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.ban_forever),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.ban_forever_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = { onTakeDown(chosen) },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag(ModerationTestTags.TAKE_DOWN_CONFIRM),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                // The button says what will happen.
                Text(
                    text = chosen?.let {
                        stringResource(R.string.moderation_take_down_with, stringResource(it.labelRes))
                    } ?: stringResource(R.string.moderation_action_take_down),
                )
            }
        }
    }

    if (askingForever) {
        AlertDialog(
            onDismissRequest = { askingForever = false },
            modifier = Modifier.testTag(ModerationTestTags.BAN_FOREVER_DIALOG),
            title = { Text(stringResource(R.string.ban_forever_confirm_title, authorName)) },
            // A decision without an end has to be readable in full.
            text = { Text(stringResource(R.string.ban_forever_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        askingForever = false
                        onTakeDown(BanLength.Forever)
                    },
                    modifier = Modifier.testTag(ModerationTestTags.BAN_FOREVER_CONFIRM),
                ) {
                    Text(
                        text = stringResource(R.string.ban_forever_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { askingForever = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, testTag: String, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(CHOICE_HEIGHT)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Everything but forever, which never appears beside them. */
private val FINITE = listOf(
    BanLength.Week,
    BanLength.Month,
    BanLength.ThreeMonths,
    BanLength.SixMonths,
)

internal val BanLength.labelRes: Int
    get() = when (this) {
        BanLength.Week -> R.string.ban_week
        BanLength.Month -> R.string.ban_month
        BanLength.ThreeMonths -> R.string.ban_three_months
        BanLength.SixMonths -> R.string.ban_six_months
        BanLength.Forever -> R.string.ban_forever
    }
