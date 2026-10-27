package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val ROW_CORNER = 12.dp

/**
 * Where a board goes in the community: an existing topic found by typing, a
 * new one named here and left for a moderator, or none. The section chips on
 * the settings screen stay required; this is the word inside the section.
 */
@Composable
fun TopicPickerScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TopicPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    TopicPickerScreenContent(
        state = state,
        onBack = onBack,
        onDone = viewModel::save,
        onQueryChange = viewModel::onQueryChange,
        onChoose = viewModel::choose,
        onChooseNone = viewModel::chooseNone,
        onCreate = viewModel::createNew,
        userMessages = viewModel.userMessages,
    )
}

@Composable
fun TopicPickerScreenContent(
    state: TopicPickerUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onQueryChange: (String) -> Unit = {},
    onChoose: (Topic) -> Unit = {},
    onChooseNone: () -> Unit = {},
    onCreate: () -> Unit = {},
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag(TopicPickerTestTags.SCREEN),
    ) {
        ActionFailedSnackbar(userMessages)
        TopBar(onBack = onBack, onDone = onDone, enabled = !state.saving)
        CenteredContent(ContentWidth.Reading, modifier = Modifier.fillMaxSize()) {
            CurrentChoice(state.chosen)
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(TopicPickerTestTags.QUERY),
                placeholder = { Text(stringResource(R.string.topic_picker_hint)) },
                singleLine = true,
            )
            if (state.searching) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = systemBarsContentPadding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            ) {
                val suggestion = state.suggestion
                if (suggestion != null && !state.chosen.names(suggestion)) {
                    item(key = "suggestion") {
                        ChoiceRow(
                            title = stringResource(R.string.topic_picker_suggestion, suggestion.name),
                            subtitle = suggestion.sectionAndCount(),
                            selected = false,
                            onClick = { onChoose(suggestion) },
                            testTag = TopicPickerTestTags.SUGGESTION,
                        )
                    }
                }
                item(key = "none") {
                    ChoiceRow(
                        title = stringResource(R.string.topic_picker_none),
                        subtitle = stringResource(R.string.topic_picker_none_sub),
                        selected = state.chosen == TopicChoice.None,
                        onClick = onChooseNone,
                        testTag = TopicPickerTestTags.NONE,
                    )
                }
                val createName = state.createName
                if (createName != null) {
                    item(key = "create") {
                        ChoiceRow(
                            title = stringResource(R.string.topic_picker_create, createName),
                            subtitle = if (state.canCreate) {
                                stringResource(R.string.topic_picker_create_note) + " " +
                                    pluralStringResource(R.plurals.topic_picker_pending_count, state.pendingCount, state.pendingCount, state.pendingLimit)
                            } else {
                                stringResource(R.string.topic_picker_limit)
                            },
                            selected = (state.chosen as? TopicChoice.New)?.name == createName,
                            onClick = onCreate,
                            enabled = state.canCreate,
                            testTag = TopicPickerTestTags.CREATE,
                        )
                    }
                }
                if (state.mine.isNotEmpty()) {
                    item(key = "mine-header") { SectionHeader(stringResource(R.string.topic_picker_yours)) }
                    items(state.mine, key = { "mine-" + it.id }) { topic ->
                        ChoiceRow(
                            title = topic.name,
                            subtitle = topic.standing(),
                            selected = state.chosen.names(topic),
                            // Turned down or folded away: nothing to file under any more.
                            enabled = topic.status == TopicStatus.Approved || topic.pending,
                            onClick = { onChoose(topic) },
                            testTag = TopicPickerTestTags.topic(topic.id),
                        )
                    }
                }
                val others = state.results.filterNot { found -> state.mine.any { it.id == found.id } }
                items(others, key = { "found-" + it.id }) { topic ->
                    ChoiceRow(
                        title = topic.name,
                        subtitle = topic.sectionAndCount(),
                        selected = state.chosen.names(topic),
                        onClick = { onChoose(topic) },
                        testTag = TopicPickerTestTags.topic(topic.id),
                    )
                }
            }
        }
    }
}

private fun TopicChoice.names(topic: Topic): Boolean = this is TopicChoice.Known && this.topic.id == topic.id

@Composable
private fun Topic.sectionAndCount(): String {
    val section = section?.let { stringResource(it.labelRes) }
    val lists = pluralStringResource(R.plurals.topic_picker_lists, listCount, listCount)
    return listOfNotNull(section, lists).joinToString(" · ")
}

@Composable
private fun Topic.standing(): String = when (status) {
    TopicStatus.Pending -> stringResource(R.string.topic_picker_waiting)
    TopicStatus.Approved -> sectionAndCount()
    TopicStatus.Rejected -> stringResource(R.string.topic_picker_rejected)
    TopicStatus.Merged -> stringResource(R.string.topic_picker_merged)
}

@Composable
private fun CurrentChoice(chosen: TopicChoice) {
    val text = when (chosen) {
        TopicChoice.None -> stringResource(R.string.topic_picker_none)
        is TopicChoice.Known -> chosen.topic.name
        is TopicChoice.New -> stringResource(R.string.topic_picker_new_waits, chosen.name)
    }
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(TopicPickerTestTags.CURRENT),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val text = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(ROW_CORNER))
            .background(tint)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onDone: () -> Unit, enabled: Boolean) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
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
                .testTag(TopicPickerTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.topic_picker_title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onDone, enabled = enabled, modifier = Modifier.testTag(TopicPickerTestTags.DONE)) {
            Text(stringResource(R.string.topic_picker_done))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun TopicPickerPreview() = TierYourLifeTheme {
    TopicPickerScreenContent(
        state = TopicPickerUiState(
            title = "Oscar films 2025",
            query = "Osc",
            results = listOf(
                Topic(id = "t1", name = "Oscars", section = ListCategory.FilmTv, status = TopicStatus.Approved, listCount = 41),
                Topic(id = "t2", name = "Oscar snubs", section = ListCategory.FilmTv, status = TopicStatus.Approved, listCount = 6),
            ),
            mine = listOf(Topic(id = "t9", name = "Kharkiv restaurants", section = null, status = TopicStatus.Pending)),
            chosen = TopicChoice.Known(Topic(id = "t1", name = "Oscars", section = ListCategory.FilmTv, status = TopicStatus.Approved)),
        ),
        onBack = {},
        onDone = {},
    )
}
