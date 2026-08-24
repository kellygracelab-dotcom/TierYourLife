package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.OnResumeEffect
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.FormatListBulletedIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeHeader
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTopBar
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
    onSettingsClick: () -> Unit,
    onNewListCreated: (Long) -> Unit,
    viewModel: TierListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultListTitle = stringResource(R.string.default_tier_list_title)
    OnResumeEffect(onResume = viewModel::loadTierLists)

    TierListsScreenContent(
        state = state,
        onTierListClick = onTierListClick,
        onSettingsClick = onSettingsClick,
        onSearchClick = viewModel::enterSearch,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onCloseSearch = viewModel::exitSearch,
        onLongPressCard = viewModel::enterSelection,
        onToggleSelected = viewModel::toggleSelection,
        onCloseSelection = viewModel::exitSelection,
        onDeleteLists = viewModel::deleteTierLists,
        onUndoDelete = viewModel::restoreTierLists,
        onCreateList = { viewModel.createTierList(defaultListTitle, onNewListCreated) },
        userMessages = viewModel.userMessages,
    )
}

@Composable
internal fun TierListsScreenContent(
    state: TierListsUiState,
    onTierListClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onLongPressCard: (Long) -> Unit = {},
    onToggleSelected: (Long) -> Unit = {},
    onCloseSelection: () -> Unit = {},
    onDeleteLists: (List<Long>) -> Unit = {},
    onUndoDelete: (List<Long>) -> Unit = {},
    onCreateList: () -> Unit = {},
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    val success = state as? TierListsUiState.Success
    val mode = success?.mode ?: HomeMode.Browsing
    val lists = success?.lists.orEmpty()
    val totalListCount = success?.totalListCount ?: 0
    val rankedCount = success?.rankedCount ?: 0

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

    LaunchedEffect(Unit) {
        userMessages.collect { snackbarHostState.showSnackbar(actionFailedMessage) }
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

                if (mode !is HomeMode.Searching) {
                    HomeHeader(totalListCount = totalListCount, rankedCount = rankedCount)
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

                    is TierListsUiState.Success -> when (mode) {
                        is HomeMode.Searching -> SearchResults(
                            lists = lists,
                            query = mode.query,
                            onTierListClick = onTierListClick,
                        )

                        else -> if (totalListCount == 0) {
                            HomeEmptyState()
                        } else {
                            HomeContent(
                                lists = lists,
                                mode = mode,
                                onTierListClick = onTierListClick,
                                onLongPressCard = onLongPressCard,
                                onToggleSelected = onToggleSelected,
                            )
                        }
                    }
                }
            }

            if (mode == HomeMode.Browsing) {
                val newListDescription = stringResource(R.string.cd_new_list)
                FloatingActionButton(
                    onClick = onCreateList,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .size(56.dp)
                        .semantics { contentDescription = newListDescription }
                        .testTag(TierListsTestTags.FAB),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    PlusIcon(24.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                }
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
    onTierListClick: (Long) -> Unit,
    onLongPressCard: (Long) -> Unit,
    onToggleSelected: (Long) -> Unit,
) {
    val isSelecting = mode is HomeMode.Selecting
    val selectedIds = (mode as? HomeMode.Selecting)?.selectedIds.orEmpty()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TierListsTestTags.LISTS),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lists, key = { it.id }) { list ->
            val isSelected = list.id in selectedIds
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
        Text(
            text = pluralStringResource(R.plurals.search_results_count, lists.size, lists.size),
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)
                .testTag(TierListsTestTags.SEARCH_RESULTS_COUNT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(lists, key = { it.id }) { list ->
                TierListCard(list = list, onClick = { onTierListClick(list.id) })
            }
        }
    }
}

@Composable
private fun HomeEmptyState() {
    HomeEmptyStateLayout(
        icon = { FormatListBulletedIcon(28.dp, MaterialTheme.colorScheme.outline) },
        title = stringResource(R.string.home_empty_title),
        body = stringResource(R.string.home_empty_body),
        bottomOffset = 80.dp,
        testTag = TierListsTestTags.EMPTY_STATE,
    )
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
