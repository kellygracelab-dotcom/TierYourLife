package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.SearchIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.BackArrowIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CardActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.EnterAlwaysColumn
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCallbacks
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowSize
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowToggle
import com.artiuillab.tieryourlife.feature.community.presentation.explore.HomeLayout
import com.artiuillab.tieryourlife.feature.community.presentation.explore.NearEndWatcher
import com.artiuillab.tieryourlife.feature.community.presentation.explore.NewListsPill
import com.artiuillab.tieryourlife.feature.community.presentation.explore.OfflineStrip
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScrolledDownWatcher
import com.artiuillab.tieryourlife.feature.community.presentation.explore.WideContentMax
import com.artiuillab.tieryourlife.feature.community.presentation.explore.edgeToEdge
import com.artiuillab.tieryourlife.feature.community.presentation.explore.feedItems
import com.artiuillab.tieryourlife.feature.community.presentation.explore.homeLayoutFor
import com.artiuillab.tieryourlife.feature.community.presentation.explore.rememberSkeletonAlpha
import kotlinx.coroutines.launch

/**
 * What stands over a page of lists, a section's or a topic's: its picture,
 * its name, the small word over the name, and Follow, where there is
 * anything to follow.
 */
internal class PageHead(
    val title: String,
    val following: Boolean,
    val onToggleFollow: (() -> Unit)?,
    val overline: String? = null,
    val backdrop: @Composable (Modifier) -> Unit,
    val onSearch: (() -> Unit)? = null,
)

/** A page's own lists, below its header and under their heading. */
internal class PageBody(
    val feed: FeedPane,
    val nowMillis: Long,
    val callbacks: FeedCallbacks,
    val onOpenList: (PublishedListSummary) -> Unit,
    val items: LazyGridScope.(layout: HomeLayout, skeletonAlpha: () -> Float) -> Unit,
)

/**
 * A section's page (§3) and a topic's alike, drawn for the window it is in:
 * a collapsing header, one short bar, or the cover's rows.
 */
@Composable
internal fun ListsPage(
    head: PageHead,
    body: PageBody,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    screenTag: String,
    search: PageSearch? = null,
    cards: CardActions = CardActions(body.onOpenList) {},
) {
    Surface(Modifier.fillMaxSize().testTag(screenTag), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = homeLayoutFor(currentWindowShape, maxHeight)
            when {
                search?.query != null -> PageSearchPane(search, layout, cards)
                else -> PageLayout(head, body, layout, onBack)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}

@Composable
private fun PageLayout(head: PageHead, body: PageBody, layout: HomeLayout, onBack: () -> Unit) {
            when (layout) {
                HomeLayout.Cover -> CoverSection(head.title, body.feed, onBack, body.onOpenList, body.callbacks.onRetry, body.callbacks.onLoadMore)
                HomeLayout.CompactHeight -> EnterAlwaysColumn(
                    // The inset stays with the screen while the bar leaves.
                    modifier = Modifier.statusBarsPadding(),
                    header = { CompactBar(head, onBack) },
                    body = { PageFeed(head, body, layout, headerShown = false) },
                )

                else -> PageFeed(head, body, layout, headerShown = true, onBack = onBack)
            }
}

/** The grid: the header where there is one, then the page's own items. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageFeed(head: PageHead, body: PageBody, layout: HomeLayout, headerShown: Boolean, onBack: () -> Unit = {}) {
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val feed = body.feed
    val alpha = rememberSkeletonAlpha(running = feed.phase == FeedPhase.Loading)
    ScrolledDownWatcher(grid, body.callbacks.onScrolledDown)
    NearEndWatcher(grid, feed.lists.size, body.callbacks.onLoadMore)
    Column(Modifier.fillMaxSize()) {
        OfflineStrip(feed.offlineSinceMillis, body.nowMillis, feed.retrying, body.callbacks.onRetry)
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(isRefreshing = feed.refreshing, onRefresh = body.callbacks.onRefresh, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(layout.feedColumns),
                        state = grid,
                        modifier = Modifier
                            .then(if (layout == HomeLayout.Wide) Modifier.widthIn(max = WideContentMax) else Modifier)
                            .fillMaxSize(),
                        contentPadding = systemBarsContentPadding(start = sideOf(layout), end = sideOf(layout), bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (headerShown) {
                            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                                PageHeader(head, Modifier.edgeToEdge(sideOf(layout)))
                            }
                        }
                        body.items(this, layout, alpha)
                    }
                }
            }
            if (headerShown) CollapsingBar(head, grid, onBack)
            NewListsPill(
                visible = feed.newListsWaiting,
                onClick = {
                    scope.launch { grid.animateScrollToItem(0) }
                    body.callbacks.onApplyHeld()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp),
            )
        }
    }
}

private fun sideOf(layout: HomeLayout) = if (layout == HomeLayout.Wide) 24.dp else 16.dp

/** The expanded header (§3.1): the page's picture under a scrim, with the name and Follow. */
@Composable
private fun PageHeader(head: PageHead, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .testTag(SectionTestTags.HEADER),
    ) {
        head.backdrop(Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.35f to Color.Black.copy(alpha = 0.05f),
                        1f to Color.Black.copy(alpha = 0.68f),
                    ),
                ),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                head.overline?.let { Text(it, style = OverlineStyle, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(head.title, style = SectionTitleStyle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            head.onToggleFollow?.let { FollowToggle(following = head.following, onClick = it) }
        }
    }
}

/**
 * Over the header: the back arrow and, once the header has scrolled away, the
 * 64dp bar with the name and a compact Follow, on the surface colour (§3.1).
 */
@Composable
private fun CollapsingBar(head: PageHead, grid: LazyGridState, onBack: () -> Unit) {
    val density = LocalDensity.current
    val travel = with(density) { (HEADER_HEIGHT - 64.dp).toPx() }
    val collapsed by remember(grid) {
        derivedStateOf {
            if (grid.firstVisibleItemIndex > 0) 1f else (grid.firstVisibleItemScrollOffset / travel).coerceIn(0f, 1f)
        }
    }
    val onImage = collapsed < 1f
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = collapsed))
            .statusBarsPadding()
            .height(64.dp)
            .padding(start = 4.dp, end = 12.dp)
            .testTag(SectionTestTags.BAR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            BackArrowIcon(24.dp, if (onImage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!onImage) {
            Text(
                head.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            head.onToggleFollow?.let { FollowToggle(following = head.following, onClick = it, size = FollowSize.Compact) }
        } else {
            Spacer(Modifier.weight(1f))
        }
        head.onSearch?.let { SearchButton(it, if (onImage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SearchButton(onClick: () -> Unit, color: Color) {
    val description = stringResource(R.string.section_search)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .semantics { contentDescription = description }
            .testTag(SectionTestTags.SEARCH_BUTTON),
    ) { SearchIcon(color) }
}

/** A short window's one bar: back, the name, and Follow. It scrolls away with the lists. */
@Composable
private fun CompactBar(head: PageHead, onBack: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 4.dp, end = 12.dp)
                .testTag(SectionTestTags.BAR),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { BackIcon() }
            Text(
                head.title,
                modifier = Modifier.weight(1f),
                style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            head.onToggleFollow?.let { FollowToggle(following = head.following, onClick = it, size = FollowSize.Compact) }
            head.onSearch?.let { SearchButton(it, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Lists under their heading and sort, or the page's word that there are
 * none yet: what a section shows under its topics and a topic shows alone.
 */
internal fun LazyGridScope.listsBlock(
    feed: FeedPane,
    layout: HomeLayout,
    alpha: () -> Float,
    cards: CardActions,
    onRetry: () -> Unit,
    sortRow: @Composable () -> Unit,
) {
    item(key = "sort", span = { GridItemSpan(maxLineSpan) }) { sortRow() }
    if (feed.phase == FeedPhase.Ready && feed.lists.isEmpty()) {
        item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { SectionEmpty() }
    } else {
        feedItems(feed, layout, alpha, cards, onRetry, reasonFor = { null })
    }
}

private val HEADER_HEIGHT = 200.dp
private val OverlineStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
