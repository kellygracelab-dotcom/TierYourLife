package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.MAX_PLAY_CARDS
import com.artiuillab.tieryourlife.feature.play.domain.estimatedDuels
import com.artiuillab.tieryourlife.feature.play.domain.estimatedMinutes
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** What the picker's controls do; a screenshot passes none of them. */
internal data class PickerActions(
    val onChoose: (GameKind) -> Unit = {},
    val onShowCards: () -> Unit = {},
    val onShowGames: () -> Unit = {},
    val onToggle: (Long) -> Unit = {},
    val onToggleEverything: () -> Unit = {},
    val onKeepInTier: (Boolean) -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onStartAgain: () -> Unit = {},
)

/** The game picker and "Choose cards", two pages of one sheet: from the bottom on a phone, in the middle on a tablet. */
@Composable
internal fun PickerSheet(picker: Picker, actions: PickerActions) {
    AdaptiveSheet(onDismiss = actions.onDismiss, width = SheetWidth.Working, maxHeight = PICKER_MAX_HEIGHT) {
        PickerContent(picker, actions)
    }
}

private val PICKER_MAX_HEIGHT = 720.dp

@Composable
internal fun ColumnScope.PickerContent(picker: Picker, actions: PickerActions) {
    // In the middle of a tablet the sheet has no handle above the title to keep it off the edge.
    if (currentWindowShape.hasRail) Spacer(Modifier.height(24.dp))
    when (picker.page) {
        PickerPage.Games -> GamesPage(picker, actions)
        PickerPage.Cards -> CardsPage(picker, actions)
    }
}

@Composable
private fun ColumnScope.GamesPage(picker: Picker, actions: PickerActions) {
    Column(
        Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .testTag(PlayTestTags.PICKER),
    ) {
        Text(
            text = stringResource(R.string.play_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.play_cards_from, picker.count, picker.count, picker.title),
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = actions.onShowCards, modifier = Modifier.testTag(PlayTestTags.CHOOSE_CARDS)) {
                Text(stringResource(R.string.play_choose_cards))
            }
        }
        Spacer(Modifier.height(8.dp))
        picker.resume?.let { offer ->
            ResumeRow(offer, onResume = actions.onResume, onStartAgain = actions.onStartAgain)
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayGames.forEach { kind ->
                GameOption(kind = kind, count = picker.count, selected = kind == picker.kind, onSelect = { actions.onChoose(kind) })
            }
        }
        if (picker.intoMine) {
            Text(
                text = stringResource(R.string.play_into_mine_note),
                modifier = Modifier.padding(top = 12.dp),
                style = FOOTNOTE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    SheetActions {
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        FilledTonalButton(
            onClick = actions.onStart,
            enabled = picker.canPlay,
            modifier = Modifier
                .height(40.dp)
                .testTag(PlayTestTags.START),
        ) { Text(stringResource(R.string.play_start)) }
    }
}

private val FOOTNOTE = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)

@Composable
private fun SheetActions(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun ResumeRow(offer: ResumeOffer, onResume: () -> Unit, onStartAgain: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PlayTestTags.RESUME_ROW),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            HistoryIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val name = stringResource(offer.kind.nameRes)
                Text(
                    text = if (offer.changed) name else stringResource(R.string.play_resume_progress, name, offer.played, offer.total),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val started = startedText(offer.startedAt)
                Text(
                    text = when {
                        offer.changed -> stringResource(R.string.play_board_changed)
                        offer.joined > 0 -> started + " · " + pluralStringResource(R.plurals.play_new_cards_join, offer.joined, offer.joined)
                        else -> started
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = if (offer.changed) onStartAgain else onResume,
                modifier = Modifier
                    .height(36.dp)
                    .testTag(PlayTestTags.RESUME),
            ) {
                Text(stringResource(if (offer.changed) R.string.play_start_again else R.string.play_resume))
            }
        }
    }
}

@Composable
private fun startedText(startedAt: Long): String {
    val days = remember(startedAt) { daysSince(startedAt) }
    return when (days) {
        0 -> stringResource(R.string.play_started_today)
        1 -> stringResource(R.string.play_started_yesterday)
        else -> pluralStringResource(R.plurals.play_started_days_ago, days, days)
    }
}

/** Calendar days, not periods of 24 hours: a game begun last night was begun yesterday. */
internal fun daysSince(startedAt: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
    val then = Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(then, today).toInt().coerceAtLeast(0)
}

@Composable
private fun GameOption(kind: GameKind, count: Int, selected: Boolean, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val quiet = if (selected) content.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape))
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag(PlayTestTags.game(kind))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(kind, 24.dp, content)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(kind.nameRes), style = MaterialTheme.typography.titleMedium, color = content)
            Text(stringResource(kind.lineRes), style = MaterialTheme.typography.bodyMedium, color = quiet)
            Text(
                text = estimateText(kind, count),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = quiet,
            )
        }
    }
}

/** "≈ 136 duels · about 5 min": Swiss may stop early, so its count is only roughly. */
@Composable
private fun estimateText(kind: GameKind, count: Int): String {
    val duels = estimatedDuels(kind, count)
    val minutes = estimatedMinutes(duels)
    val prefix = if (kind == GameKind.Swiss) "≈ " else ""
    return prefix + pluralStringResource(R.plurals.play_estimate_duels, duels, duels) + " · " +
        pluralStringResource(R.plurals.play_estimate_minutes, minutes, minutes)
}

@Composable
private fun ColumnScope.CardsPage(picker: Picker, actions: PickerActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (picker.cardsFirst) 24.dp else 8.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!picker.cardsFirst) {
            val back = stringResource(ThemeR.string.tier_detail_content_description_back)
            IconButton(onClick = actions.onShowGames, modifier = Modifier.semantics { contentDescription = back }) { BackIcon() }
        }
        Text(
            text = stringResource(R.string.play_choose_cards),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Column(
        Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
            .testTag(PlayTestTags.CARDS_PAGE),
    ) {
        picker.groups.forEach { group ->
            GroupRow(group = group, checked = group.tierId in picker.chosen, onToggle = { actions.onToggle(group.tierId) })
        }
        CheckRow(
            title = stringResource(R.string.play_everything),
            count = picker.total,
            checked = picker.everything,
            enabled = picker.total <= MAX_PLAY_CARDS,
            onToggle = actions.onToggleEverything,
        )
        picker.singleTier?.let { tier ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(value = picker.keepTier, role = Role.Switch, onValueChange = actions.onKeepInTier)
                    .testTag(PlayTestTags.KEEP_IN_TIER)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.play_keep_in_tier, tier.label),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(checked = picker.keepTier, onCheckedChange = null)
            }
        }
    }
    val over = picker.count > MAX_PLAY_CARDS
    Text(
        text = if (over) {
            pluralStringResource(R.plurals.play_selected_too_many, picker.count, picker.count)
        } else {
            pluralStringResource(R.plurals.play_selected, picker.count, picker.count, MAX_PLAY_CARDS)
        },
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 12.dp)
            .testTag(PlayTestTags.COUNTER),
        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SheetActions {
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        FilledTonalButton(
            onClick = actions.onShowGames,
            enabled = picker.canPlay,
            modifier = Modifier
                .height(40.dp)
                .testTag(PlayTestTags.CONTINUE),
        ) { Text(stringResource(R.string.play_continue)) }
    }
}

@Composable
private fun GroupRow(group: CardGroup, checked: Boolean, onToggle: () -> Unit) {
    val title = if (group.isPool) {
        stringResource(R.string.play_pool)
    } else {
        listOfNotNull(group.label, group.caption?.takeIf { it.isNotBlank() }).joinToString(" · ")
    }
    CheckRow(
        title = title,
        count = group.count,
        checked = checked,
        enabled = true,
        onToggle = onToggle,
        note = if (group.count > MAX_PLAY_CARDS) stringResource(R.string.play_too_many_in_tier) else null,
        swatch = if (group.isPool) null else tierRowColors(group.colorLight, group.colorDark).band,
        tag = PlayTestTags.group(group.tierId),
    )
}

@Composable
private fun CheckRow(
    title: String,
    count: Int,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    note: String? = null,
    swatch: Color? = null,
    tag: String = PlayTestTags.EVERYTHING,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(tag)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(12.dp))
        if (swatch != null) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(swatch),
            )
            Spacer(Modifier.width(12.dp))
        }
        val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = pluralStringResource(R.plurals.play_cards, count, count),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@get:StringRes
internal val GameKind.nameRes: Int
    get() = when (this) {
        GameKind.Swiss -> R.string.play_game_swiss
        GameKind.Hill -> R.string.play_game_hill
        GameKind.Bracket -> R.string.play_game_bracket
    }

@get:StringRes
private val GameKind.lineRes: Int
    get() = when (this) {
        GameKind.Swiss -> R.string.play_game_swiss_line
        GameKind.Hill -> R.string.play_game_hill_line
        GameKind.Bracket -> R.string.play_game_bracket_line
    }
