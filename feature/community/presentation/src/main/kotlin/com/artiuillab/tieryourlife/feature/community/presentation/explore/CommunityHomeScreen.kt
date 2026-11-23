package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.ui.HomeIconButton
import com.artiuillab.tieryourlife.core.theme.ui.HomeTopBar
import com.artiuillab.tieryourlife.core.theme.ui.OnResumeEffect
import com.artiuillab.tieryourlife.core.theme.ui.SearchIcon
import com.artiuillab.tieryourlife.core.theme.ui.SearchTopBar
import com.artiuillab.tieryourlife.core.theme.ui.SectionLabel
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.matchesSearch
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.LocalOwnFace
import com.artiuillab.tieryourlife.feature.community.presentation.components.OwnFaceViewModel
import com.artiuillab.tieryourlife.feature.community.presentation.follow.SignInToFollow
import com.artiuillab.tieryourlife.feature.community.presentation.follow.followingSentence
import com.artiuillab.tieryourlife.feature.community.presentation.follow.sentenceFor
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingActions
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedUiState
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.FollowingFeedViewModel
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.community.presentation.section.FoundTopicTiles
import com.artiuillab.tieryourlife.feature.community.presentation.section.WaitingSectionSheet
import com.artiuillab.tieryourlife.feature.community.presentation.section.WaitingTopicSheet
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSectionNames
import com.artiuillab.tieryourlife.feature.tier.board.components.ArtPictures
import com.artiuillab.tieryourlife.feature.tier.board.components.PrefetchArt
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

/** Explore's own requests beyond its feed; sections go by id. */
class ExploreCallbacks(
    val onFollowSection: (String) -> Unit = {},
    val onUnfollowSection: (String) -> Unit = {},
    val onEnterSearch: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onExitSearch: () -> Unit = {},
    val onRetrySearch: () -> Unit = {},
    val onLoadMoreSearch: () -> Unit = {},
    val onTogglePick: (String) -> Unit = {},
    val onFollowPicked: () -> Unit = {},
    /** Withdraws a waiting section proposal, or dismisses a notice. */
    val onWithdrawProposal: (String) -> Unit = {},
)

/** Where Community leads. */
class CommunityDestinations(
    val onOpenList: (PublishedListSummary) -> Unit = {},
    val onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    /** Null where the rail carries Settings. */
    val onSettings: (() -> Unit)? = null,
    val onManageFollowing: () -> Unit = {},
    val onOpenSection: (sectionId: String) -> Unit = {},
    val onOpenTopic: (Topic) -> Unit = {},
    val onOpenAllSections: () -> Unit = {},
)

/**
 * The Community half of the home screen (docs/design-spec-community-explore.md):
 * Explore with the sections and the popular feed, Following with what
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
    onOpenTopic: (CommunityRoute.Topic) -> Unit = {},
    tabs: @Composable () -> Unit = {},
    onOpenAllSections: () -> Unit = {},
    onSignIn: () -> Unit = {},
    explore: ExploreViewModel = hiltViewModel(),
    following: FollowingFeedViewModel = hiltViewModel(),
    ownFace: OwnFaceViewModel = hiltViewModel(),
) {
    val exploreState by explore.state.collectAsStateWithLifecycle()
    SignInToFollow(explore.signInRequests, onSignIn)
    val followingState by following.state.collectAsStateWithLifecycle()
    val face by ownFace.face.collectAsStateWithLifecycle()
    OnResumeEffect {
        explore.refreshHidden()
        following.refreshHidden()
    }
    val hasRail = currentWindowShape.hasRail
    val languageTag = currentLanguageTag()
    CompositionLocalProvider(LocalOwnFace provides face) {
        CommunityHomeContent(
            explore = exploreState,
            following = followingState,
            exploreFeed = FeedCallbacks(
                onOpen = explore::open,
                onRefresh = explore::refreshPopular,
                onRetry = explore.feed::retry,
                onLoadMore = explore.feed::loadMore,
                onScrolledDown = explore.feed::onScrolledDown,
                onApplyHeld = explore.feed::applyHeld,
                onHideList = { explore.hideList(it) },
                onHideAuthor = explore::hideAuthor,
                onReport = { summary, reason, note -> explore.report(summary, reason, note) },
            ),
            newestFeed = FeedCallbacks(
                onOpen = explore::openNewest,
                onRefresh = explore.newest::pullToRefresh,
                onRetry = explore.newest::retry,
                onLoadMore = explore.newest::loadMore,
                onScrolledDown = explore.newest::onScrolledDown,
                onApplyHeld = explore.newest::applyHeld,
                onHideList = { explore.hideList(it, fromNewest = true) },
                onHideAuthor = explore::hideAuthor,
                onReport = { summary, reason, note -> explore.report(summary, reason, note, fromNewest = true) },
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
                onEnterSearch = explore.search::enter,
                onQueryChange = explore.search::update,
                onExitSearch = explore.search::exit,
                onRetrySearch = explore.search::retry,
                onLoadMoreSearch = explore.search::loadMore,
                onTogglePick = following::togglePick,
                onFollowPicked = following::followPicked,
                onWithdrawProposal = following::withdrawProposal,
            ),
            destinations = CommunityDestinations(
                onOpenList = { onOpenList(CommunityRoute.CommunityList.of(it, languageTag)) },
                onOpenAuthor = onAuthorClick,
                onSettings = onSettingsClick.takeUnless { hasRail },
                onManageFollowing = onManageFollowing,
                onOpenSection = { onOpenSection(CommunityRoute.Section(it)) },
                onOpenTopic = { onOpenTopic(CommunityRoute.Topic.of(it)) },
                onOpenAllSections = onOpenAllSections,
            ),
            tabs = tabs,
            notices = remember(explore, following) { merge(explore.notices, following.notices) },
            onUndoUnfollow = explore::follow,
            picturesOnWifiOnly = explore.picturesOnWifiOnly,
        )
    }
}

/** Stateless, like the app's other screens: drawable from a test or a preview without a view model. */
@Composable
fun CommunityHomeContent(
    explore: ExploreUiState,
    following: FollowingFeedUiState,
    exploreFeed: FeedCallbacks = FeedCallbacks(),
    followingFeed: FeedCallbacks = FeedCallbacks(),
    exploreCalls: ExploreCallbacks = ExploreCallbacks(),
    /** The New chip's feed. */
    newestFeed: FeedCallbacks = FeedCallbacks(),
    destinations: CommunityDestinations = CommunityDestinations(),
    tabs: @Composable () -> Unit = {},
    notices: Flow<CommunityNotice> = emptyFlow(),
    onUndoUnfollow: (String) -> Unit = {},
    /** Opens on the Following chip rather than Popular. */
    startOnFollowing: Boolean = false,
    /** Null draws without fetching any covers ahead, as a test or a preview should. */
    picturesOnWifiOnly: (() -> Boolean)? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    val feeds = HomeFeeds(exploreFeed, newestFeed, followingFeed)
    NoticeSnackbars(notices, snackbar, exploreFeed, onUndoUnfollow)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = homeLayoutFor(currentWindowShape, maxHeight)
            if (layout == HomeLayout.Cover) {
                CoverCommunityGrid(onBack = null, sections = explore.sections, onOpenSection = destinations.onOpenSection)
            } else {
                HomeBody(explore, following, layout, feeds, exploreCalls, destinations, tabs, startOnFollowing)
                if (picturesOnWifiOnly != null) {
                    PrefetchCovers(explore.feed, following.feed, constraints.maxWidth / layout.feedColumns, picturesOnWifiOnly)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}

/**
 * The first cards of both feeds, the one on screen and the one a tab away.
 * A column's whole share of the window is never narrower than its card, and
 * Coil reuses a picture decoded larger.
 */
@Composable
private fun PrefetchCovers(explore: FeedPane, following: FeedPane, cardWidth: Int, picturesOnWifiOnly: () -> Boolean) {
    val coverHeight = (cardWidth / LIST_ART_ASPECT).toInt()
    val arts = remember(explore, following) {
        (explore.kept() + following.kept()).map { ArtPictures(it.coverImageUrl, it.previewImages, it.coverFocus, it.previewFocus, it.collageUrl) }
    }
    PrefetchArt(arts, cardWidth, coverHeight, picturesOnWifiOnly)
}

private fun FeedPane.kept() =
    if (phase == FeedPhase.Ready) lists.filterNot { it.id in justHidden }.take(PREFETCHED_CARDS) else emptyList()

private const val PREFETCHED_CARDS = 6

@Composable
private fun NoticeSnackbars(
    notices: Flow<CommunityNotice>,
    snackbar: SnackbarHostState,
    exploreFeed: FeedCallbacks,
    onUndoUnfollow: (String) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val nameOf = rememberSectionNames()
    LaunchedEffect(notices) {
        notices.collect { notice ->
            when (notice) {
                is CommunityNotice.RefreshFailed -> {
                    val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(notice.showingFromMillis))
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.explore_refresh_failed, time),
                        actionLabel = resources.getString(R.string.explore_retry),
                    )
                    if (result == SnackbarResult.ActionPerformed) exploreFeed.onRefresh()
                }

                is CommunityNotice.FollowFailed -> snackbar.showSnackbar(resources.sentenceFor(notice.refusal))
                is CommunityNotice.NowFollowing -> snackbar.showSnackbar(resources.followingSentence(notice, nameOf(notice.sectionId)))

                is CommunityNotice.Unfollowed -> {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.explore_unfollowed, nameOf(notice.sectionId)),
                        actionLabel = resources.getString(R.string.explore_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onUndoUnfollow(notice.sectionId)
                }

                CommunityNotice.ActionFailed -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_action_failed))
                CommunityNotice.TargetIsStaff -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_target_is_staff))
                CommunityNotice.ReportsToday -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_reports_today))
                is CommunityNotice.AlreadyDecided -> snackbar.showSnackbar(resources.getString(notice.words, notice.name))
            }
        }
    }
}

/** The three feeds under Community's chips. */
private class HomeFeeds(val popular: FeedCallbacks, val newest: FeedCallbacks, val following: FeedCallbacks) {
    operator fun get(chip: HomeChip): FeedCallbacks = when (chip) {
        HomeChip.Popular -> popular
        HomeChip.New -> newest
        HomeChip.Following -> following
    }
}

@Composable
private fun HomeBody(
    explore: ExploreUiState,
    following: FollowingFeedUiState,
    layout: HomeLayout,
    feeds: HomeFeeds,
    exploreCalls: ExploreCallbacks,
    destinations: CommunityDestinations,
    tabs: @Composable () -> Unit,
    startOnFollowing: Boolean,
) {
    var chip by rememberSaveable { mutableStateOf(if (startOnFollowing) HomeChip.Following else HomeChip.Popular) }
    val scope = rememberCoroutineScope()
    // Each chip keeps its own place, so going back to one finds it where it was left.
    val grids = HomeChip.entries.associateWith { rememberLazyGridState() }
    var menu by remember { mutableStateOf<Pair<HomeChip, PublishedListSummary>?>(null) }
    var waitingTopic by rememberSaveable(stateSaver = TopicSaver) { mutableStateOf<Topic?>(null) }
    var waitingSection by rememberSaveable { mutableStateOf<String?>(null) }
    val searching = explore.query != null

    // Following is asked for at once, so its chip finds its answer waiting.
    LaunchedEffect(Unit) {
        feeds.popular.onOpen()
        feeds.following.onOpen()
    }
    // Every choice of a chip but the first is an arrival of its own, as a tab's was.
    var chosen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(chip) {
        if (chip == HomeChip.New || chosen) feeds[chip].onOpen()
        chosen = true
    }
    BackHandler(enabled = searching, onBack = exploreCalls.onExitSearch)

    val currentPane = when (chip) {
        HomeChip.Popular -> explore.feed
        HomeChip.New -> explore.newest
        HomeChip.Following -> following.feed
    }
    val currentFeed = feeds[chip]

    if (searching) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            SearchTopBar(
                query = explore.query.orEmpty(),
                onQueryChange = exploreCalls.onQueryChange,
                onClose = exploreCalls.onExitSearch,
                hint = stringResource(R.string.explore_search_hint),
                statusBarPadding = false,
            )
            SearchResultsGrid(
                results = explore.results,
                query = explore.query.orEmpty(),
                layout = layout,
                cards = CardActions(destinations.onOpenList) { menu = HomeChip.Popular to it },
                onRetry = exploreCalls.onRetrySearch,
                onLoadMore = exploreCalls.onLoadMoreSearch,
                onOpenTopic = destinations.onOpenTopic,
                sections = explore.sections,
                onOpenSection = destinations.onOpenSection,
                browse = SearchBrowse(explore.sections, explore.popularTopics),
            )
        }
    } else {
        // The status bar's strip stays put; the header slides under it and back,
        // and the tabs pinned below it never reach the clock.
        EnterAlwaysColumn(
            modifier = Modifier.statusBarsPadding(),
            header = {
                when (layout) {
                    HomeLayout.Portrait -> HomeTopBar(
                        onSearchClick = exploreCalls.onEnterSearch,
                        onSettingsClick = destinations.onSettings,
                        statusBarPadding = false,
                        searchDescription = stringResource(R.string.explore_search_hint),
                    )
                    HomeLayout.CompactHeight -> CompactBar(exploreCalls.onEnterSearch)
                    else -> RailBar(exploreCalls.onEnterSearch)
                }
            },
            body = {
                Column(Modifier.fillMaxSize()) {
                    if (layout == HomeLayout.Portrait) tabs()
                    OfflineStrip(
                        currentPane.offlineSinceMillis,
                        if (chip == HomeChip.Following) following.nowMillis else explore.nowMillis,
                        currentPane.retrying,
                        currentFeed.onRetry,
                    )
                    Box(Modifier.fillMaxSize()) {
                        key(chip) {
                            HomeFeedGrid(
                                chip = chip,
                                onChip = { chip = it },
                                explore = explore,
                                following = following,
                                feed = ChipFeed(currentPane, currentFeed, CardActions(destinations.onOpenList) { menu = chip to it }),
                                layout = layout,
                                gridState = grids.getValue(chip),
                                tiles = TileActions(
                                    onOpen = destinations.onOpenSection,
                                    onFollow = exploreCalls.onFollowSection,
                                    onUnfollow = exploreCalls.onUnfollowSection,
                                    onOpenAll = destinations.onOpenAllSections,
                                ),
                                followingActions = FollowingActions(
                                    onOpenSection = destinations.onOpenSection,
                                    onOpenTopic = destinations.onOpenTopic,
                                    onOpenWaiting = { waitingTopic = it },
                                    onOpenAuthor = { destinations.onOpenAuthor(it.uid, it.name, it.photoUrl) },
                                    onManage = destinations.onManageFollowing,
                                    onTogglePick = exploreCalls.onTogglePick,
                                    onFollowPicked = exploreCalls.onFollowPicked,
                                    onOpenAllSections = destinations.onOpenAllSections,
                                    onOpenWaitingSection = { waitingSection = it.id },
                                    onDismissNotice = exploreCalls.onWithdrawProposal,
                                    onGoToExplore = { chip = HomeChip.Popular },
                                ),
                            )
                        }
                        NewListsPill(
                            visible = currentPane.newListsWaiting,
                            onClick = {
                                scope.launch { grids.getValue(chip).animateScrollToItem(0) }
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

    menu?.let { (from, summary) -> FeedListMenus(summary, { menu = null }, feeds[from], destinations.onOpenAuthor) }
    WaitingSheets(
        topic = waitingTopic,
        section = following.waitingSections.firstOrNull { it.id == waitingSection },
        onWithdraw = exploreCalls.onWithdrawProposal,
        onClose = {
            waitingTopic = null
            waitingSection = null
        },
    )
}

@Composable
private fun WaitingSheets(topic: Topic?, section: Section?, onWithdraw: (String) -> Unit, onClose: () -> Unit) {
    if (topic != null) WaitingTopicSheet(topic = topic, onDismiss = onClose)
    if (section != null) {
        WaitingSectionSheet(
            section = section,
            onWithdraw = {
                onClose()
                onWithdraw(section.id)
            },
            onDismiss = onClose,
        )
    }
}

/** A short window's one bar (§6.1): the name and search; the chips are in the content. It scrolls away with the content. */
@Composable
private fun CompactBar(onSearch: () -> Unit) {
    Column {
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
            Spacer(Modifier.weight(1f))
            SearchButton(onSearch)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** A rail's bar (§7.1): the section of the app it is, and search; Settings lives in the rail. */
@Composable
private fun RailBar(onSearch: () -> Unit) {
    Row(
        Modifier
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

/** Says what it searches, as the phone's bar does: community lists, not the person's own. */
@Composable
private fun SearchButton(onSearch: () -> Unit) {
    HomeIconButton(stringResource(R.string.explore_search_hint), onSearch, testTag = ExploreTestTags.SEARCH) { SearchIcon() }
}

/** What was typed, answered by the server: cards, or a line saying there is nothing. */
@Composable
internal fun SearchResultsGrid(
    results: SearchResults,
    query: String,
    layout: HomeLayout,
    cards: CardActions,
    onRetry: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
    sections: List<Section> = emptyList(),
    onOpenSection: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    /** What a search everywhere offers before anything is typed; null where a search is narrowed to one page. */
    browse: SearchBrowse? = null,
) {
    val alpha = rememberSkeletonAlpha(running = results.loading || results.loadingMore)
    val grid = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= grid.layoutInfo.totalItemsCount - SEARCH_NEAR_END }
    }
    LaunchedEffect(nearEnd, results.lists.size, results.canLoadMore) { if (nearEnd && results.canLoadMore) onLoadMore() }
    val found = matchingSections(sections, query)
    val full: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> androidx.compose.foundation.lazy.grid.GridItemSpan =
        { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(layout.feedColumns.coerceAtLeast(2)),
        state = grid,
        modifier = Modifier
            .fillMaxSize()
            .testTag(ExploreTestTags.SEARCH_RESULTS),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (browse != null && query.isBlank()) {
            browseItems(browse, onOpenSection, onOpenTopic)
            return@LazyVerticalGrid
        }
        // The sections are the phone's own, so they show while the server is still asked.
        foundSectionItems(found, onOpenSection)
        when {
            results.loading -> items(4) { SkeletonCard(alpha) }
            results.failed -> item(span = full) {
                FeedFailedBlock(retrying = false, onRetry = onRetry)
            }

            query.isNotBlank() && results.lists.isEmpty() && results.topics.isEmpty() ->
                if (found.isEmpty()) item(span = full) { SearchNothing() }

            else -> {
                if (results.topics.isNotEmpty()) {
                    item(key = "topics-heading", span = full) { SearchHeading(stringResource(R.string.section_topics)) }
                    item(key = "topics", span = full) { FoundTopicTiles(results.topics, onOpenTopic) }
                }
                if (results.lists.isNotEmpty()) {
                    item(key = "lists-heading", span = full) { SearchHeading(stringResource(R.string.search_lists)) }
                }
                items(results.lists, key = { it.id }) { summary ->
                    FeedCard(
                        summary = summary,
                        onClick = { cards.onOpen(summary) },
                        onLongClick = { cards.onLongPress(summary) },
                    )
                }
                if (results.loadingMore) items(2) { SkeletonCard(alpha) }
            }
        }
    }
}

private const val SEARCH_NEAR_END = 4

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.foundSectionItems(found: List<Section>, onOpenSection: (String) -> Unit) {
    if (found.isEmpty()) return
    val full: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> androidx.compose.foundation.lazy.grid.GridItemSpan =
        { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
    item(key = "sections-heading", span = full) { SearchHeading(stringResource(R.string.search_sections)) }
    item(key = "sections", span = full) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TileRows(found, columns = 3, gap = 8.dp) { section, rowModifier ->
                SectionTile(
                    section = section,
                    following = section.following,
                    onOpen = { onOpenSection(section.id) },
                    onFollow = {},
                    onUnfollow = {},
                    modifier = rowModifier.height(84.dp),
                    style = TileStyle.Mini,
                    longPressMenu = false,
                )
            }
        }
    }
}

/** An empty search everywhere: every section, and the biggest topics with their sections. */
internal class SearchBrowse(val sections: List<Section>, val topics: List<Topic>)

/** Nothing typed yet (design audit 16.11): the sections as chips, then the popular topics, each a tap away. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.browseItems(
    browse: SearchBrowse,
    onOpenSection: (String) -> Unit,
    onOpenTopic: (Topic) -> Unit,
) {
    val full: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> androidx.compose.foundation.lazy.grid.GridItemSpan =
        { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
    item(key = "browse-sections", span = full) {
        Column(Modifier.testTag(ExploreTestTags.SEARCH_BROWSE_SECTIONS), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel(stringResource(R.string.search_sections))
            BrowseSectionChips(browse.sections, onOpenSection)
        }
    }
    if (browse.topics.isNotEmpty()) {
        item(key = "browse-topics", span = full) {
            Column(Modifier.padding(top = 8.dp).testTag(ExploreTestTags.SEARCH_BROWSE_TOPICS)) {
                SectionLabel(stringResource(R.string.search_popular_topics), Modifier.padding(bottom = 4.dp))
                browse.topics.forEach { topic -> PopularTopicRow(topic) { onOpenTopic(topic) } }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowseSectionChips(sections: List<Section>, onOpenSection: (String) -> Unit) {
    val nameOf = rememberSectionNames()
    val ordered = sections.filterNot { it.isOther } + sections.filter { it.isOther }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ordered.forEach { section ->
            SuggestionChip(
                onClick = { onOpenSection(section.id) },
                label = { Text(nameOf(section.id)) },
                modifier = Modifier.testTag(ExploreTestTags.browseSection(section.id)),
            )
        }
    }
}

@Composable
private fun PopularTopicRow(topic: Topic, onClick: () -> Unit) {
    val nameOf = rememberSectionNames()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(ExploreTestTags.browseTopic(topic.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            topic.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        topic.section?.let { section ->
            Text(nameOf(section), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Sections named by what was typed, in any language or by id. */
@Composable
private fun matchingSections(sections: List<Section>, query: String): List<Section> {
    val nameOf = rememberSectionNames()
    return sections.filter { it.matchesSearch(query, label = nameOf(it.id)) }
}

@Composable
private fun SearchHeading(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SearchNothing() {
    Text(
        text = stringResource(R.string.explore_search_nothing),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .testTag(ExploreTestTags.SEARCH_NOTHING),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** A waiting topic survives rotation by what the sheet shows: its id and name. */
private val TopicSaver = androidx.compose.runtime.saveable.listSaver<Topic?, String>(
    save = { topic -> if (topic == null) emptyList() else listOf(topic.id, topic.name) },
    restore = { saved ->
        if (saved.size < 2) null else Topic(id = saved[0], name = saved[1], section = null, status = com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus.Pending)
    },
)
