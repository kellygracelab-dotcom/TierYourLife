package com.artiuillab.tieryourlife.feature.community.presentation.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.shownTitle
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CardActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCallbacks
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.HomeLayout
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeed
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowRefusal
import com.artiuillab.tieryourlife.feature.community.presentation.follow.SignInToFollow
import com.artiuillab.tieryourlife.feature.community.presentation.follow.sentenceFor
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.community.presentation.section.ListsPage
import com.artiuillab.tieryourlife.feature.community.presentation.section.PageBody
import com.artiuillab.tieryourlife.feature.community.presentation.section.PageHead
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionArt
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionLoading
import com.artiuillab.tieryourlife.feature.community.presentation.section.SortRow
import com.artiuillab.tieryourlife.feature.community.presentation.section.listsBlock
import com.artiuillab.tieryourlife.feature.community.presentation.section.lookOf
import com.artiuillab.tieryourlife.feature.community.presentation.section.tintNow
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSection
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * One topic: the section's page without the topics, its picture over its
 * lists in the order the reader picks. Following it puts its lists into the
 * Following feed beside the people followed.
 */
@Composable
fun TopicScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onSignIn: () -> Unit = {},
    viewModel: TopicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignInToFollow(viewModel.signInRequests, onSignIn)
    TopicScreenContent(
        state = state,
        onBack = onBack,
        onOpenList = onOpenList,
        onAuthorClick = onAuthorClick,
        actions = TopicActions(
            onToggleFollow = viewModel::toggleFollow,
            onSelectSort = viewModel::selectSort,
            onRetry = viewModel::load,
            onLoadMore = viewModel::loadMore,
            onHideList = viewModel::hideList,
            onReport = viewModel::report,
            onNotThisTopic = viewModel::notThisTopic,
        ),
        userMessages = viewModel.userMessages,
        followRefusals = viewModel.followRefusals,
    )
}

/** What a topic's page can be asked to do. */
class TopicActions(
    val onToggleFollow: () -> Unit = {},
    val onSelectSort: (FeedSort) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onHideList: (PublishedListSummary) -> Unit = {},
    val onReport: (PublishedListSummary, ReportReason, String?) -> Unit = { _, _, _ -> },
    val onNotThisTopic: (PublishedListSummary) -> Unit = {},
)

@Composable
fun TopicScreenContent(
    state: TopicUiState,
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit = {},
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    actions: TopicActions = TopicActions(),
    userMessages: Flow<UserMessage> = emptyFlow(),
    followRefusals: Flow<FollowRefusal> = emptyFlow(),
) {
    var actionsFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportedFrom by remember { mutableStateOf<PublishedListSummary?>(null) }
    // How many times "not this topic" was said; each one gets a word back, since nothing on screen changes.
    var told by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val toldMessage = stringResource(R.string.topic_not_this_sent)
    LaunchedEffect(told) {
        if (told > 0) snackbar.showSnackbar(toldMessage)
    }
    val resources = LocalResources.current
    LaunchedEffect(followRefusals) {
        followRefusals.collect { snackbar.showSnackbar(resources.sentenceFor(it)) }
    }

    val languageTag = currentLanguageTag()
    val open = { summary: PublishedListSummary -> onOpenList(CommunityRoute.CommunityList.of(summary, languageTag)) }
    val section = rememberSection(state.section ?: ListCategory.Other.id)
    val topic = state.topic
    val head = PageHead(
        title = state.name,
        following = topic?.following == true,
        // Only an approved word can be followed; the server refuses the rest.
        onToggleFollow = actions.onToggleFollow.takeIf { topic?.status == TopicStatus.Approved },
        overline = if (topic?.pending == true) stringResource(R.string.topic_waiting) else section.label(),
        backdrop = { TopicBackdrop(section, state.picture, answered = topic != null || state.headerFailed, it) },
    )
    val feed = state.feed.asPane()
    val cards = CardActions(open) { actionsFor = it }
    val body = PageBody(
        feed = feed,
        nowMillis = 0L,
        callbacks = FeedCallbacks(onRefresh = actions.onRetry, onRetry = actions.onRetry, onLoadMore = actions.onLoadMore),
        onOpenList = open,
        items = { layout, alpha ->
            topicBody(feed, layout, alpha, cards, actions.onRetry) {
                SortRow(sort = state.sort, onSort = actions.onSelectSort, count = topic?.listCount)
            }
        },
    )
    Box(Modifier.fillMaxSize()) {
        ListsPage(head, body, onBack, snackbar, TopicTestTags.SCREEN)
        ActionFailedSnackbar(userMessages)
    }

    actionsFor?.let { summary ->
        ListActionsSheet(
            title = summary.shownTitle(),
            authorName = summary.authorName.takeUnless { summary.editorial },
            authorPhotoUrl = summary.authorPhotoUrl,
            onDismiss = { actionsFor = null },
            onOpenAuthor = if (summary.editorial) {
                null
            } else {
                {
                    actionsFor = null
                    onAuthorClick(summary.authorUid, summary.authorName, summary.authorPhotoUrl)
                }
            },
            onHide = {
                actionsFor = null
                actions.onHideList(summary)
            }.takeUnless { summary.mine },
            onReport = {
                actionsFor = null
                reportFor = summary
            }.takeUnless { summary.mine },
            onWrongTopic = {
                actionsFor = null
                actions.onNotThisTopic(summary)
                told++
            },
        )
    }

    reportFor?.let { summary ->
        ReportDialog(
            onDismiss = { reportFor = null },
            onSend = { reason, note ->
                reportFor = null
                actions.onReport(summary, reason, note)
                reportedFrom = summary
            },
        )
    }

    reportedFrom?.let { summary ->
        ReportSentDialog(
            authorName = summary.authorName.takeUnless { summary.editorial },
            onDismiss = { reportedFrom = null },
            onHideAuthor = { reportedFrom = null },
        )
    }
}

/** The section's page below its topics: the same wait, then the lists under their heading. */
private fun LazyGridScope.topicBody(
    feed: FeedPane,
    layout: HomeLayout,
    alpha: () -> Float,
    cards: CardActions,
    onRetry: () -> Unit,
    sortRow: @Composable () -> Unit,
) {
    if (feed.phase == FeedPhase.Loading) {
        item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { SectionLoading() }
        return
    }
    listsBlock(feed, layout, alpha, cards, onRetry, sortRow)
}

/**
 * The topic's own picture over the section's tone, never the section's art
 * first: the art stands in only once the server has said there is no picture.
 */
@Composable
private fun TopicBackdrop(section: Section, picture: String?, answered: Boolean, modifier: Modifier) {
    when {
        picture != null -> Box(modifier.background(lookOf(section).tintNow)) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(picture) { ImageRequest.Builder(context).data(picture).crossfade(PICTURE_FADE_MILLIS).build() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        answered -> SectionArt(section, modifier, scrim = false)
        else -> Box(modifier.background(lookOf(section).tintNow))
    }
}

private fun CommunityFeed.asPane(): FeedPane = when (this) {
    CommunityFeed.Loading -> FeedPane()
    is CommunityFeed.Ready -> FeedPane(lists = lists, phase = FeedPhase.Ready, canLoadMore = canLoadMore, loadingMore = loadingMore)
    else -> FeedPane(phase = FeedPhase.Failed)
}

private const val PICTURE_FADE_MILLIS = 150

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun TopicPreview() = TierYourLifeTheme {
    TopicScreenContent(
        state = TopicUiState(
            name = "Pokémon",
            section = ListCategory.Games.id,
            topic = Topic(id = "t1", name = "Pokémon", section = ListCategory.Games.id, status = TopicStatus.Approved),
            feed = CommunityFeed.Ready(emptyList()),
        ),
        onBack = {},
    )
}
