package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorPill

private val BAR_BUTTON_HEIGHT = 36.dp
private val BAR_SWITCH_HEIGHT = 32.dp
private val BAR_BUTTON_MAX_WIDTH = 168.dp
private val BAR_AUTHOR_MAX_WIDTH = 160.dp

/**
 * The reader's strip, said in the bar: whose arrangement is on screen and the
 * save, without the sentence under them. In a short window's single bar the
 * author comes along, since the line that names them has no room of its own,
 * and so do the rankings when there are any.
 */
@Composable
internal fun RowScope.ReaderBarControls(
    state: CommunityListUiState.Success,
    compact: Boolean,
    onShow: (Showing) -> Unit,
    onSaveClick: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onOpenAuthor: () -> Unit,
    onOpenRankings: () -> Unit,
) {
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
    if (compact && state.rankingCount > 0) {
        AssistChip(
            onClick = onOpenRankings,
            label = { Text(pluralStringResource(R.plurals.community_rankings_count, state.rankingCount, state.rankingCount)) },
            modifier = Modifier.padding(end = 8.dp).testTag(CommunityListTestTags.RANKINGS),
        )
    }
    if (state.knowsTheirs) {
        WhoseArrangement(
            showing = state.showing,
            theirsLabel = state.chosenRanking?.authorName ?: stringResource(R.string.community_showing_theirs),
            onShow = onShow,
            height = BAR_SWITCH_HEIGHT,
        )
        Spacer(Modifier.width(8.dp))
    }
    val savedId = state.savedId
    Button(
        onClick = { if (savedId != null) onOpenSaved(savedId) else onSaveClick() },
        enabled = savedId != null || !state.saving,
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = Modifier
            .height(BAR_BUTTON_HEIGHT)
            .widthIn(max = BAR_BUTTON_MAX_WIDTH)
            .testTag(if (savedId != null) CommunityListTestTags.OPEN_SAVED else CommunityListTestTags.SAVE),
    ) {
        Text(
            text = stringResource(if (savedId != null) R.string.community_action_open else R.string.community_save_to_my_lists),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
