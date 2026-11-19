package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CardActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCallbacks
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedListMenus
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.HomeLayout
import com.artiuillab.tieryourlife.feature.community.presentation.follow.SignInToFollow
import com.artiuillab.tieryourlife.feature.community.presentation.follow.followingSentence
import com.artiuillab.tieryourlife.feature.community.presentation.follow.sentenceFor
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSectionNames
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** What a section's page can be asked to do beyond its feed. */
class SectionCallbacks(
    val onOpenTopic: (Topic) -> Unit = {},
    val onSelectSort: (FeedSort) -> Unit = {},
    val onToggleSectionFollow: () -> Unit = {},
    /** A new list of one's own, filed under this section; Suggest a topic leads there. */
    val onMakeList: () -> Unit = {},
)

/** The topics' own page-level actions: a waiting topic's sheet and the Suggest a topic window. */
private class TopicCalls(val onOpenWaiting: (Topic) -> Unit, val onSuggest: () -> Unit)

@Composable
fun SectionScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onOpenTopic: (CommunityRoute.Topic) -> Unit = {},
    onSignIn: () -> Unit = {},
    onOpenBoard: (Long) -> Unit = {},
    onOpenSection: (String) -> Unit = {},
    viewModel: SectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.search.query.collectAsStateWithLifecycle()
    val results by viewModel.search.results.collectAsStateWithLifecycle()
    val within by viewModel.search.within.collectAsStateWithLifecycle()
    val allSections by viewModel.allSections.collectAsStateWithLifecycle()
    SignInToFollow(viewModel.signInRequests, onSignIn)
    LaunchedEffect(Unit) { viewModel.open() }
    val openTopic by rememberUpdatedState(onOpenTopic)
    LaunchedEffect(viewModel) { viewModel.mergedIntoTopic.collect { openTopic(it) } }
    val openBoard by rememberUpdatedState(onOpenBoard)
    LaunchedEffect(viewModel) { viewModel.boardMade.collect { openBoard(it) } }
    val languageTag = currentLanguageTag()
    val untitled = stringResource(R.string.section_new_list_title)
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
            onOpenTopic = { onOpenTopic(CommunityRoute.Topic.of(it)) },
            onSelectSort = viewModel::selectSort,
            onToggleSectionFollow = viewModel::toggleSectionFollow,
            onMakeList = { viewModel.makeList(untitled) },
        ),
        onOpenList = { onOpenList(CommunityRoute.CommunityList.of(it, languageTag)) },
        onOpenAuthor = onAuthorClick,
        notices = viewModel.notices,
        search = PageSearch(
            query = query,
            results = results,
            within = within?.let { state.section.label() },
            sections = allSections,
            calls = PageSearchCalls(
                onEnter = viewModel::enterSearch,
                onQueryChange = viewModel.search::update,
                onExit = viewModel.search::exit,
                onWiden = viewModel.search::widen,
                onRetry = viewModel.search::retry,
                onLoadMore = viewModel.search::loadMore,
                onOpenTopic = { onOpenTopic(CommunityRoute.Topic.of(it)) },
                onOpenSection = onOpenSection,
            ),
        ),
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
    search: PageSearch? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    var menuFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var waitingTopic by remember { mutableStateOf<Topic?>(null) }
    var suggesting by rememberSaveable { mutableStateOf(false) }
    val topicCalls = TopicCalls(onOpenWaiting = { waitingTopic = it }, onSuggest = { suggesting = true })
    SectionNotices(notices, snackbar, feed.onRefresh, calls.onToggleSectionFollow)

    val head = PageHead(
        title = state.section.label(),
        following = state.stats?.following == true,
        onToggleFollow = calls.onToggleSectionFollow,
        backdrop = { SectionBackdrop(state.section, state.stats, it) },
        onSearch = search?.calls?.onEnter,
    )
    val cards = CardActions(onOpenList) { menuFor = it }
    val body = PageBody(
        feed = state.feed,
        nowMillis = state.nowMillis,
        callbacks = feed,
        onOpenList = onOpenList,
        items = { layout, alpha -> sectionBody(state, layout, alpha, feed, calls, cards, topicCalls) },
    )
    ListsPage(head, body, onBack, snackbar, SectionTestTags.SCREEN, search, cards)
    FeedListMenus(menuFor, { menuFor = null }, feed, onOpenAuthor)
    waitingTopic?.let { WaitingTopicSheet(it) { waitingTopic = null } }
    if (suggesting) {
        SuggestTopicDialog(
            section = state.section,
            onDismiss = { suggesting = false },
            onMake = {
                suggesting = false
                calls.onMakeList()
            },
        )
    }
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
    val nameOf = rememberSectionNames()
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
                        message = resources.getString(R.string.explore_unfollowed, nameOf(notice.sectionId)),
                        actionLabel = resources.getString(R.string.explore_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onFollowAgain()
                }

                is CommunityNotice.FollowFailed -> snackbar.showSnackbar(resources.sentenceFor(notice.refusal))
                CommunityNotice.ActionFailed -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_action_failed))
                CommunityNotice.TargetIsStaff -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_target_is_staff))
                CommunityNotice.ReportsToday -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_reports_today))
                is CommunityNotice.NowFollowing -> if (notice.moved) snackbar.showSnackbar(resources.followingSentence(notice, nameOf(notice.sectionId)))
                is CommunityNotice.AlreadyDecided -> Unit
            }
        }
    }
}

/**
 * One wait for the topics and the first page together, then everything at
 * once: the topics, a line, and the lists under their heading and sort.
 */
private fun LazyGridScope.sectionBody(
    state: SectionUiState,
    layout: HomeLayout,
    alpha: () -> Float,
    feed: FeedCallbacks,
    calls: SectionCallbacks,
    cards: CardActions,
    topicCalls: TopicCalls,
) {
    if (!state.topicsKnown || state.feed.phase == FeedPhase.Loading) {
        item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { SectionLoading() }
        return
    }
    item(key = "topics", span = { GridItemSpan(maxLineSpan) }) {
        TopicTiles(state.section, state.topics, state.waiting, calls.onOpenTopic, topicCalls.onOpenWaiting, topicCalls.onSuggest)
    }
    listsBlock(state.feed, layout, alpha, cards, feed.onRetry) {
        SortRow(sort = state.sort, onSort = calls.onSelectSort, count = state.stats?.listCount?.takeIf { it > 0 })
    }
}
