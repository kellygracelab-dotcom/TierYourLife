package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.OnResumeEffect
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.CommunityFeedList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.ConflictBanner
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeEmptyState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeHeader
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTabs
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTopBar
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.LocalOnlyFooter
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.LocalOnlySignInCard
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.RestoringPictures
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SearchOffIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SearchTopBar
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SelectionTopBar
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.TierListCard
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.previewTierLists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun TierListsScreen(
    onTierListClick: (Long) -> Unit,
    onCommunityListClick: (String) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onSettingsClick: () -> Unit,
    onSignInClick: () -> Unit,
    onNewListCreated: (Long) -> Unit,
    viewModel: TierListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultListTitle = stringResource(R.string.default_tier_list_title)
    OnResumeEffect {
        viewModel.loadTierLists()
        viewModel.refreshHidden()
    }

    TierListsScreenContent(
        state = state,
        onTierListClick = onTierListClick,
        onAuthorClick = onAuthorClick,
        onSettingsClick = onSettingsClick,
        onSignInClick = onSignInClick,
        onDismissSignInOffer = viewModel::dismissSignInOffer,
        onDismissConflictNotice = viewModel::dismissConflictNotice,
        onSearchClick = viewModel::enterSearch,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onCloseSearch = viewModel::exitSearch,
        onLongPressCard = viewModel::enterSelection,
        onToggleSelected = viewModel::toggleSelection,
        onCloseSelection = viewModel::exitSelection,
        onDeleteLists = viewModel::deleteTierLists,
        onUndoDelete = viewModel::restoreTierLists,
        onCreateList = { viewModel.createTierList(defaultListTitle, onNewListCreated) },
        onCreateNamedList = { title -> viewModel.createTierList(title, onNewListCreated) },
        onSelectTab = viewModel::selectTab,
        onOpenCommunityList = onCommunityListClick,
        onRetryCommunity = viewModel::loadCommunityFeed,
        onLoadMoreCommunity = viewModel::loadMoreCommunity,
        onSelectCommunityCategory = viewModel::selectCommunityCategory,
        onHideCommunityList = viewModel::hideCommunityList,
        onHideCommunityAuthor = viewModel::hideCommunityAuthor,
        onReportCommunityList = viewModel::reportCommunityList,
        userMessages = viewModel.userMessages,
    )
}

@Composable
internal fun TierListsScreenContent(
    state: TierListsUiState,
    onTierListClick: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    onSettingsClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onDismissSignInOffer: () -> Unit = {},
    onDismissConflictNotice: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onLongPressCard: (Long) -> Unit = {},
    onToggleSelected: (Long) -> Unit = {},
    onCloseSelection: () -> Unit = {},
    onDeleteLists: (List<Long>) -> Unit = {},
    onUndoDelete: (List<Long>) -> Unit = {},
    onCreateList: () -> Unit = {},
    onCreateNamedList: (String) -> Unit = {},
    onSelectTab: (HomeTab) -> Unit = {},
    onOpenCommunityList: (String) -> Unit = {},
    onRetryCommunity: () -> Unit = {},
    onLoadMoreCommunity: () -> Unit = {},
    onSelectCommunityCategory: (ListCategory?) -> Unit = {},
    onHideCommunityList: (PublishedListSummary) -> Unit = {},
    onHideCommunityAuthor: (uid: String, name: String) -> Unit = { _, _ -> },
    onReportCommunityList: (PublishedListSummary, ReportReason, String?) -> Unit = { _, _, _ -> },
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    val success = state as? TierListsUiState.Success
    val mode = success?.mode ?: HomeMode.Browsing
    val lists = success?.lists.orEmpty()
    val totalListCount = success?.totalListCount ?: 0
    val rankedCount = success?.rankedCount ?: 0
    val tab = success?.tab ?: HomeTab.Mine
    val communityFeed = success?.community ?: CommunityFeed.Loading
    val communityCategory = success?.communityCategory
    val localOnly = success?.localOnly ?: LocalOnly.Unknown
    val restoringPictures = success?.restoringPictures ?: PictureRestore.Progress.Idle
    val conflict = success?.conflict
    var actionsFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportedFrom by remember { mutableStateOf<PublishedListSummary?>(null) }

    BackHandler(enabled = mode !is HomeMode.Browsing) {
        when (mode) {
            is HomeMode.Searching -> onCloseSearch()
            is HomeMode.Selecting -> onCloseSelection()
            HomeMode.Browsing -> Unit
        }
    }

    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)
    val actionFailedMessage = stringResource(R.string.snack_action_failed)
    val stillPublicMessage = stringResource(R.string.snack_published_list_still_public)

    LaunchedEffect(Unit) {
        userMessages.collect { message ->
            snackbarHostState.showSnackbar(
                when (message) {
                    UserMessage.ActionFailed -> actionFailedMessage
                    UserMessage.PublishedListStillPublic -> stillPublicMessage
                },
            )
        }
    }

    val deleteAndAnnounce: (List<Long>) -> Unit = { ids ->
        onDeleteLists(ids)
        snackbarHostState.currentSnackbarData?.dismiss()
        coroutineScope.launch {
            val message = resources.getQuantityString(R.plurals.snack_lists_deleted, ids.size, ids.size)
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoDelete(ids)
            }
        }
    }

    val showsEmptyState = state is TierListsUiState.Success &&
        mode !is HomeMode.Searching &&
        tab == HomeTab.Mine &&
        totalListCount == 0

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                when (mode) {
                    is HomeMode.Searching -> SearchTopBar(
                        query = mode.query,
                        onQueryChange = onSearchQueryChange,
                        onClose = onCloseSearch,
                    )

                    is HomeMode.Selecting -> SelectionTopBar(
                        count = mode.selectedIds.size,
                        onClose = onCloseSelection,
                        onDelete = { deleteAndAnnounce(mode.selectedIds.toList()) },
                    )

                    HomeMode.Browsing -> HomeTopBar(
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick,
                    )
                }

                // Directly under the bar, and above the tabs: it is about
                // everything below it, not about whichever tab is showing.
                RestoringPictures(restoringPictures)

                if (mode !is HomeMode.Searching) {
                    HomeTabs(selected = tab, onSelect = onSelectTab)
                    // The counters describe this phone's lists, which says
                    // nothing about the feed the Community tab is showing.
                    if (tab == HomeTab.Mine) {
                        HomeHeader(totalListCount = totalListCount, rankedCount = rankedCount)
                    }
                }

                when (state) {
                    TierListsUiState.Loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.testTag(TierListsTestTags.LOADING))
                    }

                    TierListsUiState.Error -> Text(
                        text = stringResource(R.string.tier_lists_load_error),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    is TierListsUiState.Success -> when {
                        // Searching on this tab asks the server, so the same
                        // feed answers; it is not the local lists filtered.
                        tab == HomeTab.Community -> CommunityFeedList(
                            feed = communityFeed,
                            category = communityCategory,
                            onSelectCategory = onSelectCommunityCategory,
                            onOpen = onOpenCommunityList,
                            onRetry = onRetryCommunity,
                            onNearEnd = onLoadMoreCommunity,
                            onLongPress = { actionsFor = it },
                            onOpenAuthor = { uid ->
                                val summary = (communityFeed as? CommunityFeed.Ready)
                                    ?.lists
                                    ?.firstOrNull { it.authorUid == uid }
                                if (summary != null) {
                                    onAuthorClick(uid, summary.authorName, summary.authorPhotoUrl)
                                }
                            },
                        )

                        mode is HomeMode.Searching -> SearchResults(
                            lists = lists,
                            query = mode.query,
                            onTierListClick = onTierListClick,
                        )

                        totalListCount == 0 -> HomeEmptyState(onCreateNamedList = onCreateNamedList)

                        else -> HomeContent(
                            lists = lists,
                            mode = mode,
                            localOnly = localOnly,
                            conflict = conflict,
                            onTierListClick = onTierListClick,
                            onLongPressCard = onLongPressCard,
                            onToggleSelected = onToggleSelected,
                            onSignInClick = onSignInClick,
                            onDismissSignInOffer = onDismissSignInOffer,
                            onDismissConflictNotice = onDismissConflictNotice,
                        )
                    }
                }
            }

            if (mode == HomeMode.Browsing && tab == HomeTab.Mine) {
                val newListDescription = stringResource(R.string.cd_new_list)
                val fabModifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .semantics { contentDescription = newListDescription }
                    .testTag(TierListsTestTags.FAB)

                // A bare plus has nothing to take its meaning from until the
                // first board exists.
                if (showsEmptyState) {
                    ExtendedFloatingActionButton(
                        onClick = onCreateList,
                        modifier = fabModifier,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        icon = { PlusIcon(24.dp, MaterialTheme.colorScheme.onPrimaryContainer) },
                        text = { Text(stringResource(R.string.home_new_list)) },
                    )
                } else {
                    FloatingActionButton(
                        onClick = onCreateList,
                        modifier = fabModifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        PlusIcon(24.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
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
                        onHideCommunityList(summary)
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
                        onReportCommunityList(summary, reason, note)
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
                        onHideCommunityAuthor(summary.authorUid, summary.authorName)
                    },
                )
            }

            DeletedItemSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun HomeContent(
    lists: List<TierList>,
    mode: HomeMode,
    localOnly: LocalOnly,
    conflict: TierList?,
    onTierListClick: (Long) -> Unit,
    onLongPressCard: (Long) -> Unit,
    onToggleSelected: (Long) -> Unit,
    onSignInClick: () -> Unit,
    onDismissSignInOffer: () -> Unit,
    onDismissConflictNotice: (String) -> Unit,
) {
    val isSelecting = mode is HomeMode.Selecting
    val selectedIds = (mode as? HomeMode.Selecting)?.selectedIds.orEmpty()
    val here = localOnly as? LocalOnly.Here
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TierListsTestTags.LISTS),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Above the boards, because it is about all of them. Out of the way
        // while somebody is picking boards to delete: two sets of buttons
        // asking different questions is one too many.
        // Above everything, including the sign-in offer: two copies of one
        // board is news, and the offer can wait a screen.
        if (conflict != null && !isSelecting) {
            item(key = "conflict-banner") {
                CenteredContent(ContentWidth.Reading, Modifier.padding(horizontal = 16.dp)) {
                    ConflictBanner(
                        title = conflict.title,
                        onGotIt = { onDismissConflictNotice(conflict.title) },
                    )
                }
            }
        }
        if (here?.offerSignIn == true && !isSelecting) {
            item(key = "local-only-card") {
                CenteredContent(ContentWidth.Reading, Modifier.padding(horizontal = 16.dp)) {
                    LocalOnlySignInCard(
                        boardCount = lists.size,
                        onSignIn = onSignInClick,
                        onDismiss = onDismissSignInOffer,
                    )
                }
            }
        }
        items(lists, key = { it.id }) { list ->
            val isSelected = list.id in selectedIds
            CenteredContent(ContentWidth.Reading, Modifier.padding(horizontal = 16.dp)) {
                TierListCard(
                    list = list,
                    onClick = {
                        if (isSelecting) onToggleSelected(list.id) else onTierListClick(list.id)
                    },
                    onLongClick = {
                        if (!isSelecting) onLongPressCard(list.id)
                    },
                    selectionMode = isSelecting,
                    selected = isSelected,
                )
            }
        }
        if (here != null) {
            item(key = "local-only-footer") {
                CenteredContent(ContentWidth.Reading, Modifier.padding(horizontal = 16.dp)) {
                    LocalOnlyFooter(boardCount = lists.size)
                }
            }
        }
    }
}

@Composable
private fun SearchResults(lists: List<TierList>, query: String, onTierListClick: (Long) -> Unit) {
    if (lists.isEmpty()) {
        HomeEmptyStateLayout(
            icon = { SearchOffIcon(28.dp, MaterialTheme.colorScheme.outline) },
            title = stringResource(R.string.search_no_results_title, query),
            body = stringResource(R.string.search_no_results_body),
            bottomOffset = 120.dp,
            testTag = TierListsTestTags.SEARCH_NO_RESULTS,
        )
        return
    }

    Column {
        // The same measure as the results, or the count stops standing over
        // what it counts.
        CenteredContent(ContentWidth.Reading) {
            Text(
                text = pluralStringResource(R.plurals.search_results_count, lists.size, lists.size),
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)
                    .testTag(TierListsTestTags.SEARCH_RESULTS_COUNT),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(lists, key = { it.id }) { list ->
                CenteredContent(ContentWidth.Reading) {
                    TierListCard(list = list, onClick = { onTierListClick(list.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyStateLayout(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    bottomOffset: Dp,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = bottomOffset)
                .widthIn(max = 264.dp)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListsLightPreview() = TierYourLifeTheme(false) {
    TierListsScreenContent(
        state = TierListsUiState.Success(previewTierLists, previewTierLists.size, 24),
        onTierListClick = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListsDarkPreview() = TierYourLifeTheme(true) {
    TierListsScreenContent(
        state = TierListsUiState.Success(previewTierLists, previewTierLists.size, 24),
        onTierListClick = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListsEmptyPreview() = TierYourLifeTheme(false) {
    TierListsScreenContent(
        state = TierListsUiState.Success(emptyList(), 0, 0),
        onTierListClick = {},
    )
}
