package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.presentation.R

/** The reader's version on screen over a placing of somebody's: theirs is a tap away, to look at. */
internal val CommunityListUiState.Success.mineOverTheirs: Boolean
    get() = twoHalves && arranged && showing == Showing.Mine

/** "Your version · See Olena’s" under the title: no switch, one link to their placing. */
@Composable
internal fun YourVersionLine(state: CommunityListUiState.Success, onSeeTheirs: () -> Unit, modifier: Modifier = Modifier) {
    if (!state.mineOverTheirs) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.community_your_version) + " · ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = seeTheirs(state),
            modifier = Modifier
                .clickable(onClick = onSeeTheirs)
                .testTag(CommunityListTestTags.SEE_THEIRS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The same link in a bar, where the line under the title has no room of its own. */
@Composable
internal fun SeeTheirsButton(state: CommunityListUiState.Success, onSeeTheirs: () -> Unit, height: Dp) {
    TextButton(
        onClick = onSeeTheirs,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier
            .height(height)
            .testTag(CommunityListTestTags.SEE_THEIRS),
    ) {
        Text(seeTheirs(state), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "Olena’s ranking": whose placing is on screen while the reader only looks at it. */
@Composable
internal fun TheirsRankingLabel(state: CommunityListUiState.Success, modifier: Modifier = Modifier) {
    val name = state.theirsName
    Text(
        text = if (name != null) stringResource(R.string.community_theirs_ranking, name) else stringResource(R.string.community_theirs_ranking_unnamed),
        modifier = modifier.testTag(CommunityListTestTags.THEIRS_RANKING),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** From their placing back to the reader's own version, which waited where it was. */
@Composable
internal fun BackToYoursButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = modifier.testTag(CommunityListTestTags.BACK_TO_YOURS),
    ) {
        Text(stringResource(R.string.community_back_to_yours), maxLines = 1)
    }
}

@Composable
private fun seeTheirs(state: CommunityListUiState.Success): String {
    val name = state.theirsName
    return if (name != null) stringResource(R.string.community_see_theirs, name) else stringResource(R.string.community_see_theirs_unnamed)
}
