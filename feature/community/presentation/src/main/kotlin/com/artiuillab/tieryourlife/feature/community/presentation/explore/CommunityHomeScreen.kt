package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.ui.HomeTopBar
import com.artiuillab.tieryourlife.core.theme.ui.OnResumeEffect
import com.artiuillab.tieryourlife.core.theme.ui.SearchIcon
import com.artiuillab.tieryourlife.core.theme.ui.SearchTopBar
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingActions
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedViewModel
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingTab
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.community.presentation.section.WaitingTopicSheet
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** What one feed tab can be asked to do. */
class FeedCallbacks(
    val onOpen: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onScrolledDown: (Boolean) -> Unit = {},
    val onApplyHeld: () -> Unit = {},
    val onHideList: (PublishedListSummary) -> Unit = {},
    val onHideAuthor: (uid: String, name: String) -> Unit = { _, _ -> },
    val onReport: (PublishedListSummary, ReportReason, String?) -> Unit = { _, _, _ -> },
)

/** Explore's own requests beyond its feed. */
class ExploreCallbacks(
    val onFollowSection: (ListCategory) -> Unit = {},
    val onUnfollowSection: (ListCategory) -> Unit = {},
    val onEnterSearch: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onExitSearch: () -> Unit = {},
    val onQuickFollow: (ListCategory) -> Unit = {},
)

/** Where Community leads. */
class CommunityDestinations(
    val onOpenList: (PublishedListSummary) -> Unit = {},
    val onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    /** Null where the rail carries Settings. */
    val onSettings: (() -> Unit)? = null,
    val onManageFollowing: () -> Unit = {},
    val onOpenSection: (ListCategory, topicId: String?) -> Unit = { _, _ -> },
)

/**
 * The Community half of the home screen (docs/design-spec-community-explore.md):
 * Explore with the nine sections and the popular feed, Following with what
 * this person follows. [tabs] is the app's own Your lists / Community row,
 * drawn under the bar on a phone and absent where the rail does that job.
 */
@Composable
fun CommunityHomeScreen(
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onSettingsClick: () -> Unit,
    onManageFollowing: () -> Unit,
    onOpenSection: (CommunityRoute.Section) -> Unit,
    tabs: @Composable () -> Unit = {},
    explore: ExploreViewModel = hiltViewModel(),
    following: FollowingFeedViewModel = hiltViewModel(),
) {
    val exploreState by explore.state.collectAsStateWithLifecycle()
    val followingState by following.state.collectAsStateWithLifecycle()
    OnResumeEffect {
        explore.refreshHidden()
        following.refreshHidden()
    }
    val hasRail = currentWindowShape.hasRail
    CommunityHomeContent(
        explore = exploreState,
        following = followingState,
        exploreFeed = FeedCallbacks(
            onOpen = explore::open,
            onRefresh = explore.feed::pullToRefresh,
            onRetry = explore.feed::retry,
            onLoadMore = explore.feed::loadMore,
            onScrolledDown = explore.feed::onScrolledDown,
            onApplyHeld = explore.feed::applyHeld,
            onHideList = explore::hideList,
            onHideAuthor = explore::hideAuthor,
            onReport = explore::report,
        ),
        followingFeed = FeedCallbacks(
            onOpen = following::open,
            onRefresh = following.feed::pullToRefresh,
            onRetry = following.feed::retry,
            onLoadMore = following.feed::loadMore,
            onScrolledDown = following.feed::onScrolledDown,
            onApplyHeld = following.feed::applyHeld,
            onHideList = following::hideList,
            onHideAuthor = following::hideAuthor,
            onReport = following::report,
        ),
        exploreCalls = ExploreCallbacks(
            onFollowSection = explore::follow,
            onUnfollowSection = explore::unfollow,
            onEnterSearch = explore::enterSearch,
            onQueryChange = explore::updateQuery,
            onExitSearch = explore::exitSearch,
            onQuickFollow = following::quickFollow,
        ),
        destinations = CommunityDestinations(
            onOpenList = { onOpenList(CommunityRoute.CommunityList.of(it)) },
            onOpenAuthor = onAuthorClick,
            onSettings = onSettingsClick.takeUnless { hasRail },
            onManageFollowing = onManageFollowing,
            onOpenSection = { section, topicId -> onOpenSection(CommunityRoute.Section(section.id, topicId)) },
        ),
        tabs = tabs,
        notices = remember(explore, following) { merge(explore.notices, following.notices) },
        onUndoUnfollow = explore::follow,
    )
}

/** Stateless, like the app's other screens: drawable from a test or a preview without a view model. */
@Composable
fun CommunityHomeContent(
    explore: ExploreUiState,
    following: FollowingFeedUiState,
    exploreFeed: FeedCallbacks = FeedCallbacks(),
    followingFeed: FeedCallbacks = FeedCallbacks(),
    exploreCalls: ExploreCallbacks = ExploreCallbacks(),
    destinations: CommunityDestinations = CommunityDestinations(),
    tabs: @Composable () -> Unit = {},
    notices: Flow<CommunityNotice> = emptyFlow(),
    onUndoUnfollow: (ListCategory) -> Unit = {},
    startOnFollowing: Boolean = false,
) {
    val snackbar = remember { SnackbarHostState() }
    NoticeSnackbars(notices, snackbar, exploreFeed, onUndoUnfollow)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = homeLayoutFor(currentWindowShape, maxHeight)
            if (layout == HomeLayout.Cover) {
                CoverHome(explore, onOpenSection = { destinations.onOpenSection(it, null) })
            } else {
                HomeBody(explore, following, layout, exploreFeed, followingFeed, exploreCalls, destinations, tabs, startOnFollowing)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun NoticeSnackbars(
    notices: Flow<CommunityNotice>,
    snackbar: SnackbarHostState,
    exploreFeed: FeedCallbacks,
    onUndoUnfollow: (ListCategory) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(notices) {
        notices.collect { notice ->
            when (notice) {
                is CommunityNotice.RefreshFailed -> {
                    val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(notice.showingFromMillis))
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.explore_refresh_failed, time),
                        actionLabel = context.getString(R.string.explore_retry),
                    )
                    if (result == SnackbarResult.ActionPerformed) exploreFeed.onRefresh()
                }

                CommunityNotice.FollowFailed -> snackbar.showSnackbar(context.getString(R.string.explore_follow_failed))
                is CommunityNotice.NowFollowing -> snackbar.showSnackbar(
                    context.getString(R.string.explore_now_following, context.getString(notice.section.labelRes)),
                )

                is CommunityNotice.Unfollowed -> {
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.explore_unfollowed, context.getString(notice.section.labelRes)),
                        actionLabel = context.getString(R.string.explore_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onUndoUnfollow(notice.section)
                }

                CommunityNotice.ActionFailed -> snackbar.showSnackbar(context.getString(ThemeR.string.snack_action_failed))
            }
        }
    }
}

@Composable
private fun HomeBody(
    explore: ExploreUiState,
    following: FollowingFeedUiState,
    layout: HomeLayout,
    exploreFeed: FeedCallbacks,
    followingFeed: FeedCallbacks,
    exploreCalls: ExploreCallbacks,
    destinations: CommunityDestinations,
    tabs: @Composable () -> Unit,
    startOnFollowing: Boolean,
) {
    val pager = rememberPagerState(initialPage = if (startOnFollowing) 1 else 0) { 2 }
    val scope = rememberCoroutineScope()
    val exploreGrid = rememberLazyGridState()
    val followingGrid = rememberLazyGridState()
    var exploreMenu by remember { mutableStateOf<PublishedListSummary?>(null) }
    var followingMenu by remember { mutableStateOf<PublishedListSummary?>(null) }
    var waitingTopic by rememberSaveable(stateSaver = TopicSaver) { mutableStateOf<Topic?>(null) }
    val searching = explore.query != null

    LaunchedEffect(Unit) { exploreFeed.onOpen() }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage == 1) followingFeed.onOpen() }
    BackHandler(enabled = searching, onBack = exploreCalls.onExitSearch)

    val currentPane = if (pager.currentPage == 0) explore.feed else following.feed
    val currentFeed = if (pager.currentPage == 0) exploreFeed else followingFeed
    val selectTab: (Int) -> Unit = { page -> scope.launch { pager.animateScrollToPage(page) } }

    if (searching) {
        Column(Modifier.fillMaxSize()) {
            SearchTopBar(query = explore.query.orEmpty(), onQueryChange = exploreCalls.onQueryChange, onClose = exploreCalls.onExitSearch)
            SearchResultsGrid(explore.results, layout, CardActions(destinations.onOpenList) { exploreMenu = it })
        }
    } else {
        EnterAlwaysColumn(
            header = {
                when (layout) {
                    HomeLayout.Portrait -> HomeTopBar(onSearchClick = exploreCalls.onEnterSearch, onSettingsClick = destinations.onSettings)
                    HomeLayout.CompactHeight -> CompactBar(pager.currentPage, selectTab, exploreCalls.onEnterSearch)
                    else -> RailBar(exploreCalls.onEnterSearch)
                }
            },
            body = {
                Column(Modifier.fillMaxSize()) {
                    if (layout == HomeLayout.Portrait) tabs()
                    if (layout != HomeLayout.CompactHeight) SubTabs(pager.currentPage, selectTab, inset = layout != HomeLayout.Portrait)
                    OfflineStrip(currentPane.offlineSinceMillis, if (pager.currentPage == 0) explore.nowMillis else following.nowMillis, currentPane.retrying, currentFeed.onRetry)
                    Box(Modifier.fillMaxSize()) {
                        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
                            if (page == 0) {
                                ExploreTab(
                                    state = explore,
                                    layout = layout,
                                    gridState = exploreGrid,
                                    tiles = TileActions(
                                        onOpen = { destinations.onOpenSection(it, null) },
                                        onFollow = exploreCalls.onFollowSection,
                                        onUnfollow = exploreCalls.onUnfollowSection,
                                    ),
                                    cards = CardActions(destinations.onOpenList) { exploreMenu = it },
                                    onRefresh = exploreFeed.onRefresh,
                                    onRetry = exploreFeed.onRetry,
                                    onLoadMore = exploreFeed.onLoadMore,
                                    onScrolledDown = exploreFeed.onScrolledDown,
                                )
                            } else {
                                FollowingTab(
                                    state = following,
                                    layout = layout,
                                    gridState = followingGrid,
                                    actions = FollowingActions(
                                        onOpenSection = { destinations.onOpenSection(it, null) },
                                        onOpenTopic = { topic -> topic.section?.let { destinations.onOpenSection(it, topic.id) } },
                                        onOpenWaiting = { waitingTopic = it },
                                        onOpenAuthor = { destinations.onOpenAuthor(it.uid, it.name, it.photoUrl) },
                                        onManage = destinations.onManageFollowing,
                                        onQuickFollow = exploreCalls.onQuickFollow,
                                        onGoToExplore = { selectTab(0) },
                                    ),
                                    cards = CardActions(destinations.onOpenList) { followingMenu = it },
                                    onRefresh = followingFeed.onRefresh,
                                    onRetry = followingFeed.onRetry,
                                    onLoadMore = followingFeed.onLoadMore,
                                    onScrolledDown = followingFeed.onScrolledDown,
                                )
                            }
                        }
                        NewListsPill(
                            visible = currentPane.newListsWaiting,
                            onClick = {
                                val grid = if (pager.currentPage == 0) exploreGrid else followingGrid
                                scope.launch { grid.animateScrollToItem(0) }
                                currentFeed.onApplyHeld()
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                        )
                    }
                }
            },
        )
    }

    FeedListMenus(exploreMenu, { exploreMenu = null }, exploreFeed, destinations.onOpenAuthor)
    FeedListMenus(followingMenu, { followingMenu = null }, followingFeed, destinations.onOpenAuthor)
    waitingTopic?.let { topic -> WaitingTopicSheet(topic = topic, onDismiss = { waitingTopic = null }) }
}

/** Explore · Following (§1.2): two equal tabs under the bar, or hugging their labels beside a rail's title. */
@Composable
private fun SubTabs(selected: Int, onSelect: (Int) -> Unit, inset: Boolean) {
    SecondaryTabRow(
        selectedTabIndex = selected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (inset) 12.dp else 0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
    ) {
        SubTab(stringResource(R.string.explore_tab_explore), selected == 0, ExploreTestTags.TAB_EXPLORE) { onSelect(0) }
        SubTab(stringResource(R.string.explore_tab_following), selected == 1, ExploreTestTags.TAB_FOLLOWING) { onSelect(1) }
    }
}

@Composable
private fun SubTab(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .testTag(tag),
        text = { Text(label, style = MaterialTheme.typography.titleSmall) },
        selectedContentColor = MaterialTheme.colorScheme.onSurface,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A short window's one bar (§6.1): the name, the two tabs inline, and search. It scrolls away with the content. */
@Composable
private fun CompactBar(selected: Int, onSelect: (Int) -> Unit, onSearch: () -> Unit) {
    Column(Modifier.statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.explore_title_community),
                style = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(12.dp))
            InlineTab(stringResource(R.string.explore_tab_explore), selected == 0, ExploreTestTags.TAB_EXPLORE) { onSelect(0) }
            InlineTab(stringResource(R.string.explore_tab_following), selected == 1, ExploreTestTags.TAB_FOLLOWING) { onSelect(1) }
            Spacer(Modifier.weight(1f))
            SearchButton(onSearch)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun InlineTab(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Column(
        Modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

/** A rail's bar (§7.1): the section of the app it is, and search; Settings lives in the rail. */
@Composable
private fun RailBar(onSearch: () -> Unit) {
    Row(
        Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 24.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.explore_title_community),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SearchButton(onSearch)
    }
}

@Composable
private fun SearchButton(onSearch: () -> Unit) {
    IconButton(onClick = onSearch, modifier = Modifier.testTag(ExploreTestTags.SEARCH)) { SearchIcon() }
}

/** What was typed, answered by the server: cards, or a line saying there is nothing. */
@Composable
private fun SearchResultsGrid(results: SearchResults, layout: HomeLayout, cards: CardActions) {
    val alpha = rememberSkeletonAlpha()
    LazyVerticalGrid(
        columns = GridCells.Fixed(layout.feedColumns.coerceAtLeast(2)),
        modifier = Modifier
            .fillMaxSize()
            .testTag(ExploreTestTags.SEARCH_RESULTS),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            results.loading -> items(4) { SkeletonCard(alpha, coverHeight = layout.coverHeight) }
            results.failed -> item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                FeedFailedBlock(retrying = false, onRetry = {})
            }

            else -> items(results.lists, key = { it.id }) { summary ->
                FeedCard(
                    summary = summary,
                    onClick = { cards.onOpen(summary) },
                    onLongClick = { cards.onLongPress(summary) },
                    coverHeight = layout.coverHeight,
                )
            }
        }
    }
}

/** The Flip's cover (§8.1): the nine sections and nothing else; a list waits for the phone to open. */
@Composable
private fun CoverHome(explore: ExploreUiState, onOpenSection: (ListCategory) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .testTag(ExploreTestTags.COVER_HOME),
    ) {
        Text(
            stringResource(R.string.explore_title_community),
            modifier = Modifier
                .height(36.dp)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        TilesBlock(
            layout = HomeLayout.Cover,
            sections = explore.sections,
            countsKnown = false,
            actions = TileActions(onOpen = onOpenSection),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/** A waiting topic survives rotation by what the sheet shows: its id and name. */
private val TopicSaver = androidx.compose.runtime.saveable.listSaver<Topic?, String>(
    save = { topic -> if (topic == null) emptyList() else listOf(topic.id, topic.name) },
    restore = { saved ->
        if (saved.size < 2) null else Topic(id = saved[0], name = saved[1], section = null, status = com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus.Pending)
    },
)
