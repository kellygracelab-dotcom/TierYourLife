package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.core.theme.ui.PublishIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_SECTION_NAME
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.tier.board.common.choosableSections
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.publishErrorText
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker.MAX_TOPIC_NAME_LENGTH
import com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker.cleanTopicName
import com.artiuillab.tieryourlife.feature.tier.board.R as BoardR

/** Test tags of the publish sheet. */
object PublishSheetTags {
    const val SHEET = "publish_sheet"
    const val NAME = "publish_sheet_name"
    const val SECTION_HEAD = "publish_sheet_section_head"
    const val PROPOSE_TOPIC = "publish_sheet_propose_topic"
    const val TOPIC_NAME = "publish_sheet_topic_name"
    const val ADD_TOPIC = "publish_sheet_add_topic"
    const val AS_SECTION = "publish_sheet_as_section"
    const val PUBLISH = "publish_sheet_publish"
    const val ERROR = "publish_sheet_error"
    const val COVER = "publish_sheet_cover"
    const val CHANGE_COVER = "publish_sheet_change_cover"

    fun section(id: String) = "publish_sheet_section_$id"
    fun topic(name: String) = "publish_sheet_topic_$name"
}

/** What the sheet hands back. */
data class PublishSheetCalls(
    val onSection: (String) -> Unit = {},
    val onTopic: (Topic?) -> Unit = {},
    val onProposeTopic: (String) -> Unit = {},
    /** The section proposal form with this name, the board's section to file under meanwhile; null where it cannot open. */
    val onProposeSection: ((String) -> Unit)? = null,
    val onPublish: () -> Unit = {},
    /** The name typed for an untitled list, written before it goes out. */
    val onName: (String) -> Unit = {},
    val onDismiss: () -> Unit = {},
    /** The cover picker, back to this sheet after; null leaves the cover out. */
    val onChangeCover: (() -> Unit)? = null,
)

/**
 * The first publish of a board of one's own in one sheet, in the order the web
 * asks it (Claude Design, publish window 16.11.2026): section, then a topic
 * or a proposed one, the one explanation, then Publish with its ring.
 */
@Composable
fun PublishSheet(
    list: TierList,
    state: PublishSheetState,
    busy: Boolean,
    error: PublishError?,
    calls: PublishSheetCalls,
) {
    // "Untitled list" is not a name to publish under: the field comes first, and Publish waits for it.
    val untitled = isUntitled(list.title)
    var name by rememberSaveable(list.id) { mutableStateOf("") }
    var nameWanted by rememberSaveable(list.id) { mutableStateOf(false) }
    val publish = {
        val typed = name.trim()
        when {
            !untitled -> calls.onPublish()
            typed.isEmpty() -> nameWanted = true
            else -> {
                calls.onName(typed)
                calls.onPublish()
            }
        }
    }
    AdaptiveSheet(onDismiss = calls.onDismiss, width = SheetWidth.Choosing, maxHeight = 720.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp).testTag(PublishSheetTags.SHEET)) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Heading(list)
                if (untitled) {
                    NameBlock(name, wanted = nameWanted, enabled = !busy) {
                        name = it
                        nameWanted = false
                    }
                }
                calls.onChangeCover?.let { change ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BlockHead(stringResource(R.string.publish_sheet_cover))
                        CoverBlock(list, enabled = !busy, onChange = change)
                    }
                }
                SectionBlock(list.category, state.sectionWanted, enabled = !busy, onSection = calls.onSection)
                TopicBlock(list, state.topics, enabled = !busy, calls = calls)
                Explanation()
                if (error != null) {
                    Text(
                        text = publishErrorText(error, LocalResources.current, LocalConfiguration.current.locales[0]),
                        modifier = Modifier.testTag(PublishSheetTags.ERROR),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PublishButton(busy = busy, onClick = publish)
        }
    }
}

@Composable
private fun Heading(list: TierList) {
    val cards = list.tiers.sumOf { it.items.size }
    Column {
        Text(stringResource(R.string.publish_sheet_title, list.title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            pluralStringResource(R.plurals.publish_sheet_cards, cards, cards),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Blank, or the name a new list is given until somebody names it, in this language or the app's first. */
@Composable
private fun isUntitled(title: String): Boolean =
    title.isBlank() || title.trim() == stringResource(R.string.default_tier_list_title) || title.trim() == UNTITLED_LIST

private const val UNTITLED_LIST = "Untitled list"
private const val MAX_NAME = 120

@Composable
private fun NameBlock(name: String, wanted: Boolean, enabled: Boolean, onName: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BlockHead(stringResource(R.string.publish_sheet_name), wanted = wanted)
        OutlinedTextField(
            value = name,
            onValueChange = { onName(it.take(MAX_NAME)) },
            enabled = enabled,
            singleLine = true,
            isError = wanted,
            placeholder = { Text(stringResource(R.string.publish_sheet_name_hint)) },
            supportingText = if (wanted) {
                { Text(stringResource(R.string.publish_sheet_name_first)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag(PublishSheetTags.NAME),
        )
    }
}

@Composable
private fun BlockHead(text: String, modifier: Modifier = Modifier, wanted: Boolean = false, quiet: Boolean = false) {
    Text(
        text = text.uppercase(LocalConfiguration.current.locales[0]),
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = when {
            wanted -> MaterialTheme.colorScheme.error
            quiet -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionBlock(chosen: String?, wanted: Boolean, enabled: Boolean, onSection: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BlockHead(stringResource(R.string.publish_sheet_section), Modifier.testTag(PublishSheetTags.SECTION_HEAD), wanted = wanted)
        if (wanted) {
            Text(stringResource(R.string.publish_sheet_section_first), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choosableSections().forEach { section ->
                Chip(
                    label = section.label(),
                    selected = section.id == chosen,
                    enabled = enabled,
                    onClick = { onSection(section.id) },
                    modifier = Modifier.testTag(PublishSheetTags.section(section.id)),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicBlock(list: TierList, topics: List<Topic>?, enabled: Boolean, calls: PublishSheetCalls) {
    val section = list.category?.let { id -> choosableSections().firstOrNull { it.id == id } }
    var proposing by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (section == null) {
            BlockHead(stringResource(R.string.publish_sheet_topic_plain), quiet = true)
            Text(stringResource(R.string.publish_sheet_pick_section), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            return@Column
        }
        val sectionName = section.label()
        BlockHead(stringResource(R.string.publish_sheet_topic, sectionName))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val shown = topics.orEmpty().take(MAX_TOPIC_CHIPS)
            shown.forEach { topic ->
                val selected = topic.id == list.topicId
                Chip(
                    label = topic.name,
                    selected = selected,
                    enabled = enabled,
                    onClick = { calls.onTopic(topic.takeUnless { selected }) },
                    modifier = Modifier.testTag(PublishSheetTags.topic(topic.name)),
                )
            }
            // Chosen before, or proposed: shown, whether or not the section's first few hold it.
            val own = list.newTopicName ?: list.topicName?.takeIf { name -> list.topicId != null && shown.none { it.id == list.topicId } }
            if (own != null) {
                val label = if (list.newTopicName != null) "$own · ${stringResource(R.string.publish_sheet_new)}" else own
                Chip(label = label, selected = true, enabled = enabled, onClick = { calls.onTopic(null) }, modifier = Modifier.testTag(PublishSheetTags.topic(own)))
            }
            FilterChip(
                selected = proposing,
                onClick = { proposing = !proposing },
                enabled = enabled,
                label = { Text(stringResource(R.string.publish_sheet_propose_topic)) },
                leadingIcon = { PlusIcon(18.dp, MaterialTheme.colorScheme.primary) },
                modifier = Modifier.testTag(PublishSheetTags.PROPOSE_TOPIC),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            )
        }
        if (topics == null) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        if (proposing) {
            ProposeTopic(sectionName, enabled, calls, onDone = { proposing = false })
        }
    }
}

@Composable
private fun ProposeTopic(sectionName: String, enabled: Boolean, calls: PublishSheetCalls, onDone: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    var asSection by rememberSaveable { mutableStateOf(false) }
    val limit = if (asSection) MAX_SECTION_NAME else MAX_TOPIC_NAME_LENGTH
    val name = cleanTopicName(typed.take(limit))
    val add = {
        if (name != null) {
            if (asSection) calls.onProposeSection?.invoke(name) else calls.onProposeTopic(name)
            typed = ""
            onDone()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.take(limit) },
                modifier = Modifier.weight(1f).testTag(PublishSheetTags.TOPIC_NAME),
                enabled = enabled,
                singleLine = true,
                placeholder = { Text(stringResource(if (asSection) R.string.publish_sheet_new_section else R.string.publish_sheet_topic_name)) },
                supportingText = { Text("${minOf(typed.length, limit)}/$limit") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            FilledTonalButton(onClick = add, enabled = enabled && name != null, modifier = Modifier.testTag(PublishSheetTags.ADD_TOPIC)) {
                Text(stringResource(if (asSection) R.string.publish_sheet_continue else R.string.publish_sheet_add))
            }
        }
        if (calls.onProposeSection != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = asSection, onCheckedChange = { asSection = it }, enabled = enabled, modifier = Modifier.testTag(PublishSheetTags.AS_SECTION))
                Text(stringResource(R.string.publish_sheet_as_section), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            stringResource(if (asSection) R.string.publish_sheet_section_note else R.string.publish_sheet_moderator, sectionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Explanation() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PublishIcon(18.dp, MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.publish_sheet_how), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PublishButton(busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag(PublishSheetTags.PUBLISH),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        } else {
            PublishIcon(20.dp, LocalContentColor.current)
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (busy) BoardR.string.board_publishing else BoardR.string.board_publish))
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = if (selected) {
            { CheckIcon(18.dp, LocalContentColor.current) }
        } else {
            null
        },
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    )
}

private const val MAX_TOPIC_CHIPS = 8
