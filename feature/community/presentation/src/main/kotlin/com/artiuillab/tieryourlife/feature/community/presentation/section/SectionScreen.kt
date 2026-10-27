package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.BackArrowIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CardActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.EnterAlwaysColumn
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCallbacks
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedListMenus
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
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** What a section's page can be asked to do beyond its feed. */
class SectionCallbacks(
    val onSelectTopic: (Topic?) -> Unit = {},
    val onSelectSort: (FeedSort) -> Unit = {},
    val onToggleSectionFollow: () -> Unit = {},
    val onToggleTopicFollow: () -> Unit = {},
)

@Composable
fun SectionScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    viewModel: SectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open() }
    SectionScreenContent(
        state = state,
        onBack = onBack,
        feed = FeedCallbacks(
            onRefresh = { viewModel.feed.pullToRefresh() },
            onRetry = { viewModel.feed.retry() },
            onLoadMore = { viewModel.feed.loadMore() },
            onScrolledDown = { viewModel.feed.onScrolledDown(it) },
            onApplyHeld = { viewModel.feed.applyHeld() },
            onHideList = viewModel::hideList,
            onHideAuthor = viewModel::hideAuthor,
            onReport = viewModel::report,
        ),
        calls = SectionCallbacks(
            onSelectTopic = viewModel::selectTopic,
            onSelectSort = viewModel::selectSort,
            onToggleSectionFollow = viewModel::toggleSectionFollow,
            onToggleTopicFollow = viewModel::toggleTopicFollow,
        ),
        onOpenList = { onOpenList(CommunityRoute.CommunityList.of(it)) },
        onOpenAuthor = onAuthorClick,
        notices = viewModel.notices,
    )
}

/** A section's page (§3), drawn for the window it is in: a collapsing header, one short bar, or the cover's rows. */
@Composable
fun SectionScreenContent(
    state: SectionUiState,
    onBack: () -> Unit,
    feed: FeedCallbacks = FeedCallbacks(),
    calls: SectionCallbacks = SectionCallbacks(),
    onOpenList: (PublishedListSummary) -> Unit = {},
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    notices: Flow<CommunityNotice> = emptyFlow(),
) {
    val snackbar = remember { SnackbarHostState() }
    var menuFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var waitingTopic by remember { mutableStateOf<Topic?>(null) }
    SectionNotices(notices, snackbar, feed.onRefresh, calls.onToggleSectionFollow)
    BackHandler(enabled = state.selectedTopic != null) { calls.onSelectTopic(null) }

    Surface(Modifier.fillMaxSize().testTag(SectionTestTags.SCREEN), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = homeLayoutFor(currentWindowShape, maxHeight)
            val cards = CardActions(onOpenList) { menuFor = it }
            when (layout) {
                HomeLayout.Cover -> CoverSection(state, onBack, onOpenList, feed.onRetry, feed.onLoadMore)
                HomeLayout.CompactHeight -> EnterAlwaysColumn(
                    // The inset stays with the screen while the bar leaves.
                    modifier = Modifier.statusBarsPadding(),
                    header = { CompactSectionBar(state, onBack, calls.onToggleSectionFollow) },
                    body = { SectionFeed(state, layout, feed, calls, cards, headerShown = false, onOpenWaiting = { waitingTopic = it }) },
                )

                else -> SectionFeed(state, layout, feed, calls, cards, headerShown = true, onOpenWaiting = { waitingTopic = it }, onBack = onBack)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    FeedListMenus(menuFor, { menuFor = null }, feed, onOpenAuthor)
    waitingTopic?.let { WaitingTopicSheet(it) { waitingTopic = null } }
}

@Composable
private fun SectionNotices(
    notices: Flow<CommunityNotice>,
    snackbar: SnackbarHostState,
    onRefresh: () -> Unit,
    onFollowAgain: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(notices) {
        notices.collect { notice ->
            when (notice) {
                is CommunityNotice.RefreshFailed -> {
                    val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(notice.showingFromMillis))
                    val result = snackbar.showSnackbar(resources.getString(R.string.explore_refresh_failed, time), resources.getString(R.string.explore_retry))
                    if (result == SnackbarResult.ActionPerformed) onRefresh()
                }

                is CommunityNotice.Unfollowed -> {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.explore_unfollowed, resources.getString(notice.section.labelRes)),
                        actionLabel = resources.getString(R.string.explore_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onFollowAgain()
                }

                CommunityNotice.FollowFailed -> snackbar.showSnackbar(resources.getString(R.string.explore_follow_failed))
                CommunityNotice.ActionFailed -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_action_failed))
                is CommunityNotice.NowFollowing -> Unit
            }
        }
    }
}

/** The grid: header (where there is one), chips, the topic row, the sort row, the lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionFeed(
    state: SectionUiState,
    layout: HomeLayout,
    feed: FeedCallbacks,
    calls: SectionCallbacks,
    cards: CardActions,
    headerShown: Boolean,
    onOpenWaiting: (Topic) -> Unit,
    onBack: () -> Unit = {},
) {
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val alpha = rememberSkeletonAlpha()
    ScrolledDownWatcher(grid, feed.onScrolledDown)
    NearEndWatcher(grid, state.feed.lists.size, feed.onLoadMore)
    // A filtered page is reached by scrolling: it opens with the header folded away.
    LaunchedEffect(state.selectedTopic?.id) {
        if (headerShown && state.selectedTopic != null && grid.firstVisibleItemIndex == 0) grid.scrollToItem(1)
    }
    Column(Modifier.fillMaxSize()) {
        OfflineStrip(state.feed.offlineSinceMillis, state.nowMillis, state.feed.retrying, feed.onRetry)
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(isRefreshing = state.feed.refreshing, onRefresh = feed.onRefresh, modifier = Modifier.fillMaxSize()) {
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
                                SectionHeader(state.section, state.stats, calls.onToggleSectionFollow, Modifier.edgeToEdge(sideOf(layout)))
                            }
                        }
                        sectionControls(state, calls, onOpenWaiting, sideOf(layout))
                        feedItems(state.feed, layout, alpha, cards, feed.onRetry, reasonFor = { null })
                    }
                }
            }
            if (headerShown) CollapsingBar(state, grid, onBack, calls.onToggleSectionFollow)
            NewListsPill(
                visible = state.feed.newListsWaiting,
                onClick = {
                    scope.launch { grid.animateScrollToItem(0) }
                    feed.onApplyHeld()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp),
            )
        }
    }
}

private fun sideOf(layout: HomeLayout) = if (layout == HomeLayout.Wide) 24.dp else 16.dp

private fun LazyGridScope.sectionControls(state: SectionUiState, calls: SectionCallbacks, onOpenWaiting: (Topic) -> Unit, side: Dp) {
    item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
        Box {
            TopicChipsRow(
                topics = state.topics,
                waiting = state.waiting,
                selected = state.selectedTopic,
                onSelect = calls.onSelectTopic,
                onOpenWaiting = onOpenWaiting,
                onSuggest = null,
                side = side,
            )
        }
    }
    val topic = state.selectedTopic
    if (topic != null) {
        item(key = "topic-${topic.id}", span = { GridItemSpan(maxLineSpan) }) {
            TopicRow(topic, state.section, calls.onToggleTopicFollow)
        }
    }
    item(key = "sort", span = { GridItemSpan(maxLineSpan) }) {
        Box {
            SortRow(listCount = topic?.listCount ?: state.stats?.listCount, sort = state.sort, onSort = calls.onSelectSort)
        }
    }
}

/**
 * Over the header: the back arrow and, once the header has scrolled away, the
 * 64dp bar with the name and a compact Follow, on the surface colour (§3.1).
 */
@Composable
private fun CollapsingBar(state: SectionUiState, grid: LazyGridState, onBack: () -> Unit, onToggleFollow: () -> Unit) {
    val density = LocalDensity.current
    val travel = with(density) { (200.dp - 64.dp).toPx() }
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
                stringResource(state.section.labelRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FollowToggle(following = state.stats?.following == true, onClick = onToggleFollow, size = FollowSize.Compact)
        }
    }
}

/** A short window's one bar for a section: back, the name, the counts, and Follow. It scrolls away with the lists. */
@Composable
private fun CompactSectionBar(state: SectionUiState, onBack: () -> Unit, onToggleFollow: () -> Unit) {
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
                stringResource(state.section.labelRes),
                style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            sectionCounts(state.stats)?.let {
                Text(
                    "  ·  $it",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } ?: Spacer(Modifier.weight(1f))
            FollowToggle(following = state.stats?.following == true, onClick = onToggleFollow, size = FollowSize.Compact)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
