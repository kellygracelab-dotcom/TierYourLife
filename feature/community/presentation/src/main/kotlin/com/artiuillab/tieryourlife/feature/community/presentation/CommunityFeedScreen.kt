package com.artiuillab.tieryourlife.feature.community.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityFeedList
import com.artiuillab.tieryourlife.feature.community.presentation.components.FeedControls
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.presentation.common.OnResumeEffect
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTopBar
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SearchTopBar

/**
 * The community half of the home screen: everybody's published lists, or the
 * lists of the people this person follows. [tabs] is the row that switches
 * halves, drawn under the bar by whichever half is on screen; the home screen
 * passes nothing on a window whose rail does that job.
 */
@Composable
fun CommunityFeedScreen(
    onOpenList: (String) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onSettingsClick: () -> Unit,
    tabs: @Composable () -> Unit = {},
    viewModel: CommunityFeedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Every arrival asks again, quietly: what is on screen stays until the answer lands.
    LaunchedEffect(Unit) { viewModel.load() }
    OnResumeEffect { viewModel.refreshHidden() }

    CommunityFeedScreenContent(
        state = state,
        onOpenList = onOpenList,
        onAuthorClick = onAuthorClick,
        onSettingsClick = onSettingsClick,
        onSearchClick = viewModel::enterSearch,
        onQueryChange = viewModel::updateQuery,
        onCloseSearch = viewModel::exitSearch,
        onRetry = viewModel::load,
        onLoadMore = viewModel::loadMore,
        onSelectCategory = viewModel::selectCategory,
        onSelectSource = viewModel::selectSource,
        onSelectSort = viewModel::selectSort,
        onFollowAuthor = viewModel::followSuggested,
        onHideList = viewModel::hideList,
        onHideAuthor = viewModel::hideAuthor,
        onReportList = viewModel::report,
        tabs = tabs,
    )
}

/** Public, like the other stateless screens: the app is composed one module up and must be drawable without a view model. */
@Composable
fun CommunityFeedScreenContent(
    state: CommunityFeedUiState,
    onOpenList: (String) -> Unit = {},
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onSelectCategory: (ListCategory?) -> Unit = {},
    onSelectSource: (FeedSource) -> Unit = {},
    onSelectSort: (FeedSort) -> Unit = {},
    onFollowAuthor: (String) -> Unit = {},
    onHideList: (PublishedListSummary) -> Unit = {},
    onHideAuthor: (uid: String, name: String) -> Unit = { _, _ -> },
    onReportList: (PublishedListSummary, ReportReason, String?) -> Unit = { _, _, _ -> },
    tabs: @Composable () -> Unit = {},
) {
    val hasRail = currentWindowShape.hasRail
    val searching = state.query != null
    var actionsFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportedFrom by remember { mutableStateOf<PublishedListSummary?>(null) }

    BackHandler(enabled = searching, onBack = onCloseSearch)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (searching) {
                    SearchTopBar(
                        query = state.query.orEmpty(),
                        onQueryChange = onQueryChange,
                        onClose = onCloseSearch,
                    )
                } else {
                    // Settings is promoted into the rail where there is one.
                    HomeTopBar(
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick.takeUnless { hasRail },
                    )
                    tabs()
                }

                CommunityFeedList(
                    feed = state.feed,
                    category = state.category,
                    onSelectCategory = onSelectCategory,
                    onOpen = onOpenList,
                    onRetry = onRetry,
                    onNearEnd = onLoadMore,
                    onLongPress = { actionsFor = it },
                    onOpenAuthor = { uid ->
                        val summary = (state.feed as? CommunityFeed.Ready)
                            ?.lists
                            ?.firstOrNull { it.authorUid == uid }
                        if (summary != null) {
                            onAuthorClick(uid, summary.authorName, summary.authorPhotoUrl)
                        } else {
                            // Offered, not found in the feed: the empty
                            // following screen has authors the feed does not carry.
                            val offered = (state.feed as? CommunityFeed.FollowingNobody)
                                ?.authors
                                ?.firstOrNull { it.uid == uid }
                            if (offered != null) {
                                onAuthorClick(uid, offered.name, offered.photoUrl)
                            }
                        }
                    },
                    controls = FeedControls(
                        source = state.source,
                        sort = state.sort,
                        onSelectSource = onSelectSource,
                        onSelectSort = onSelectSort,
                        onFollow = onFollowAuthor,
                    ),
                )
            }

            actionsFor?.let { summary ->
                ListActionsSheet(
                    title = summary.title,
                    authorName = summary.authorName,
                    authorPhotoUrl = summary.authorPhotoUrl,
                    onDismiss = { actionsFor = null },
                    onOpenAuthor = {
                        actionsFor = null
                        onAuthorClick(summary.authorUid, summary.authorName, summary.authorPhotoUrl)
                    },
                    onHide = {
                        actionsFor = null
                        onHideList(summary)
                    },
                    onReport = {
                        actionsFor = null
                        reportFor = summary
                    },
                )
            }

            reportFor?.let { summary ->
                ReportDialog(
                    onDismiss = { reportFor = null },
                    onSend = { reason, note ->
                        reportFor = null
                        onReportList(summary, reason, note)
                        reportedFrom = summary
                    },
                )
            }

            reportedFrom?.let { summary ->
                ReportSentDialog(
                    authorName = summary.authorName,
                    onDismiss = { reportedFrom = null },
                    onHideAuthor = {
                        reportedFrom = null
                        onHideAuthor(summary.authorUid, summary.authorName)
                    },
                )
            }
        }
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun CommunityFeedLightPreview() = TierYourLifeTheme(false) {
    CommunityFeedScreenContent(state = CommunityFeedUiState(feed = CommunityFeed.Ready(emptyList())))
}

@TierYourLifeDevicePreviews
@Composable
private fun CommunityFeedDarkPreview() = TierYourLifeTheme(true) {
    CommunityFeedScreenContent(state = CommunityFeedUiState(feed = CommunityFeed.Loading))
}
