package com.artiuillab.tieryourlife.feature.community.presentation.moderation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.PendingTopic
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.TopicQueueTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import kotlinx.coroutines.delay
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private const val TOPIC_NAME_MAX_LENGTH = 40
private const val SEARCH_DELAY_MILLIS = 300L

@Composable
internal fun RenameTopicDialog(name: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var typed by rememberSaveable { mutableStateOf(name) }
    val clean = typed.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TopicQueueTestTags.RENAME_DIALOG),
        title = { Text(stringResource(R.string.moderation_rename_title)) },
        text = {
            OutlinedTextField(
                value = typed,
                onValueChange = { if (it.length <= TOPIC_NAME_MAX_LENGTH) typed = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TopicQueueTestTags.RENAME_FIELD),
                label = { Text(stringResource(R.string.moderation_rename_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(clean) },
                enabled = clean.length > 1 && clean != name,
                modifier = Modifier.testTag(TopicQueueTestTags.RENAME_CONFIRM),
            ) { Text(stringResource(R.string.moderation_rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(ThemeR.string.action_cancel)) }
        },
    )
}

/**
 * The twins the queue already named come first; a search finds the rest. One
 * tap on a candidate merges: the dialog is the confirmation.
 */
@Composable
internal fun MergeTopicDialog(
    pending: PendingTopic,
    candidates: List<Topic>,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onMerge: (Topic) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(SEARCH_DELAY_MILLIS)
        onSearch(query)
    }
    // What the queue offered, then what the search found, without the same topic twice.
    val offered = (pending.similar + candidates).distinctBy { it.id }.filterNot { it.id == pending.topic.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TopicQueueTestTags.MERGE_DIALOG),
        title = { Text(stringResource(R.string.moderation_merge_into, pending.topic.name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TopicQueueTestTags.MERGE_FIELD),
                    label = { Text(stringResource(R.string.moderation_merge_search_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                offered.forEach { topic ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onMerge(topic) }
                            .testTag(TopicQueueTestTags.mergeCandidate(topic.id))
                            .padding(vertical = 10.dp),
                    ) {
                        Text(
                            text = topic.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        topic.section?.let { section ->
                            Text(
                                text = stringResource(section.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(ThemeR.string.action_cancel)) }
        },
    )
}
