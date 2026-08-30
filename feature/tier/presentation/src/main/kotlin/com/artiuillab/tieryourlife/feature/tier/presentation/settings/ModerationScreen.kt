package com.artiuillab.tieryourlife.feature.tier.presentation.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.FlagIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon

@Composable
fun ModerationScreen(
    onBack: () -> Unit,
    viewModel: ModerationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModerationScreenContent(
        state = state,
        onBack = onBack,
        onTakeDown = viewModel::takeDown,
        onDismiss = viewModel::dismiss,
        onRetry = viewModel::load,
    )
}

@Composable
internal fun ModerationScreenContent(
    state: ModerationUiState,
    onBack: () -> Unit,
    onTakeDown: (String) -> Unit = {},
    onDismiss: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        CenteredContent(
            max = ContentWidth.Reading,
            modifier = Modifier.fillMaxSize().testTag(ModerationTestTags.SCREEN),
        ) {
            TopBar(onBack)

            when (state) {
                ModerationUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ModerationUiState.Failed -> Message(
                    title = stringResource(R.string.moderation_failed),
                    body = stringResource(R.string.home_community_failed_body),
                    action = stringResource(R.string.action_try_again),
                    onAction = onRetry,
                )

                is ModerationUiState.Ready -> if (state.reports.isEmpty()) {
                    Message(
                        title = stringResource(R.string.moderation_empty_title),
                        body = stringResource(R.string.moderation_empty_body),
                        action = null,
                        onAction = {},
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.reports, key = { "${it.listId}-${it.createdAtMillis}" }) { report ->
                            ReportCard(
                                report = report,
                                busy = state.settling != null,
                                onTakeDown = { onTakeDown(report.listId) },
                                onDismiss = { onDismiss(report.listId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription }
                .testTag(ModerationTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.moderation_title),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReportCard(
    report: ModerationReport,
    busy: Boolean,
    onTakeDown: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .testTag(ModerationTestTags.reportCard(report.listId))
            .padding(16.dp),
    ) {
        Text(
            text = report.listTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.community_by_author, report.authorName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagIcon(18.dp, MaterialTheme.colorScheme.error)
            Text(
                text = stringResource(report.reason.labelRes()),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        report.note?.let { note ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.testTag(ModerationTestTags.dismiss(report.listId)),
            ) { Text(stringResource(R.string.moderation_action_keep)) }
            TextButton(
                onClick = onTakeDown,
                enabled = !busy,
                modifier = Modifier.testTag(ModerationTestTags.takeDown(report.listId)),
            ) {
                Text(
                    text = stringResource(R.string.moderation_action_take_down),
                    color = if (busy) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun Message(title: String, body: String, action: String?, onAction: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(ModerationTestTags.MESSAGE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { FlagIcon(28.dp, MaterialTheme.colorScheme.outline) }
            Spacer(Modifier.height(8.dp))
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

private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.Sexual -> R.string.report_reason_sexual
    ReportReason.Violence -> R.string.report_reason_violence
    ReportReason.Hate -> R.string.report_reason_hate
    ReportReason.Spam -> R.string.report_reason_spam
    ReportReason.Other -> R.string.report_reason_other
}

internal object ModerationTestTags {
    const val SCREEN = "moderation_screen"
    const val BACK = "moderation_back"
    const val MESSAGE = "moderation_message"

    fun reportCard(listId: String): String = "moderation_report_$listId"

    fun takeDown(listId: String): String = "moderation_take_down_$listId"

    fun dismiss(listId: String): String = "moderation_dismiss_$listId"
}

@TierYourLifeDevicePreviews
@Composable
private fun ModerationScreenPreview() = TierYourLifeTheme {
    ModerationScreenContent(
        state = ModerationUiState.Ready(
            listOf(
                ModerationReport(
                    listId = "1",
                    listTitle = "Every A24 film, ranked",
                    authorName = "Danylo Kovalenko",
                    reason = ReportReason.Spam,
                    note = "Every card is a link to the same shop.",
                    createdAtMillis = 0,
                ),
                ModerationReport(
                    listId = "2",
                    listTitle = "Ramen places in Kyiv",
                    authorName = "Someone Else",
                    reason = ReportReason.Hate,
                    note = null,
                    createdAtMillis = 0,
                ),
            ),
        ),
        onBack = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun ModerationScreenEmptyPreview() = TierYourLifeTheme {
    ModerationScreenContent(state = ModerationUiState.Ready(emptyList()), onBack = {})
}
