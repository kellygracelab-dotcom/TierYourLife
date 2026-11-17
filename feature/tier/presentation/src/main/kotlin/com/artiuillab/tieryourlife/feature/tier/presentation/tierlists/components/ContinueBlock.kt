package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
internal fun ContinueBlock(started: List<StartedList>, onContinue: (StartedList, resumeGame: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag(TierListsTestTags.CONTINUE_BLOCK), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(
            text = stringResource(R.string.started_count, started.size),
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        )
        started.forEach { list -> StartedRow(list, onContinue) }
    }
}

@Composable
private fun StartedRow(list: StartedList, onContinue: (StartedList, resumeGame: Boolean) -> Unit) {
    val continueOf = stringResource(R.string.started_continue_of, list.title)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(TierListsTestTags.started(list.listId)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
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
}

/** "Anime · 7 of 25 placed", as on the site; progress kept before the section was stored says only the count. */
@Composable
private fun placedLine(list: StartedList): String {
    val section = list.section ?: return pluralStringResource(R.plurals.started_placed, list.placed, list.placed, list.total)
    return pluralStringResource(R.plurals.started_placed_in, list.placed, sectionLabel(section), list.placed, list.total)
}
