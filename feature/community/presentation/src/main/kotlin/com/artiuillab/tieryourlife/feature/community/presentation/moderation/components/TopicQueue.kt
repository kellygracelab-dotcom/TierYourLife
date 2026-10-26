package com.artiuillab.tieryourlife.feature.community.presentation.moderation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.PendingTopic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.TopicQueueTestTags
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.TopicQueueUiState
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.CategorySheet
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/**
 * The words people proposed. Each card carries what a decision needs: who
 * asked, where they wanted it, and what it might be a twin of. Approving asks
 * for the section, since the proposer's wish is a wish; merging asks for the
 * topic it folds into; renaming leaves the card in the queue under its new name.
 */
@Composable
internal fun TopicQueue(
    state: TopicQueueUiState,
    onApprove: (id: String, ListCategory) -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onMerge: (id: String, into: String) -> Unit,
    onReject: (id: String) -> Unit,
    onFindMergeTargets: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var approving by remember { mutableStateOf<PendingTopic?>(null) }
    var renaming by remember { mutableStateOf<PendingTopic?>(null) }
    var merging by remember { mutableStateOf<PendingTopic?>(null) }

    when (state) {
        TopicQueueUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        TopicQueueUiState.Failed -> QueueMessage(
            title = stringResource(R.string.moderation_topics_failed),
            body = stringResource(R.string.home_community_failed_body),
            action = stringResource(ThemeR.string.action_try_again),
            onAction = onRetry,
        )

        is TopicQueueUiState.Ready -> if (state.pending.isEmpty()) {
            QueueMessage(
                title = stringResource(R.string.moderation_topics_empty_title),
                body = stringResource(R.string.moderation_topics_empty_body),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.pending, key = { it.topic.id }) { pending ->
                    PendingTopicCard(
                        pending = pending,
                        busy = state.settling != null,
                        onApprove = { approving = pending },
                        onRename = { renaming = pending },
                        onMerge = { merging = pending },
                        onReject = { onReject(pending.topic.id) },
                    )
                }
            }
        }
    }

    approving?.let { pending ->
        CategorySheet(
            selected = pending.requestedSection,
            onDismiss = { approving = null },
            onSelect = { section ->
                approving = null
                onApprove(pending.topic.id, section)
            },
        )
    }
    renaming?.let { pending ->
        RenameTopicDialog(
            name = pending.topic.name,
            onDismiss = { renaming = null },
            onRename = { name ->
                renaming = null
                onRename(pending.topic.id, name)
            },
        )
    }
    merging?.let { pending ->
        MergeTopicDialog(
            pending = pending,
            candidates = (state as? TopicQueueUiState.Ready)?.candidates.orEmpty(),
            onSearch = onFindMergeTargets,
            onDismiss = {
                merging = null
                onFindMergeTargets("")
            },
            onMerge = { into ->
                merging = null
                onMerge(pending.topic.id, into.id)
            },
        )
    }
}

@Composable
private fun PendingTopicCard(
    pending: PendingTopic,
    busy: Boolean,
    onApprove: () -> Unit,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .testTag(TopicQueueTestTags.topicCard(pending.topic.id))
            .padding(16.dp),
    ) {
        Text(
            text = pending.topic.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.moderation_topic_proposed_by, pending.proposerName),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        pending.requestedSection?.let { section ->
            Text(
                text = stringResource(R.string.moderation_topic_wanted_in, stringResource(section.labelRes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The twins first: most queue decisions are "this already exists".
        if (pending.similar.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.moderation_topic_similar,
                    pending.similar.joinToString(", ") { it.name },
                ),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.moderation_approve_consequence),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onReject,
                enabled = !busy,
                modifier = Modifier.testTag(TopicQueueTestTags.rejectTopic(pending.topic.id)),
            ) {
                Text(
                    text = stringResource(R.string.moderation_reject),
                    color = if (busy) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                )
            }
            TextButton(
                onClick = onMerge,
                enabled = !busy,
                modifier = Modifier.testTag(TopicQueueTestTags.mergeTopic(pending.topic.id)),
            ) { Text(stringResource(R.string.moderation_merge)) }
            TextButton(
                onClick = onRename,
                enabled = !busy,
                modifier = Modifier.testTag(TopicQueueTestTags.renameTopic(pending.topic.id)),
            ) { Text(stringResource(R.string.moderation_rename)) }
            TextButton(
                onClick = onApprove,
                enabled = !busy,
                modifier = Modifier.testTag(TopicQueueTestTags.approveTopic(pending.topic.id)),
            ) { Text(stringResource(R.string.moderation_approve)) }
        }
    }
}

@Composable
internal fun QueueMessage(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TopicQueueTestTags.MESSAGE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
