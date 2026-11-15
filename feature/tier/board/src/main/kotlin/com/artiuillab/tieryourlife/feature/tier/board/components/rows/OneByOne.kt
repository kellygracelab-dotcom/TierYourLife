package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.BoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.ui.ViewCarouselIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** A card on the board tapped while ranking one at a time: [selectedId] is drawn picked, [onPick] picks or lets go. */
@Stable
class CardPicking(val selectedId: Long?, val onPick: (itemId: Long) -> Unit)

/** Null while the board is dragged as usual: a tap on a card does nothing then, and a double tap opens it. */
val LocalCardPicking = compositionLocalOf<CardPicking?> { null }

/** Where a placed card came from, to put it back. */
private data class Placement(val itemId: Long, val fromTierId: Long, val fromIndex: Int)

/**
 * Ranking one card at a time: the pool's cards come up in the pool's order,
 * a tier's button sends the card to that tier's end, Skip passes it over
 * without moving it, and a card tapped on the board comes up instead, to move
 * again or go back to the pool. Agreed with the site, 16.11.2026.
 */
@Stable
class OneByOne internal constructor(private val move: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit) {
    var selectedId by mutableStateOf<Long?>(null)
        private set
    private var skipped by mutableStateOf(emptySet<Long>())
    private val placements = mutableStateListOf<Placement>()

    val canUndo: Boolean get() = placements.isNotEmpty()

    /** The card a tier's button would place: the one picked on the board, or the pool's next. */
    fun current(list: TierList): TierItem? {
        val picked = selectedId?.let { id -> list.ranked().flatMap { it.items }.firstOrNull { it.id == id } }
        if (picked != null) return picked
        val pool = list.pool()?.items.orEmpty()
        return pool.firstOrNull { it.id !in skipped } ?: pool.firstOrNull()
    }

    val isPicked: Boolean get() = selectedId != null

    fun pick(itemId: Long) {
        selectedId = itemId.takeUnless { it == selectedId }
    }

    fun place(list: TierList, item: TierItem, tier: Tier) {
        val from = list.where(item.id) ?: return
        val end = if (from.fromTierId == tier.id) tier.items.size - 1 else tier.items.size
        if (from.fromTierId == tier.id && from.fromIndex == end) return done()
        placements += from
        move(item.id, tier.id, end)
        done()
    }

    fun toPool(list: TierList, item: TierItem) {
        val pool = list.pool() ?: return
        val from = list.where(item.id) ?: return
        if (from.fromTierId == pool.id) return done()
        placements += from
        move(item.id, pool.id, pool.items.size)
        done()
    }

    fun skip(list: TierList, item: TierItem) {
        if (selectedId != null) return done()
        val pool = list.pool()?.items.orEmpty()
        val passed = skipped + item.id
        // Past the last card the pool starts again from the first it passed.
        skipped = if (pool.all { it.id in passed }) emptySet() else passed
    }

    fun undo() {
        val last = placements.removeLastOrNull() ?: return
        skipped = skipped - last.itemId
        move(last.itemId, last.fromTierId, last.fromIndex)
        done()
    }

    private fun done() {
        selectedId = null
    }
}

private fun TierList.ranked() = tiers.filterNot { it.isPool }

private fun TierList.pool() = tiers.firstOrNull { it.isPool }

private fun TierList.where(itemId: Long): Placement? = tiers.firstNotNullOfOrNull { tier ->
    tier.items.indexOfFirst { it.id == itemId }.takeIf { it >= 0 }?.let { Placement(itemId, tier.id, it) }
}

@Composable
internal fun rememberOneByOne(move: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit): OneByOne {
    val latest by rememberUpdatedState(move)
    return remember { OneByOne { itemId, toTierId, toPosition -> latest(itemId, toTierId, toPosition) } }
}

/** The pool's switch where one at a time is [offered], or null; [compact] on the phone's tray, whose head keeps one line. */
internal fun oneByOneSwitch(offered: Boolean, memory: BoardLayoutMemory, compact: Boolean = false): (@Composable () -> Unit)? = if (offered) {
    { OneByOneToggle(on = memory.oneCardAtATime, onToggle = { memory.oneCardAtATime = !memory.oneCardAtATime }, compact = compact) }
} else {
    null
}

/** The switch in the pool's head between dragging and one card at a time. */
@Composable
internal fun OneByOneToggle(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val description = stringResource(R.string.one_by_one_on)
    val tint = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .semantics {
                role = Role.Switch
                selected = on
                contentDescription = description
            }
            .testTag(TierDetailTestTags.ONE_BY_ONE_TOGGLE)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 6.dp),
    ) {
        if (compact) {
            ViewCarouselIcon(20.dp, tint)
        } else {
            Text(text = stringResource(R.string.one_by_one_toggle), style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}

/**
 * The pool while ranking one card at a time: the card large, a button in each
 * tier's colour, Skip, and Undo once something was placed. [beside] lays the
 * buttons beside the card, as under the rows; a narrow column puts them under it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OneByOnePanel(
    list: TierList,
    state: OneByOne,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    beside: Boolean = true,
) {
    val ranked = list.ranked()
    val left = list.pool()?.items?.size ?: 0
    val card = state.current(list)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event -> card != null && onKey(event, list, ranked, card, state) }
            .testTag(TierDetailTestTags.ONE_BY_ONE_PANEL)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (card == null) {
            AllPlaced(canUndo = state.canUndo, onUndo = state::undo, onExit = onExit)
            return@Column
        }
        val buttons: @Composable () -> Unit = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ranked.forEach { tier -> TierButton(tier) { state.place(list, card, tier) } }
            }
        }
        if (beside) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigCard(card, picked = state.isPicked)
                Box(Modifier.weight(1f)) { buttons() }
            }
        } else {
            Box(Modifier.align(Alignment.CenterHorizontally)) { BigCard(card, picked = state.isPicked) }
            buttons()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.one_by_one_left, left, left),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.canUndo) {
                TextButton(onClick = state::undo, modifier = Modifier.testTag(TierDetailTestTags.ONE_BY_ONE_UNDO)) {
                    Text(stringResource(R.string.tier_detail_snackbar_undo))
                }
            }
            if (state.isPicked) {
                TextButton(onClick = { state.toPool(list, card) }, modifier = Modifier.testTag(TierDetailTestTags.ONE_BY_ONE_TO_POOL)) {
                    Text(stringResource(R.string.one_by_one_to_pool))
                }
            }
            TextButton(onClick = { state.skip(list, card) }, modifier = Modifier.testTag(TierDetailTestTags.ONE_BY_ONE_SKIP)) {
                Text(stringResource(R.string.one_by_one_skip))
            }
        }
    }
}

/** 1 to 9 for the tiers, Space or → to skip, Backspace back to the pool, Ctrl+Z to undo: the site's keys. */
private fun onKey(event: KeyEvent, list: TierList, ranked: List<Tier>, card: TierItem, state: OneByOne): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val digit = DIGITS.indexOf(event.key)
    when {
        digit >= 0 -> ranked.getOrNull(digit)?.let { state.place(list, card, it) } ?: return false
        event.key == Key.Spacebar || event.key == Key.DirectionRight -> state.skip(list, card)
        event.key == Key.Backspace && state.isPicked -> state.toPool(list, card)
        event.key == Key.Z && event.isCtrlPressed -> state.undo()
        else -> return false
    }
    return true
}

private val DIGITS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

@Composable
private fun BigCard(card: TierItem, picked: Boolean) {
    AnimatedContent(targetState = card, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it.id }, label = "current card") { shown ->
        Box(
            Modifier
                .testTag(TierDetailTestTags.ONE_BY_ONE_CARD)
                .semantics { contentDescription = shown.title },
        ) {
            ProvideTextCardRoom(listOf(shown), BIG_CARD_WIDTH, BIG_CARD_HEIGHT, 0.dp) {
                ItemTile(item = shown, width = BIG_CARD_WIDTH, height = BIG_CARD_HEIGHT)
            }
            if (picked) PickedOutline(BIG_CARD_WIDTH, BIG_CARD_HEIGHT)
        }
    }
}

@Composable
private fun TierButton(tier: Tier, onClick: () -> Unit) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Box(
        modifier = Modifier
            .heightIn(min = TIER_BUTTON_HEIGHT)
            .widthIn(min = TIER_BUTTON_WIDTH, max = TIER_BUTTON_MAX_WIDTH)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.band)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .testTag(TierDetailTestTags.oneByOneTier(tier.id))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AllPlaced(canUndo: Boolean, onUndo: () -> Unit, onExit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.one_by_one_done),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (canUndo) {
            TextButton(onClick = onUndo, modifier = Modifier.testTag(TierDetailTestTags.ONE_BY_ONE_UNDO)) {
                Text(stringResource(R.string.tier_detail_snackbar_undo))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.testTag(TierDetailTestTags.ONE_BY_ONE_EXIT)) {
            Text(stringResource(R.string.one_by_one_exit))
        }
    }
}

/** The ring around a card picked to move, on the board and in the panel. */
@Composable
internal fun PickedOutline(width: Dp, height: Dp) {
    Box(Modifier.size(width, height).border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)))
}

private val BIG_CARD_WIDTH = 96.dp
private val BIG_CARD_HEIGHT = 136.dp
private val TIER_BUTTON_HEIGHT = 44.dp
private val TIER_BUTTON_WIDTH = 52.dp
private val TIER_BUTTON_MAX_WIDTH = 140.dp
