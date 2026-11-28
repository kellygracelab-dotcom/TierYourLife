package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.isShortWindow
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronDownIcon
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
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
    val onShowIndividual: () -> Unit = {},
    val onSample: (Int) -> Unit = {},
    val onToggleCard: (Long) -> Unit = {},
    val onSelectCandidates: (Boolean) -> Unit = {},
    val onToggle: (Long) -> Unit = {},
    val onToggleEverything: () -> Unit = {},
    val onToggleRanked: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onStartAgain: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onUndoDiscard: () -> Unit = {},
)

/**
 * The game picker and "Choose cards", two pages of one sheet: from the bottom
 * on a phone, in the middle on a tablet. Held sideways the window is wide and
 * low, so the dialog is too, and the four games stand side by side.
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
        picker.page == PickerPage.Individual -> IndividualCardsPage(picker, actions)
        wide && picker.page == PickerPage.Games -> WideGamesPage(picker, actions)
        wide -> WideCardsPage(picker, actions)
        picker.page == PickerPage.Games -> GamesPage(picker, actions)
        else -> CardsPage(picker, actions)
    }
}

/**
 * Every game on screen at once, the chosen one filled, and Start saying what
 * it starts. A game put down comes first and is the sheet's main action; the
 * new game sits under it (Danylo, 28.11: «на андроиде вариант B давай тоже»).
 */
@Composable
private fun ColumnScope.GamesPage(picker: Picker, actions: PickerActions) {
    Column(
        Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            .testTag(PlayTestTags.PICKER),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlayHeading(picker, actions)
        NewGameHeader(picker, actions)
        GameChoice(picker, actions)
        StartButton(picker, actions.onStart, Modifier.fillMaxWidth())
        StartNotes(picker)
    }
}

/** "Play", and what it is while nothing is saved; the saved game's card when one is. */
@Composable
private fun PlayHeading(picker: Picker, actions: PickerActions, style: TextStyle = MaterialTheme.typography.headlineSmall) {
    Column {
        Text(text = stringResource(R.string.play_title), style = style, color = MaterialTheme.colorScheme.onSurface)
        if (picker.resume == null && !picker.discarded) {
            Text(
                text = stringResource(R.string.play_how_it_works),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val offer = picker.resume
    when {
        offer != null -> SavedGameCard(offer, actions)
        picker.discarded -> DiscardedRow(actions.onUndoDiscard)
    }
}

/** "Or a new game" beside a saved one, and the cards the new one is played over, which open Choose cards. */
@Composable
private fun NewGameHeader(picker: Picker, actions: PickerActions) {
    val label = picker.resume?.let { stringResource(R.string.play_or_new_game) }
    SideBySideOrStacked(Modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = HEADER_LABEL,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CardsChip(picker, actions.onShowCards)
    }
}

/** Two pieces side by side, the second at the end, or one above the other when a language makes them too long. */
@Composable
private fun SideBySideOrStacked(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val width = constraints.maxWidth
        val first = placeables.first()
        val second = placeables.getOrNull(1)
        when {
            second == null -> layout(width, first.height) { first.placeRelative(0, 0) }
            first.width + second.width + SIDE_GAP.roundToPx() <= width -> {
                val height = maxOf(first.height, second.height)
                layout(width, height) {
                    first.placeRelative(0, (height - first.height) / 2)
                    second.placeRelative(width - second.width, (height - second.height) / 2)
                }
            }
            else -> layout(width, first.height + second.height) {
                first.placeRelative(0, 0)
                second.placeRelative(0, first.height)
            }
        }
    }
}

private val SIDE_GAP = 12.dp
private val HEADER_LABEL = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

/** «All 127 cards ▾», or «12 cards from S»: what a new game is played over, and the way to change it. */
@Composable
private fun CardsChip(picker: Picker, onShowCards: () -> Unit) {
    OutlinedButton(
        onClick = onShowCards,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .testTag(PlayTestTags.CHOOSE_CARDS),
        contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(text = cardsText(picker), style = CHIP_TEXT, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(4.dp))
        ChevronDownIcon(18.dp, MaterialTheme.colorScheme.onSurface)
    }
}

private val CHIP_TEXT = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

@Composable
private fun cardsText(picker: Picker): String {
    val count = picker.count
    return if (picker.everything) {
        pluralStringResource(R.plurals.play_cards_all, count, count)
    } else {
        pluralStringResource(R.plurals.play_cards_from, count, count, picker.areaName())
    }
}

/**
 * The four games as tiles, two across, or [across] where the dialog is wide
 * enough; as rows where two tiles do not fit or the text is large. The rule is
 * the Designer's (28.11), and CI's 320×640 window is the rows' case.
 */
@Composable
private fun GameChoice(picker: Picker, actions: PickerActions, across: Int = 2) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val large = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
        val columns = if (maxWidth >= TILE_MIN_WIDTH * across + TILE_GAP * (across - 1)) across else 2
        if (large || maxWidth < TILES_MIN_WIDTH) {
            GameRows(picker, actions)
        } else {
            GameTiles(picker, actions, columns)
        }
    }
}

/** A 340 dp window, less the sheet's sides. */
private val TILES_MIN_WIDTH = 292.dp
private val TILE_MIN_WIDTH = 140.dp
private val TILE_GAP = 10.dp
private const val LARGE_FONT_SCALE = 1.3f

@Composable
private fun GameTiles(picker: Picker, actions: PickerActions, columns: Int) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        PlayGames.chunked(columns).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
            ) {
                row.forEach { kind ->
                    GameTile(
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
        }
    }
}

@Composable
private fun GameTile(kind: GameKind, count: Int, selected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val colors = optionColors(selected, filled = true)
    Column(
        modifier
            .heightIn(min = 118.dp)
            .gameOption(kind, selected, onSelect, filled = true)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameIcon(kind, 24.dp, colors.icon)
            Spacer(
                Modifier
                    .weight(1f)
                    .widthIn(min = 8.dp),
            )
            Text(text = minutesText(kind, count), style = TILE_TIME, color = colors.content)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(kind.nameRes), style = GAME_NAME, color = colors.content)
            Text(stringResource(kind.shortLineRes), style = GAME_LINE, color = colors.quiet)
        }
    }
}

@Composable
private fun GameRows(picker: Picker, actions: PickerActions) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayGames.forEach { kind ->
            GameRow(kind = kind, count = picker.count, selected = kind == picker.kind, onSelect = { actions.onChoose(kind) })
        }
    }
}

@Composable
private fun GameRow(kind: GameKind, count: Int, selected: Boolean, onSelect: () -> Unit) {
    val colors = optionColors(selected, filled = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .gameOption(kind, selected, onSelect, filled = false)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(kind, 24.dp, colors.icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(stringResource(kind.nameRes), style = GAME_NAME, color = colors.content)
            Text(stringResource(kind.shortLineRes), style = GAME_LINE, color = colors.quiet)
        }
        Spacer(Modifier.width(12.dp))
        Text(text = minutesText(kind, count), style = ROW_TIME, color = colors.content)
    }
}

private val TILE_TIME = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
private val ROW_TIME = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
private val GAME_NAME = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
private val GAME_LINE = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)

/** "21 min": what the game is expected to take over the cards chosen. */
@Composable
private fun minutesText(kind: GameKind, count: Int): String {
    val minutes = estimatedMinutes(estimatedDuels(kind, count))
    return pluralStringResource(R.plurals.play_minutes, minutes, minutes)
}

/** «Start Smart · 21 min», or «Start new · …» beside a saved game, where it is outlined and Resume is filled. */
@Composable
private fun StartButton(picker: Picker, onStart: () -> Unit, modifier: Modifier) {
    val name = stringResource(picker.kind.nameRes)
    val minutes = minutesText(picker.kind, picker.count)
    val saved = picker.resume != null
    val label = stringResource(if (saved) R.string.play_start_new_game else R.string.play_start_game, name, minutes)
    val buttonModifier = modifier
        .heightIn(min = 52.dp)
        .testTag(PlayTestTags.START)
    val text: @Composable RowScope.() -> Unit = {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = LocalContentColor.current, textAlign = TextAlign.Center)
    }
    if (saved) {
        OutlinedButton(
            onClick = onStart,
            enabled = picker.canPlay,
            modifier = buttonModifier,
            border = BorderStroke(1.dp, if (picker.canPlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            content = text,
        )
    } else {
        Button(onClick = onStart, enabled = picker.canPlay, modifier = buttonModifier, content = text)
    }
}

/** Under Start: the cards a game will move, that it waits while nothing is saved, and whose version it plays into. */
@Composable
private fun StartNotes(picker: Picker) {
    val notes = buildList {
        if (picker.placedChosen > 0) add(pluralStringResource(R.plurals.play_placed_warning, picker.placedChosen, picker.placedChosen))
        if (picker.resume == null) add(stringResource(R.string.play_game_kept))
        if (picker.intoMine) add(stringResource(R.string.play_into_mine_note))
    }
    if (notes.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        notes.forEachIndexed { index, note ->
            Text(
                text = note,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (index == 0 && picker.placedChosen > 0) Modifier.testTag(PlayTestTags.PLACED_WARNING) else Modifier),
                style = FOOTNOTE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** What the game is played over, as the chip says it: the pool, the ranked tiers, one tier, or the board. */
@Composable
private fun Picker.areaName(): String = when {
    everything -> title
    poolOnly -> stringResource(R.string.play_pool)
    ranked -> stringResource(R.string.play_ranked)
    else -> singleTier?.title() ?: title
}

private val FOOTNOTE = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)

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

@Composable
private fun DiscardedRow(onUndo: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(PlayTestTags.DISCARDED_ROW),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HistoryIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.play_snack_saved_discarded),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onUndo,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag(PlayTestTags.UNDO_DISCARD),
            ) {
                Text(stringResource(R.string.play_snack_undo))
            }
        }
    }
}

/**
 * The game put down for this board: when it began, how far it got, and a filled Resume.
 * One that cannot go on as it was keeps today's Start again or Discard, and no progress.
 */
@Composable
private fun SavedGameCard(offer: ResumeOffer, actions: PickerActions) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PlayTestTags.RESUME_ROW),
    ) {
        BoxWithConstraints(Modifier.padding(14.dp)) {
            // A narrow window leaves the text too little beside the button: then it goes under the text.
            val stacked = maxWidth < SAVED_SIDE_BY_SIDE_WIDTH
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stacked) {
                    SavedGameText(offer)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { SavedGameButton(offer, actions) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SavedGameText(offer, Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        SavedGameButton(offer, actions)
                    }
                }
            if (!offer.changed && offer.total > 0) {
                LinearProgressIndicator(
                    progress = { (offer.played.toFloat() / offer.total).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    drawStopIndicator = {},
                )
            }
            }
        }
    }
}

private val SAVED_SIDE_BY_SIDE_WIDTH = 280.dp

@Composable
private fun SavedGameText(offer: ResumeOffer, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = savedFirstLine(offer), style = SAVED_FIRST, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val name = stringResource(offer.kind.nameRes)
        Text(
            text = if (offer.changed) name else stringResource(R.string.play_saved_progress, name, offer.played, offer.total),
            style = SAVED_SECOND,
            color = MaterialTheme.colorScheme.onSurface,
        )
        savedProblem(offer)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SavedGameButton(offer: ResumeOffer, actions: PickerActions) {
    val modifier = Modifier.heightIn(min = 44.dp)
    if (offer.changed && !offer.canStartAgain) {
        // Nothing can pick it up again: letting it go is the one thing left to do.
        FilledTonalButton(onClick = actions.onDiscard, modifier = modifier.testTag(PlayTestTags.DISCARD_SAVED)) {
            Text(stringResource(R.string.play_snack_discard))
        }
    } else {
        Button(
            onClick = if (offer.changed) actions.onStartAgain else actions.onResume,
            modifier = modifier.testTag(PlayTestTags.RESUME),
        ) {
            Text(stringResource(if (offer.changed) R.string.play_start_again else R.string.play_resume))
        }
    }
}

private val SAVED_FIRST = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
private val SAVED_SECOND = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)

/** «Your game · started 5 days ago», and the cards that joined a Swiss game since it was put down. */
@Composable
private fun savedFirstLine(offer: ResumeOffer): String {
    val days = remember(offer.startedAt) { daysSince(offer.startedAt) }
    val started = when (days) {
        0 -> stringResource(R.string.play_your_game_today)
        1 -> stringResource(R.string.play_your_game_yesterday)
        else -> pluralStringResource(R.plurals.play_your_game_days_ago, days, days)
    }
    if (offer.changed || offer.joined == 0) return started
    return started + " · " + pluralStringResource(R.plurals.play_new_cards_join, offer.joined, offer.joined)
}

/** Why a game put down cannot go on as it was. */
@Composable
private fun savedProblem(offer: ResumeOffer): String? = when {
    offer.tierGone -> stringResource(R.string.play_tier_gone)
    offer.changed && !offer.canStartAgain -> stringResource(R.string.play_too_few_to_start_again)
    offer.changed -> stringResource(R.string.play_board_changed)
    else -> null
}

/** Calendar days, not periods of 24 hours: a game begun last night was begun yesterday. */
internal fun daysSince(startedAt: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
    val then = Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(then, today).toInt().coerceAtLeast(0)
}

/** The colours of a game, chosen or not: a tile sits on the container colour, a row on an outline. */
private data class OptionColors(val background: Color, val content: Color, val quiet: Color, val icon: Color)

@Composable
private fun optionColors(selected: Boolean, filled: Boolean): OptionColors {
    val scheme = MaterialTheme.colorScheme
    if (selected) {
        val content = scheme.onPrimaryContainer
        return OptionColors(background = scheme.primaryContainer, content = content, quiet = content, icon = content)
    }
    return OptionColors(
        background = if (filled) scheme.surfaceContainerHighest else Color.Transparent,
        content = scheme.onSurface,
        quiet = scheme.onSurfaceVariant,
        icon = scheme.onSurfaceVariant,
    )
}

@Composable
private fun Modifier.gameOption(kind: GameKind, selected: Boolean, onSelect: () -> Unit, filled: Boolean): Modifier {
    val shape = RoundedCornerShape(16.dp)
    val outlined = !selected && !filled
    return clip(shape)
        .background(optionColors(selected, filled).background)
        .then(if (outlined) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
        .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
        .testTag(PlayTestTags.game(kind))
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
            enabled = true,
            onToggle = actions.onToggleEverything,
        )
        if (picker.offersRanked) {
            CheckRow(
                title = stringResource(R.string.play_ranked),
                count = picker.rankedCount,
                checked = picker.ranked,
                enabled = true,
                onToggle = actions.onToggleRanked,
                tag = PlayTestTags.RANKED,
            )
        }
        CardSelectionOptions(picker, actions)
    }
    SelectedCounter(picker, Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp))
    SheetActions {
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        ContinueButton(picker, actions.onShowGames)
    }
}

@Composable
private fun SelectedCounter(picker: Picker, modifier: Modifier = Modifier) {
    Text(
        text = pluralStringResource(R.plurals.play_selected, picker.count, picker.count, picker.total),
        modifier = modifier.testTag(PlayTestTags.COUNTER),
        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        GameKind.Smart -> R.string.play_game_smart
        GameKind.Swiss -> R.string.play_game_swiss
        GameKind.Hill -> R.string.play_game_hill
        GameKind.Bracket -> R.string.play_game_bracket
    }

@get:StringRes
private val GameKind.shortLineRes: Int
    get() = when (this) {
        GameKind.Smart -> R.string.play_game_smart_short
        GameKind.Swiss -> R.string.play_game_swiss_short
        GameKind.Hill -> R.string.play_game_hill_short
        GameKind.Bracket -> R.string.play_game_bracket_short
    }

private val WIDE_TITLE = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
private val WIDE_SUBTITLE = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

/**
 * Sideways the dialog is wide and low: the four games abreast, and the cards
 * at its foot beside Cancel and Start, so a saved game and the games fit its height.
 */
@Composable
private fun ColumnScope.WideGamesPage(picker: Picker, actions: PickerActions) {
    Column(
        Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp)
            .testTag(PlayTestTags.PICKER),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayHeading(picker, actions, style = WIDE_TITLE)
        GameChoice(picker, actions, across = PlayGames.size)
        StartNotes(picker)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardsChip(picker, actions.onShowCards)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.play_cancel)) }
        StartButton(picker, actions.onStart, Modifier)
    }
}

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
            text = pluralStringResource(R.plurals.play_cards, picker.total, picker.total),
            modifier = Modifier.weight(1f, fill = false),
            style = WIDE_SUBTITLE,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(6.dp))
    // The groups, then Everything and Ranked, which stand for themselves.
    val cells: List<Any> = picker.groups + listOfNotNull(EVERYTHING_CELL, RANKED_CELL.takeIf { picker.offersRanked })
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
                pair.forEach { cell ->
                    when (cell) {
                        EVERYTHING_CELL -> EverythingCell(picker, actions.onToggleEverything, Modifier.weight(1f))
                        RANKED_CELL -> RankedCell(picker, actions.onToggleRanked, Modifier.weight(1f))
                        is CardGroup -> GroupCell(cell, cell.tierId in picker.chosen, { actions.onToggle(cell.tierId) }, Modifier.weight(1f))
                        else -> Unit
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        CardSelectionOptions(picker, actions)
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
private fun RankedCell(picker: Picker, onToggle: () -> Unit, modifier: Modifier) {
    CellRow(checked = picker.ranked, enabled = true, onToggle = onToggle, tag = PlayTestTags.RANKED, modifier = modifier) {
        CellText(stringResource(R.string.play_ranked), picker.rankedCount, true)
    }
}

private const val EVERYTHING_CELL = "everything"
private const val RANKED_CELL = "ranked"

@Composable
private fun EverythingCell(picker: Picker, onToggle: () -> Unit, modifier: Modifier) {
    val enabled = true
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
            enabled -> quiet
            else -> quiet.copy(alpha = 0.38f)
        },
    )
}

private val CELL_NAME = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
private val SWATCH_LETTER = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
