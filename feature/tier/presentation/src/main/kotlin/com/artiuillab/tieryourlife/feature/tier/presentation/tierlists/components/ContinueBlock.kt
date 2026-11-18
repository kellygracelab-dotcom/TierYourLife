package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.DeleteOutlineIcon
import com.artiuillab.tieryourlife.core.theme.ui.SectionLabel
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.StartedList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Other people's lists begun here, above one's own: otherwise the way back to
 * one was to find the list again in Community. Continue opens it where it was
 * left; Resume game, when a game of it was put down, opens it with the game.
 */
@Composable
internal fun ContinueBlock(
    started: List<StartedList>,
    onContinue: (StartedList, resumeGame: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDiscard: (StartedList) -> Unit = {},
) {
    Column(modifier.fillMaxWidth().testTag(TierListsTestTags.CONTINUE_BLOCK), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(
            text = stringResource(R.string.started_count, started.size),
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        )
        started.forEach { list ->
            key(list.listId) { SwipeToDiscard(list, onDiscard) { StartedRow(list, onContinue, onDiscard) } }
        }
    }
}

/**
 * Either way: past a third of the width the row goes and its arrangement with it, Undo bringing both back.
 * Under the row, at the edge it left, the bin says what letting go does.
 */
@Composable
private fun SwipeToDiscard(list: StartedList, onDiscard: (StartedList) -> Unit, row: @Composable () -> Unit) {
    // Not saveable: the grid keeps a gone block's saved state, and a row brought back by Undo would
    // come back already swiped away and go again at once.
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold = { it / 3 }) }
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) onDiscard(list)
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val direction = state.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val tint = MaterialTheme.colorScheme.onErrorContainer
                        DeleteOutlineIcon(20.dp, tint)
                        Text(stringResource(R.string.started_discard), color = tint, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) { row() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartedRow(list: StartedList, onContinue: (StartedList, resumeGame: Boolean) -> Unit, onDiscard: (StartedList) -> Unit) {
    val continueOf = stringResource(R.string.started_continue_of, list.title)
    val discardLabel = stringResource(R.string.started_discard)
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TierListsTestTags.started(list.listId))
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(discardLabel) {
                        onDiscard(list)
                        true
                    },
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(Modifier.combinedClickable(onClick = { onContinue(list, false) }, onLongClick = { menuOpen = true })) {
            StartedRowContent(list, continueOf, onContinue)
            StartedMenu(list, menuOpen, onDismiss = { menuOpen = false }, onContinue, onDiscard)
        }
    }
}

/** The long press's menu, as the site's: the way to discard that does not need the swipe to be known. */
@Composable
private fun StartedMenu(
    list: StartedList,
    open: Boolean,
    onDismiss: () -> Unit,
    onContinue: (StartedList, resumeGame: Boolean) -> Unit,
    onDiscard: (StartedList) -> Unit,
) {
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.started_continue)) },
            onClick = {
                onDismiss()
                onContinue(list, false)
            },
        )
        if (list.gameSaved) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.started_resume)) },
                onClick = {
                    onDismiss()
                    onContinue(list, true)
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.started_discard_arrangement), color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDiscard(list)
            },
            modifier = Modifier.testTag(TierListsTestTags.started(list.listId) + DISCARD_TAG),
        )
    }
}

@Composable
private fun StartedRowContent(list: StartedList, continueOf: String, onContinue: (StartedList, resumeGame: Boolean) -> Unit) {
    Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = list.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = placedLine(list),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { if (list.total > 0) list.placed.toFloat() / list.total else 0f },
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp),
                drawStopIndicator = {},
            )
            if (list.gameSaved) {
                TextButton(
                    onClick = { onContinue(list, true) },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    modifier = Modifier.testTag(TierListsTestTags.resumeGame(list.listId)),
                ) { Text(stringResource(R.string.started_resume)) }
            }
        }
        FilledTonalButton(
            onClick = { onContinue(list, false) },
            modifier = Modifier
                .semantics { contentDescription = continueOf }
                .testTag(TierListsTestTags.continueList(list.listId)),
        ) { Text(stringResource(R.string.started_continue), maxLines = 1) }
    }
}

/** "Anime · 7 of 25 placed", as on the site; progress kept before the section was stored says only the count. */
@Composable
private fun placedLine(list: StartedList): String {
    val section = list.section ?: return pluralStringResource(R.plurals.started_placed, list.placed, list.placed, list.total)
    return pluralStringResource(R.plurals.started_placed_in, list.placed, sectionLabel(section), list.placed, list.total)
}

/** On the row's own tag, for its menu's Discard. */
internal const val DISCARD_TAG = "_discard"
