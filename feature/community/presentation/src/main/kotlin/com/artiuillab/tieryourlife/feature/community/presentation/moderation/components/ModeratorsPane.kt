package com.artiuillab.tieryourlife.feature.community.presentation.moderation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.Moderator
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.ModeratorsUiState
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.TopicQueueTestTags
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val FACE_SIZE = 36.dp

/**
 * The owner's list of who else decides. Appointing takes an account id typed
 * or pasted as text; a face and a name come with the account's next list.
 */
@Composable
internal fun ModeratorsPane(
    state: ModeratorsUiState,
    onAppoint: (uid: String, name: String) -> Unit,
    onRemove: (uid: String) -> Unit,
    onRetry: () -> Unit,
) {
    var appointing by rememberSaveable { mutableStateOf(false) }

    when (state) {
        ModeratorsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        ModeratorsUiState.Unavailable -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.moderation_moderators_unavailable), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry, modifier = Modifier.testTag(TopicQueueTestTags.MODERATORS_RETRY)) {
                    Text(stringResource(ThemeR.string.action_try_again))
                }
            }
        }

        is ModeratorsUiState.Ready -> Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { appointing = true },
                    enabled = !state.busy,
                    modifier = Modifier.testTag(TopicQueueTestTags.APPOINT),
                ) { Text(stringResource(R.string.moderation_appoint)) }
            }
            if (state.moderators.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp)
                        .testTag(TopicQueueTestTags.MODERATORS_EMPTY),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.moderation_moderators_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(state.moderators, key = { it.uid }) { moderator ->
                        ModeratorRow(moderator = moderator, busy = state.busy, onRemove = { onRemove(moderator.uid) })
                    }
                }
            }
        }
    }

    if (appointing) {
        AppointModeratorDialog(
            onDismiss = { appointing = false },
            onAppoint = { uid, name ->
                appointing = false
                onAppoint(uid, name)
            },
        )
    }
}

@Composable
private fun ModeratorRow(moderator: Moderator, busy: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TopicQueueTestTags.moderatorRow(moderator.uid)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthorFace(photoUrl = moderator.photoUrl, name = moderator.name.ifEmpty { null }, size = FACE_SIZE)
        Column(Modifier.weight(1f)) {
            Text(
                text = moderator.name.ifEmpty { stringResource(R.string.community_author_unnamed) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The id is what was typed to appoint them, so it is what tells two namesakes apart.
            Text(
                text = moderator.uid,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(
            onClick = onRemove,
            enabled = !busy,
            modifier = Modifier.testTag(TopicQueueTestTags.removeModerator(moderator.uid)),
        ) { Text(stringResource(R.string.moderation_remove_moderator)) }
    }
}

@Composable
private fun AppointModeratorDialog(onDismiss: () -> Unit, onAppoint: (uid: String, name: String) -> Unit) {
    var uid by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TopicQueueTestTags.APPOINT_DIALOG),
        title = { Text(stringResource(R.string.moderation_appoint_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TopicQueueTestTags.APPOINT_UID),
                    label = { Text(stringResource(R.string.moderation_appoint_uid)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TopicQueueTestTags.APPOINT_NAME),
                    label = { Text(stringResource(R.string.moderation_appoint_name)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAppoint(uid.trim(), name.trim()) },
                enabled = uid.isNotBlank(),
                modifier = Modifier.testTag(TopicQueueTestTags.APPOINT_CONFIRM),
            ) { Text(stringResource(R.string.moderation_appoint)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(ThemeR.string.action_cancel)) }
        },
    )
}
