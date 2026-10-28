package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.BookmarkAddIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorPill
import com.artiuillab.tieryourlife.feature.tier.board.BoardBar
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes

private val BAR_BUTTON_HEIGHT = 36.dp
private val BAR_SWITCH_HEIGHT = 32.dp
private val TABLET_CONTROL_HEIGHT = 40.dp
private val BAR_BUTTON_MAX_WIDTH = 168.dp
private val BAR_AUTHOR_MAX_WIDTH = 160.dp

/**
 * The reader's strip, said in the bar: whose arrangement is on screen and the
 * save, without the sentence under them. In a short window's single bar the
 * author comes along, since the line that names them has no room of its own,
 * and so do the rankings when there are any. A tablet held upright has the
 * room for a taller switch and a tonal save with its icon.
 */
@Composable
internal fun RowScope.ReaderBarControls(
    state: CommunityListUiState.Success,
    mode: BoardBar,
    onShow: (Showing) -> Unit,
    onSaveClick: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onOpenAuthor: () -> Unit,
    onOpenRankings: () -> Unit,
) {
    val compact = mode == BoardBar.Compact
    if (compact && !state.editorial) {
        AuthorPill(
            name = state.authorName,
            photoUrl = state.authorPhotoUrl,
            onClick = onOpenAuthor,
            modifier = Modifier.widthIn(max = BAR_AUTHOR_MAX_WIDTH),
            testTag = CommunityListTestTags.BAR_AUTHOR,
        )
        Spacer(Modifier.width(8.dp))
    }
    if (compact && state.takeCount > 0) {
        AssistChip(
            onClick = onOpenRankings,
            label = { Text(pluralStringResource(R.plurals.community_rankings_count, state.takeCount, state.takeCount)) },
            modifier = Modifier.padding(end = 8.dp).testTag(CommunityListTestTags.RANKINGS),
        )
    }
    if (state.knowsTheirs) {
        WhoseArrangement(
            showing = state.showing,
            theirsLabel = state.chosenRanking?.authorName ?: stringResource(R.string.community_showing_theirs),
            onShow = onShow,
            height = if (mode == BoardBar.Docked) TABLET_CONTROL_HEIGHT else BAR_SWITCH_HEIGHT,
        )
        Spacer(Modifier.width(8.dp))
    }
    SaveInBar(state = state, mode = mode, onSaveClick = onSaveClick, onOpenSaved = onOpenSaved)
}

/** Save, or Open once the copy exists; a tablet's bar has a tonal one with its icon. */
@Composable
private fun SaveInBar(state: CommunityListUiState.Success, mode: BoardBar, onSaveClick: () -> Unit, onOpenSaved: (Long) -> Unit) {
    val savedId = state.savedId
    val onClick = { if (savedId != null) onOpenSaved(savedId) else onSaveClick() }
    val tag = if (savedId != null) CommunityListTestTags.OPEN_SAVED else CommunityListTestTags.SAVE
    if (mode == BoardBar.Docked && savedId == null) {
        val description = stringResource(R.string.community_save_to_my_lists)
        FilledTonalButton(
            onClick = onClick,
            enabled = !state.saving,
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
            modifier = Modifier
                .height(TABLET_CONTROL_HEIGHT)
                .semantics { contentDescription = description }
                .testTag(tag),
        ) {
            BookmarkAddIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.community_save_short), maxLines = 1)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = savedId != null || !state.saving,
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier
                .height(if (mode == BoardBar.Docked) TABLET_CONTROL_HEIGHT else BAR_BUTTON_HEIGHT)
                .widthIn(max = BAR_BUTTON_MAX_WIDTH)
                .testTag(tag),
        ) {
            Text(
                text = stringResource(if (savedId != null) R.string.community_action_open else R.string.community_save_to_my_lists),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A tablet's line under the title: who made the list and in which section, a tap away from their profile. */
@Composable
internal fun AuthorAndSection(state: CommunityListUiState.Success, onOpenAuthor: () -> Unit) {
    val section = state.section?.let { stringResource(it.labelRes) }
    val text = listOfNotNull(state.authorName.takeUnless { state.editorial }, section).joinToString(" · ") + " ›"
    Text(
        text = text,
        modifier = Modifier
            .then(if (state.editorial) Modifier else Modifier.clickable(onClick = onOpenAuthor))
            .testTag(CommunityListTestTags.AUTHOR_AND_SECTION),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
