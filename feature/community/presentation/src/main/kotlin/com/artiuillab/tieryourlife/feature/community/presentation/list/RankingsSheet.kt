package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.community.presentation.feed.components.Day
import com.artiuillab.tieryourlife.feature.community.presentation.feed.components.dayOf
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val LOAD_MORE_ROWS_AHEAD = 3

/**
 * The people who placed this list's cards their own way: the author first,
 * this reader next, the rest newest first. Choosing one fills the left half
 * of the switch with their placing.
 */
@Composable
internal fun RankingsSheet(
    state: CommunityListUiState.Success,
    onDismiss: () -> Unit,
    onChoose: (Ranking?) -> Unit,
    onLoadMore: () -> Unit,
    onWithdraw: () -> Unit,
) {
    AdaptiveSheet(onDismiss = onDismiss, modifier = Modifier.testTag(CommunityListTestTags.RANKINGS_SHEET)) {
        Text(
            text = stringResource(R.string.community_rankings_title),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.community_rankings_body),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val listState = rememberLazyListState()
        val shown = state.rankings.size
        LaunchedEffect(listState, shown, state.moreRankings) {
            if (!state.moreRankings) return@LaunchedEffect
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                .collect { lastVisible -> if (lastVisible >= shown - LOAD_MORE_ROWS_AHEAD) onLoadMore() }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = false)) {
            item(key = "author") {
                RankingRow(
                    name = state.authorName,
                    photoUrl = state.authorPhotoUrl,
                    note = stringResource(R.string.community_ranking_author),
                    selected = state.chosenRanking == null,
                    onClick = { onChoose(null) },
                    modifier = Modifier.testTag(CommunityListTestTags.RANKING_AUTHOR),
                )
            }
            items(state.rankings, key = { it.uid }) { ranking ->
                RankingRow(
                    name = if (ranking.mine) stringResource(R.string.community_ranking_you) else ranking.authorName,
                    photoUrl = ranking.authorPhotoUrl,
                    note = whenPlaced(ranking.updatedAtMillis),
                    selected = state.chosenRanking?.uid == ranking.uid,
                    onClick = { onChoose(ranking) },
                    modifier = Modifier.testTag(CommunityListTestTags.ranking(ranking.uid)),
                    trailing = if (ranking.mine) {
                        {
                            TextButton(onClick = onWithdraw, modifier = Modifier.testTag(CommunityListTestTags.WITHDRAW_RANKING)) {
                                Text(stringResource(R.string.community_ranking_withdraw))
                            }
                        }
                    } else {
                        null
                    },
                )
            }
            if (state.rankingsLoading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingRow(
    name: String,
    photoUrl: String?,
    note: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthorFace(photoUrl = photoUrl, name = name, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) trailing()
        if (selected) CheckIcon(iconSize = 22.dp, color = MaterialTheme.colorScheme.primary)
    }
}

/** When a placing was last sent, the way the feed says days. */
@Composable
private fun whenPlaced(millis: Long): String = when (val day = dayOf(millis, LocalDate.now(), ZoneId.systemDefault())) {
    Day.Today -> stringResource(R.string.community_day_today)
    Day.Yesterday -> stringResource(R.string.community_day_yesterday)
    is Day.On -> day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
