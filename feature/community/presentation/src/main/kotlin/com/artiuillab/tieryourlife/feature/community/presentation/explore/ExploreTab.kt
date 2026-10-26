package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStats
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.flow.distinctUntilChanged

/** What the tiles do: open a section, follow or stop following it from the long-press menu. */
internal class TileActions(
    val onOpen: (ListCategory) -> Unit = {},
    val onFollow: (ListCategory) -> Unit = {},
    val onUnfollow: (ListCategory) -> Unit = {},
)

/** What a card in a feed does. */
internal class CardActions(
    val onOpen: (PublishedListSummary) -> Unit = {},
    val onLongPress: (PublishedListSummary) -> Unit = {},
)

private val FeedSpacing = 12.dp

/** The nine tiles, laid out the way the window has room for (§2.1, §6.1, §7.1). */
@Composable
internal fun TilesBlock(
    layout: HomeLayout,
    sections: List<SectionStats>,
    countsKnown: Boolean,
    actions: TileActions,
    modifier: Modifier = Modifier,
) {
    val byId = sections.associateBy { it.section }
    val tile: @Composable (ListCategory, Modifier, Boolean) -> Unit = { section, tileModifier, centred ->
        val stats = byId[section]
        SectionTile(
            section = section,
            newThisWeek = stats?.newThisWeek?.takeIf { countsKnown && section != ListCategory.Other },
            following = stats?.following == true,
            onOpen = { actions.onOpen(section) },
            onFollow = { actions.onFollow(section) },
            onUnfollow = { actions.onUnfollow(section) },
            modifier = tileModifier,
            centredLabel = centred,
            style = if (layout == HomeLayout.Cover) TileStyle.Cover else TileStyle(),
            longPressMenu = layout != HomeLayout.Cover,
        )
    }
    val all = ListCategory.entries
    when (layout) {
        HomeLayout.CompactHeight -> LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .edgeToEdge(gridSide(layout)),
            contentPadding = PaddingValues(top = 12.dp, start = gridSide(layout), end = gridSide(layout)),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(all, key = { it.id }) { section -> tile(section, Modifier.size(width = 160.dp, height = 88.dp), false) }
        }

        HomeLayout.Portrait -> Column(modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            all.filter { it != ListCategory.Other }.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { section -> tile(section, Modifier.weight(1f).height(96.dp), false) }
                }
            }
            tile(ListCategory.Other, Modifier.fillMaxWidth().height(56.dp), true)
        }

        HomeLayout.Medium, HomeLayout.Cover -> {
            val height = if (layout == HomeLayout.Cover) 84.dp else 120.dp
            val gap = if (layout == HomeLayout.Cover) 6.dp else 10.dp
            Column(modifier.padding(top = if (layout == HomeLayout.Cover) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                all.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { section -> tile(section, Modifier.weight(1f).height(height), false) }
                    }
                }
            }
        }

        HomeLayout.Wide -> Column(modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(all.take(5), all.drop(5)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { section -> tile(section, Modifier.weight(1f).height(112.dp), false) }
                    if (row.size < 5) Spacer(Modifier.weight((5 - row.size).toFloat()))
                }
            }
        }
    }
}

/** The Explore tab (§2): tiles, the heading, and the popular feed in as many columns as fit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreTab(
    state: ExploreUiState,
    layout: HomeLayout,
    gridState: LazyGridState,
    tiles: TileActions,
    cards: CardActions,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onScrolledDown: (Boolean) -> Unit,
) {
    val skeletonAlpha = rememberSkeletonAlpha()
    ScrolledDownWatcher(gridState, onScrolledDown)
    NearEndWatcher(gridState, state.feed.lists.size, onLoadMore)
    PullToRefreshBox(isRefreshing = state.feed.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.feedColumns),
                state = gridState,
                modifier = Modifier
                    .then(if (layout == HomeLayout.Wide) Modifier.widthIn(max = WideContentMax) else Modifier)
                    .fillMaxSize()
                    .testTag(ExploreTestTags.SCREEN),
                contentPadding = PaddingValues(start = gridSide(layout), end = gridSide(layout), bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(feedSpacing(layout)),
                verticalArrangement = Arrangement.spacedBy(feedSpacing(layout)),
            ) {
                item(key = "tiles", span = { GridItemSpan(maxLineSpan) }) {
                    TilesBlock(layout, state.sections, state.countsKnown, tiles)
                }
                item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.explore_popular_this_week),
                        modifier = Modifier
                            .padding(top = if (layout == HomeLayout.CompactHeight) 4.dp else 10.dp)
                            .testTag(ExploreTestTags.HEADING),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                feedItems(state.feed, layout, skeletonAlpha, cards, onRetry, reasonFor = { null })
            }
        }
    }
}

private fun gridSide(layout: HomeLayout) = if (layout == HomeLayout.Wide) 24.dp else 16.dp

private fun feedSpacing(layout: HomeLayout) = if (layout == HomeLayout.Wide) 16.dp else FeedSpacing

/** The feed part of a grid: skeletons, cards (or the note where one was hidden), or the block that says why there are none. */
internal fun LazyGridScope.feedItems(
    pane: FeedPane,
    layout: HomeLayout,
    skeletonAlpha: Float,
    cards: CardActions,
    onRetry: () -> Unit,
    reasonFor: @Composable (PublishedListSummary) -> String?,
) {
    when (pane.phase) {
        FeedPhase.Loading -> items(count = SKELETONS, key = { "skeleton-$it" }) {
            SkeletonCard(skeletonAlpha, coverHeight = layout.coverHeight)
        }

        FeedPhase.Failed -> item(key = "failed", span = { GridItemSpan(maxLineSpan) }) {
            FeedFailedBlock(retrying = pane.retrying, onRetry = onRetry)
        }

        FeedPhase.Ready -> {
            pane.lists.forEach { summary ->
                item(key = summary.id) {
                    val hidden = pane.justHidden[summary.id]
                    if (hidden != null) {
                        HiddenCard(reported = hidden)
                    } else {
                        FeedCard(
                            summary = summary,
                            onClick = { cards.onOpen(summary) },
                            onLongClick = { cards.onLongPress(summary) },
                            coverHeight = layout.coverHeight,
                            reason = reasonFor(summary),
                        )
                    }
                }
            }
        }
    }
}

private const val SKELETONS = 6

/** Tells the loader whether the person has moved off the top, so a fresh answer waits behind the pill. */
@Composable
internal fun ScrolledDownWatcher(gridState: LazyGridState, onScrolledDown: (Boolean) -> Unit) {
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex > 0 }
            .distinctUntilChanged()
            .collect(onScrolledDown)
    }
}

@Composable
internal fun NearEndWatcher(gridState: LazyGridState, shown: Int, onNearEnd: () -> Unit) {
    LaunchedEffect(gridState, shown) {
        if (shown == 0) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last -> if (last >= gridState.layoutInfo.totalItemsCount - LOAD_AHEAD) onNearEnd() }
    }
}

private const val LOAD_AHEAD = 6
