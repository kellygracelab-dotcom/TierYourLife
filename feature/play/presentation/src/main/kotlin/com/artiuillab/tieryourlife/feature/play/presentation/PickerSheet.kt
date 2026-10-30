package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.isShortWindow
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

/**
 * The game picker and "Choose cards", two pages of one sheet: from the bottom
 * on a phone, in the middle on a tablet. Held sideways the window is wide and
 * low, so the dialog is too, and the three games stand side by side.
 */
@Composable
internal fun PickerSheet(picker: Picker, actions: PickerActions) {
    val wide = isShortWindow && currentWindowShape.hasRail
    AdaptiveSheet(
        onDismiss = actions.onDismiss,
        width = if (wide) WIDE_PICKER_WIDTH else SheetWidth.Working,
        maxHeight = if (wide) WIDE_PICKER_MAX_HEIGHT else PICKER_MAX_HEIGHT,
    ) {
        PickerContent(picker, actions, wide)
    }
}

private val PICKER_MAX_HEIGHT = 720.dp
private val WIDE_PICKER_WIDTH = 760.dp
private val WIDE_PICKER_MAX_HEIGHT = 363.dp

@Composable
internal fun ColumnScope.PickerContent(picker: Picker, actions: PickerActions, wide: Boolean = false) {
    // In the middle of a window the sheet has no handle above the title to keep it off the edge.
    if (wide) {
        Spacer(Modifier.height(12.dp))
    } else if (currentWindowShape.hasRail) {
        Spacer(Modifier.height(24.dp))
    }
    when {
        wide && picker.page == PickerPage.Games -> WideGamesPage(picker, actions)
        wide -> WideCardsPage(picker, actions)
        picker.page == PickerPage.Games -> GamesPage(picker, actions)
        else -> CardsPage(picker, actions)
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
        CardsFromLine(picker, onShowCards = actions.onShowCards)
        Spacer(Modifier.height(16.dp))
        picker.resume?.let { offer ->
            ResumeRow(offer, minHeight = 56.dp, onResume = actions.onResume, onStartAgain = actions.onStartAgain)
            Text(
                text = stringResource(R.string.play_new_game),
                modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayGames.forEach { kind ->
                GameOption(kind = kind, count = picker.count, selected = kind == picker.kind, onSelect = { actions.onChoose(kind) })
            }
        }
        if (picker.intoMine) IntoMineNote()
    }
    SheetActions {
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        StartButton(picker, actions.onStart)
    }
}

/** "34 cards from Sci-fi films · Choose cards", the link in the sentence it belongs to. */
@Composable
private fun CardsFromLine(picker: Picker, onShowCards: () -> Unit) {
    val from = pluralStringResource(R.plurals.play_cards_from, picker.count, picker.count, picker.title)
    val choose = stringResource(R.string.play_choose_cards)
    val link = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium))
    val text = buildAnnotatedString {
        append(from)
        append(" · ")
        withLink(LinkAnnotation.Clickable(tag = CHOOSE_LINK, styles = link) { onShowCards() }) { append(choose) }
    }
    Text(
        text = text,
        modifier = Modifier
            .padding(top = 4.dp)
            .testTag(PlayTestTags.CHOOSE_CARDS),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val CHOOSE_LINK = "choose_cards"

@Composable
private fun IntoMineNote() {
    Text(
        text = stringResource(R.string.play_into_mine_note),
        modifier = Modifier.padding(top = 12.dp),
        style = FOOTNOTE,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val FOOTNOTE = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)

@Composable
private fun StartButton(picker: Picker, onStart: () -> Unit) {
    FilledTonalButton(
        onClick = onStart,
        enabled = picker.canPlay,
        modifier = Modifier
            .height(40.dp)
            .testTag(PlayTestTags.START),
    ) { Text(stringResource(R.string.play_start)) }
}

@Composable
private fun SheetActions(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** The game put down for this board: what it is, how far it got, and the one button that picks it up. */
@Composable
private fun ResumeRow(offer: ResumeOffer, minHeight: Dp, onResume: () -> Unit, onStartAgain: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .testTag(PlayTestTags.RESUME_ROW),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HistoryIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                val name = stringResource(offer.kind.nameRes)
                Text(
                    text = if (offer.changed) name else stringResource(R.string.play_resume_progress, name, offer.played, offer.total),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(10.dp))
                val started = startedText(offer.startedAt)
                Text(
                    text = when {
                        offer.changed -> stringResource(R.string.play_board_changed)
                        offer.joined > 0 -> started + " · " + pluralStringResource(R.plurals.play_new_cards_join, offer.joined, offer.joined)
                        else -> started
                    },
                    modifier = Modifier.weight(1f, fill = false),
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

/** The colours of a game's card, chosen or not: the same on the sheet and in the sideways dialog. */
private data class OptionColors(val background: Color, val content: Color, val quiet: Color)

@Composable
private fun optionColors(selected: Boolean): OptionColors {
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    return OptionColors(
        background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        content = content,
        quiet = if (selected) content.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Modifier.gameOption(kind: GameKind, selected: Boolean, onSelect: () -> Unit): Modifier {
    val shape = RoundedCornerShape(16.dp)
    return clip(shape)
        .background(optionColors(selected).background)
        .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape))
        .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
        .testTag(PlayTestTags.game(kind))
}

@Composable
private fun GameOption(kind: GameKind, count: Int, selected: Boolean, onSelect: () -> Unit) {
    val colors = optionColors(selected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .gameOption(kind, selected, onSelect)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        GameIcon(kind, 22.dp, colors.content)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(kind.nameRes), style = MaterialTheme.typography.titleMedium, color = colors.content)
            Text(stringResource(kind.lineRes), style = MaterialTheme.typography.bodyMedium, color = colors.quiet)
            Text(
                text = estimateText(kind, count),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.quiet,
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
        KeepInTierRow(picker, actions.onKeepInTier)
    }
    SelectedCounter(picker, Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp))
    SheetActions {
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        ContinueButton(picker, actions.onShowGames)
    }
}

/** Only while one tier is chosen: its cards may stay in it and only be ordered. */
@Composable
private fun KeepInTierRow(picker: Picker, onKeepInTier: (Boolean) -> Unit) {
    val tier = picker.singleTier ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = picker.keepTier, role = Role.Switch, onValueChange = onKeepInTier)
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

@Composable
private fun SelectedCounter(picker: Picker, modifier: Modifier = Modifier) {
    val over = picker.count > MAX_PLAY_CARDS
    Text(
        text = if (over) {
            pluralStringResource(R.plurals.play_selected_too_many, picker.count, picker.count)
        } else {
            pluralStringResource(R.plurals.play_selected, picker.count, picker.count, MAX_PLAY_CARDS)
        },
        modifier = modifier.testTag(PlayTestTags.COUNTER),
        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ContinueButton(picker: Picker, onContinue: () -> Unit) {
    FilledTonalButton(
        onClick = onContinue,
        enabled = picker.canPlay,
        modifier = Modifier
            .height(40.dp)
            .testTag(PlayTestTags.CONTINUE),
    ) { Text(stringResource(R.string.play_continue)) }
}

@Composable
private fun GroupRow(group: CardGroup, checked: Boolean, onToggle: () -> Unit) {
    CheckRow(
        title = group.title(),
        count = group.count,
        checked = checked,
        enabled = true,
        onToggle = onToggle,
        note = if (group.count > MAX_PLAY_CARDS) stringResource(R.string.play_too_many_in_tier) else null,
        swatch = if (group.isPool) null else tierRowColors(group.colorLight, group.colorDark).band,
        tag = PlayTestTags.group(group.tierId),
    )
}

/** "The pool", or the tier as its band names it: "S · Masterpiece". */
@Composable
private fun CardGroup.title(): String = if (isPool) {
    stringResource(R.string.play_pool)
} else {
    listOfNotNull(label, caption?.takeIf { it.isNotBlank() }).joinToString(" · ")
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

private val WIDE_TITLE = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
private val WIDE_SUBTITLE = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

/** Sideways: the three games side by side, every one of them in view without a scroll. */
@Composable
private fun ColumnScope.WideGamesPage(picker: Picker, actions: PickerActions) {
    Column(
        Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .testTag(PlayTestTags.PICKER),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.play_title), style = WIDE_TITLE, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Text(
                text = pluralStringResource(R.plurals.play_cards_from, picker.count, picker.count, picker.title),
                modifier = Modifier.weight(1f),
                style = WIDE_SUBTITLE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = actions.onShowCards,
                modifier = Modifier.testTag(PlayTestTags.CHOOSE_CARDS),
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
            ) {
                ChecklistIcon(18.dp, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.play_choose_cards))
            }
        }
        Spacer(Modifier.height(6.dp))
        picker.resume?.let { offer ->
            ResumeRow(offer, minHeight = 52.dp, onResume = actions.onResume, onStartAgain = actions.onStartAgain)
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayGames.forEach { kind ->
                GameColumn(
                    kind = kind,
                    count = picker.count,
                    selected = kind == picker.kind,
                    onSelect = { actions.onChoose(kind) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        if (picker.intoMine) IntoMineNote()
    }
    SheetActions {
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        StartButton(picker, actions.onStart)
    }
}

@Composable
private fun GameColumn(kind: GameKind, count: Int, selected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val colors = optionColors(selected)
    Column(
        modifier
            .heightIn(min = 146.dp)
            .gameOption(kind, selected, onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(10.dp))
            GameIcon(kind, 20.dp, colors.content)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(kind.nameRes), style = COLUMN_NAME, color = colors.content)
        Text(
            text = stringResource(kind.lineRes),
            modifier = Modifier.padding(top = 4.dp),
            style = COLUMN_LINE,
            color = colors.quiet,
        )
        Spacer(
            Modifier
                .weight(1f)
                .heightIn(min = 10.dp),
        )
        Text(estimateText(kind, count), style = COLUMN_ESTIMATE, color = colors.quiet)
    }
}

private val COLUMN_NAME = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
private val COLUMN_LINE = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
private val COLUMN_ESTIMATE = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)

/** Sideways: rows of 48 in two columns, the title and the buttons pinned while the tiers scroll between. */
@Composable
private fun ColumnScope.WideCardsPage(picker: Picker, actions: PickerActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.play_choose_cards), style = WIDE_TITLE, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(
            text = pluralStringResource(R.plurals.play_cards_limit, MAX_PLAY_CARDS, MAX_PLAY_CARDS),
            modifier = Modifier.weight(1f, fill = false),
            style = WIDE_SUBTITLE,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(6.dp))
    // The groups, and Everything last, standing for itself as null.
    val cells: List<CardGroup?> = picker.groups + null
    Column(
        Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .testTag(PlayTestTags.CARDS_PAGE),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { group ->
                    if (group == null) {
                        EverythingCell(picker, actions.onToggleEverything, Modifier.weight(1f))
                    } else {
                        GroupCell(group, group.tierId in picker.chosen, { actions.onToggle(group.tierId) }, Modifier.weight(1f))
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        KeepInTierRow(picker, actions.onKeepInTier)
    }
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectedCounter(picker, Modifier.weight(1f))
        // Opened on the cards, there is no game page to go back to.
        if (picker.cardsFirst) {
            TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        } else {
            TextButton(onClick = actions.onShowGames) { Text(stringResource(R.string.play_back)) }
        }
        ContinueButton(picker, actions.onShowGames)
    }
}

@Composable
private fun GroupCell(group: CardGroup, checked: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    CellRow(checked = checked, enabled = true, onToggle = onToggle, tag = PlayTestTags.group(group.tierId), modifier = modifier) {
        if (group.isPool) {
            PoolIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val colors = tierRowColors(group.colorLight, group.colorDark)
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.band),
                contentAlignment = Alignment.Center,
            ) {
                Text(group.label, style = SWATCH_LETTER, color = colors.onBand, maxLines = 1)
            }
        }
        CellText(group.title(), group.count, enabled = true)
    }
}

@Composable
private fun EverythingCell(picker: Picker, onToggle: () -> Unit, modifier: Modifier) {
    val enabled = picker.total <= MAX_PLAY_CARDS
    CellRow(checked = picker.everything, enabled = enabled, onToggle = onToggle, tag = PlayTestTags.EVERYTHING, modifier = modifier) {
        val quiet = MaterialTheme.colorScheme.onSurfaceVariant
        EverythingIcon(22.dp, if (enabled) quiet else quiet.copy(alpha = 0.38f))
        CellText(stringResource(R.string.play_everything), picker.total, enabled)
    }
}

@Composable
private fun CellRow(checked: Boolean, enabled: Boolean, onToggle: () -> Unit, tag: String, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(tag)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(8.dp))
        Spacer(Modifier.width(4.dp))
        content()
    }
}

/** The name, and the count beside it: red when the tier is too big to play whole. */
@Composable
private fun RowScope.CellText(title: String, count: Int, enabled: Boolean) {
    val ink = MaterialTheme.colorScheme.onSurface
    Text(
        text = title,
        modifier = Modifier
            .weight(1f)
            .padding(start = 10.dp),
        style = CELL_NAME,
        color = if (enabled) ink else ink.copy(alpha = 0.38f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = count.toString(),
        modifier = Modifier.padding(start = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = when {
            count > MAX_PLAY_CARDS && enabled -> MaterialTheme.colorScheme.error
            enabled -> quiet
            else -> quiet.copy(alpha = 0.38f)
        },
    )
}

private val CELL_NAME = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
private val SWATCH_LETTER = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
