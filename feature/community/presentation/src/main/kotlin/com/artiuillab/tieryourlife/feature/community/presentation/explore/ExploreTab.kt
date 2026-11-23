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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingActions
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.PickBar
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.PickBarRoom
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.followingItems
import kotlinx.coroutines.flow.distinctUntilChanged

/** What the tiles do, by section id: open a section, follow or stop following it from the long-press menu, or open them all. */
internal class TileActions(
    val onOpen: (String) -> Unit = {},
    val onFollow: (String) -> Unit = {},
    val onUnfollow: (String) -> Unit = {},
    val onOpenAll: () -> Unit = {},
)

/** What a card in a feed does. */
internal class CardActions(
    val onOpen: (PublishedListSummary) -> Unit = {},
    val onLongPress: (PublishedListSummary) -> Unit = {},
)

private val FeedSpacing = 12.dp

/**
 * The section tiles, laid out the way the window has room for (§2.1, §6.1,
 * §7.1), in the order given: as many as the slots hold, the last one leading
 * to all of them when there are more ([homeTiles]).
 */
@Composable
internal fun TilesBlock(
    layout: HomeLayout,
    sections: List<Section>,
    actions: TileActions,
    modifier: Modifier = Modifier,
) {
    val home = homeTiles(sections, layout)
    val style = if (layout == HomeLayout.Cover) TileStyle.Cover else TileStyle()
    val tile: @Composable (Section, Modifier, Boolean) -> Unit = { section, tileModifier, centred ->
        SectionTile(
            section = section,
            following = section.following,
            onOpen = { actions.onOpen(section.id) },
            onFollow = { actions.onFollow(section.id) },
            onUnfollow = { actions.onUnfollow(section.id) },
            modifier = tileModifier,
            centredLabel = centred,
            style = style,
            longPressMenu = layout != HomeLayout.Cover,
        )
    }
    val slot: @Composable (Section?, Modifier) -> Unit = { section, slotModifier ->
        if (section != null) tile(section, slotModifier, false) else AllSectionsTile(home.total, actions.onOpenAll, slotModifier, style = style)
    }
    // Null stands for the slot that leads to every section.
    val slots: List<Section?> = home.tiles + if (home.allSections) listOf(null) else emptyList()
    when (layout) {
        HomeLayout.CompactHeight -> LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .edgeToEdge(gridSide(layout)),
            contentPadding = PaddingValues(top = 12.dp, start = gridSide(layout), end = gridSide(layout)),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(slots, key = { it?.id ?: ALL_SECTIONS_KEY }) { section -> slot(section, Modifier.size(width = 160.dp, height = 88.dp)) }
        }

        HomeLayout.Portrait -> Column(modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TileRows(home.tiles, columns = 2, gap = 10.dp) { section, rowModifier -> tile(section, rowModifier.height(96.dp), false) }
            home.row?.let { tile(it, Modifier.fillMaxWidth().height(56.dp), true) }
            if (home.allSections) AllSectionsTile(home.total, actions.onOpenAll, Modifier.fillMaxWidth().height(56.dp), row = true)
        }

        HomeLayout.Medium, HomeLayout.Cover -> {
            val height = if (layout == HomeLayout.Cover) 70.dp else 120.dp
            val gap = if (layout == HomeLayout.Cover) 6.dp else 10.dp
            Column(modifier.padding(top = if (layout == HomeLayout.Cover) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                TileRows(slots, columns = 3, gap = gap) { section, rowModifier -> slot(section, rowModifier.height(height)) }
            }
        }

        HomeLayout.Wide -> Column(modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TileRows(slots, columns = 5, gap = 12.dp) { section, rowModifier -> slot(section, rowModifier.height(112.dp)) }
        }
    }
}

private const val ALL_SECTIONS_KEY = "all-sections"

/** Rows of [columns] equal tiles; a short last row keeps the same widths and leaves the rest empty. */
@Composable
internal fun <T> TileRows(items: List<T>, columns: Int, gap: Dp, tile: @Composable (T, Modifier) -> Unit) {
    items.chunked(columns).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            row.forEach { item -> tile(item, Modifier.weight(1f)) }
            if (row.size < columns) Spacer(Modifier.weight((columns - row.size).toFloat()))
        }
    }
}

/** Which lists Community shows under its sections: a chip each, where a second row of tabs used to be. */
internal enum class HomeChip { Popular, New, Following }

/** One chip's feed: what it shows and what it can be asked. */
internal class ChipFeed(val pane: FeedPane, val calls: FeedCallbacks, val cards: CardActions)

/**
 * Community's one grid (design audit 16.11, option B): the section tiles, the
 * way to all of them, the chips, and under them the chosen chip's lists in as
 * many columns as fit. Following brings what is followed, or the sections to
 * pick from when nothing is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeFeedGrid(
    chip: HomeChip,
    onChip: (HomeChip) -> Unit,
    explore: ExploreUiState,
    following: FollowingFeedUiState,
    feed: ChipFeed,
    layout: HomeLayout,
    gridState: LazyGridState,
    tiles: TileActions,
    followingActions: FollowingActions,
) {
    val pane = feed.pane
    val skeletonAlpha = rememberSkeletonAlpha(running = pane.phase == FeedPhase.Loading)
    val picking = chip == HomeChip.Following && following.followsNothing
    ScrolledDownWatcher(gridState, feed.calls.onScrolledDown)
    NearEndWatcher(gridState, pane.lists.size, feed.calls.onLoadMore)
    PullToRefreshBox(isRefreshing = pane.refreshing, onRefresh = feed.calls.onRefresh, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.feedColumns),
                state = gridState,
                modifier = Modifier
                    .then(if (layout == HomeLayout.Wide) Modifier.widthIn(max = WideContentMax) else Modifier)
                    .fillMaxSize()
                    .testTag(if (chip == HomeChip.Following) ExploreTestTags.FOLLOWING_FEED else ExploreTestTags.SCREEN),
                contentPadding = systemBarsContentPadding(
                    start = gridSide(layout),
                    end = gridSide(layout),
                    bottom = if (picking) PickBarRoom else 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(feedSpacing(layout)),
                verticalArrangement = Arrangement.spacedBy(feedSpacing(layout)),
            ) {
                item(key = "tiles", span = { GridItemSpan(maxLineSpan) }) {
                    TilesBlock(layout, explore.sections, tiles)
                }
                item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    HomeChips(chip, onChip, Modifier.padding(top = if (layout == HomeLayout.CompactHeight) 0.dp else 2.dp))
                }
                if (chip == HomeChip.Popular && pane.phase == FeedPhase.Ready) freshItems(explore.fresh, feed.cards)
                when (chip) {
                    HomeChip.Popular, HomeChip.New -> feedItems(pane, skeletonAlpha, feed.cards, feed.calls.onRetry, reasonFor = { null })
                    HomeChip.Following -> followingItems(following, skeletonAlpha, followingActions, feed.cards, feed.calls.onRetry)
                }
            }
            if (picking) {
                PickBar(following.picked.size, following.followingPicked, followingActions.onFollowPicked, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/** People's fresh lists, first in Popular and in its own grid, each with a New badge. */
private fun LazyGridScope.freshItems(lists: List<PublishedListSummary>, cards: CardActions) {
    lists.forEach { summary ->
        item(key = "fresh-${summary.id}", contentType = FeedContent.Card) {
            FeedCard(
                summary = summary,
                onClick = { cards.onOpen(summary) },
                onLongClick = { cards.onLongPress(summary) },
                badge = stringResource(R.string.feed_badge_new),
            )
        }
    }
}

/** Popular, New, Following: 32dp chips in a 48dp row, so each is still a whole finger's target. */
@Composable
private fun HomeChips(selected: HomeChip, onSelect: (HomeChip) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeChip.entries.forEach { chip ->
            FilterChip(
                selected = chip == selected,
                onClick = { onSelect(chip) },
                label = { Text(stringResource(chip.label)) },
                modifier = Modifier.testTag(ExploreTestTags.chip(chip)),
            )
        }
    }
}

private val HomeChip.label: Int
    get() = when (this) {
        HomeChip.Popular -> R.string.feed_sort_popular
        HomeChip.New -> R.string.feed_chip_new
        HomeChip.Following -> R.string.explore_tab_following
    }

private fun gridSide(layout: HomeLayout) = if (layout == HomeLayout.Wide) 24.dp else 16.dp

private fun feedSpacing(layout: HomeLayout) = if (layout == HomeLayout.Wide) 16.dp else FeedSpacing

/** The feed part of a grid: skeletons, cards (or the note where one was hidden), or the block that says why there are none. */
internal fun LazyGridScope.feedItems(
    pane: FeedPane,
    skeletonAlpha: () -> Float,
    cards: CardActions,
    onRetry: () -> Unit,
    reasonFor: @Composable (PublishedListSummary) -> String?,
) {
    when (pane.phase) {
        FeedPhase.Loading -> items(count = SKELETONS, key = { "skeleton-$it" }, contentType = { FeedContent.Skeleton }) {
            SkeletonCard(skeletonAlpha)
        }

        FeedPhase.Failed -> item(key = "failed", span = { GridItemSpan(maxLineSpan) }) {
            FeedFailedBlock(retrying = pane.retrying, onRetry = onRetry, unverified = pane.unverified)
        }

        FeedPhase.Ready -> {
            // Typed, so a card scrolled away is reused for the next card rather than for a heading.
            pane.lists.forEach { summary ->
                item(key = summary.id, contentType = FeedContent.Card) {
                    val hidden = pane.justHidden[summary.id]
                    if (hidden != null) {
                        HiddenCard(reported = hidden)
                    } else {
                        FeedCard(
                            summary = summary,
                            onClick = { cards.onOpen(summary) },
                            onLongClick = { cards.onLongPress(summary) },
                            reason = reasonFor(summary),
                        )
                    }
                }
            }
        }
    }
}

private const val SKELETONS = 6

private enum class FeedContent { Skeleton, Card }

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
