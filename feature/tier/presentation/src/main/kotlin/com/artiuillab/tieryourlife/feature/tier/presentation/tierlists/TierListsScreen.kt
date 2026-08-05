package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteOutlineIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.FormatListBulletedIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SearchIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SearchOffIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.SettingsIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.TierListCard
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.previewTierLists
import kotlinx.coroutines.launch

// testTag constants shared between production UI and instrumentation tests.
internal object TierListsTestTags {
    const val LOADING = "tier_lists_loading"
    const val LISTS = "tier_lists"
    const val SEARCH_FIELD = "home_search_field"
    const val SEARCH_CLOSE = "home_search_close"
    const val SEARCH_CLEAR = "home_search_clear"
    const val SEARCH_RESULTS_COUNT = "home_search_results_count"
    const val SEARCH_NO_RESULTS = "home_search_no_results"
    const val SELECTION_BAR = "home_selection_bar"
    const val SELECTION_CLOSE = "home_selection_close"
    const val SELECTION_DELETE = "home_selection_delete"
    const val FAB = "home_fab"
    const val EMPTY_STATE = "home_empty_state"
    fun tierListCard(id: Long): String = "tier_list_card_$id"
}

// Mock-literal colors with no matching M3 role — the same pair already reused by
// MoveItemSheet's "currently here" tint and the tier-detail settings screen's selected
// display-mode row (docs/design-spec-home.md, section 3).
private val SelectedRowTintLight = Color(0xFFEDEBFA)
private val SelectedRowTintDark = Color(0xFF2E2F45)

@Composable
fun TierListsScreen(
    onTierListClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onNewListCreated: (Long) -> Unit,
    viewModel: TierListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
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
        onCreateList = { viewModel.createTierList(onNewListCreated) },
    )
}

// Reads happen here, not in the view model's init: the view model survives both a
// round trip to the detail screen and a configuration change, so an init-time load
// only ever covers the very first appearance — renaming, deleting, restoring or
// changing the display mode on the detail screen would never be reflected back here.
//
// LocalLifecycleOwner inside a navigation-compose destination is the back stack
// entry's own lifecycle, not the activity's: it pauses while another destination is
// on top and resumes when this one is back on top, which is exactly "returned to
// this screen" — recomposition alone never toggles it, so an unrelated state change
// elsewhere on the screen can't trigger a second read. Lifecycle.addObserver also
// delivers a catch-up ON_RESUME the moment it is registered on an already-resumed
// lifecycle, which is what covers the very first appearance without a second,
// separate trigger for it.
@Composable
internal fun OnResumeEffect(onResume: () -> Unit) {
    val currentOnResume by rememberUpdatedState(onResume)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
) {
    val success = state as? TierListsUiState.Success
    val mode = success?.mode ?: HomeMode.Browsing
    val lists = success?.lists.orEmpty()
    val totalListCount = success?.totalListCount ?: 0
    val rankedCount = success?.rankedCount ?: 0

    // System back leaves search or selection rather than the screen (docs/design-spec-home.md,
    // sections 2 and 3) — only enabled while one of those two modes is active, so plain
    // browsing still lets an ordinary back press pop this destination as usual.
    BackHandler(enabled = mode !is HomeMode.Browsing) {
        when (mode) {
            is HomeMode.Searching -> onCloseSearch()
            is HomeMode.Selecting -> onCloseSelection()
            HomeMode.Browsing -> Unit
        }
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)

    // The one place a Home delete funnels through, so there is exactly one message per
    // deletion and a second delete while the first snackbar is still up replaces it
    // rather than queuing behind it (docs/design-spec-home.md, section 5).
    val deleteAndAnnounce: (List<Long>) -> Unit = { ids ->
        onDeleteLists(ids)
        snackbarHostState.currentSnackbarData?.dismiss()
        coroutineScope.launch {
            val message = context.resources.getQuantityString(R.plurals.snack_lists_deleted, ids.size, ids.size)
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

                // The heading and its summary line stay put while selecting, and only
                // the bar above them changes. The design had them collapse away, which
                // meant entering the mode yanked the whole list upward under the
                // finger that was still resting on the card it had just long-pressed —
                // and left the first card flush against the bar with nothing between
                // them. Search is different: there the field replaces the bar and the
                // heading really is redundant beside it.
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

                    is TierListsUiState.Error -> Text(
                        text = state.message,
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
private fun HomeHeader(totalListCount: Int, rankedCount: Int) {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
    ) {
        Text(
            text = stringResource(R.string.tier_lists_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // Hidden entirely when there are no lists — "0 lists · 0 ranked" under a
        // heading that already says there are none is noise (docs/design-spec-home.md,
        // section 1).
        if (totalListCount > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.tier_lists_summary,
                    pluralStringResource(R.plurals.tier_lists_count, totalListCount, totalListCount),
                    pluralStringResource(R.plurals.tier_lists_rankings_count, rankedCount, rankedCount),
                    stringResource(R.string.tier_lists_private),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeTopBar(onSearchClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        HomeIconButton(stringResource(R.string.tier_lists_content_description_search), onSearchClick) {
            SearchIcon()
        }
        HomeIconButton(
            stringResource(R.string.tier_lists_content_description_settings),
            onSettingsClick,
        ) { SettingsIcon() }
    }
}

@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val closeDescription = stringResource(R.string.cd_close_search)
    val clearDescription = stringResource(R.string.cd_clear_query)
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        leadingIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .semantics { contentDescription = closeDescription }
                    .testTag(TierListsTestTags.SEARCH_CLOSE),
            ) { BackIcon() }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .semantics { contentDescription = clearDescription }
                        .testTag(TierListsTestTags.SEARCH_CLEAR),
                ) { ClearIcon(20.dp, onSurfaceVariant) }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
            .height(56.dp)
            .focusRequester(focusRequester)
            .testTag(TierListsTestTags.SEARCH_FIELD),
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp)
                .testTag(TierListsTestTags.SELECTION_BAR),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeIconButton(
                stringResource(R.string.cd_close_selection),
                onClose,
                TierListsTestTags.SELECTION_CLOSE,
            ) { ClearIcon(24.dp, onSurfaceVariant) }
            Text(
                text = pluralStringResource(R.plurals.selection_count, count, count),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val deleteDescription = stringResource(
                R.string.cd_delete_selected,
                pluralStringResource(R.plurals.tier_lists_count, count, count),
            )
            HomeIconButton(
                deleteDescription,
                onDelete,
                TierListsTestTags.SELECTION_DELETE,
            ) { DeleteOutlineIcon(24.dp, onSurfaceVariant) }
        }
    }
}

@Composable
private fun HomeIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String? = null,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) { content() }
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
    val isDark = TierYourLifeMedia.current.isDark
    val selectedTint = if (isDark) SelectedRowTintDark else SelectedRowTintLight

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TierListsTestTags.LISTS),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lists, key = { it.id }) { list ->
            val isSelected = list.id in selectedIds
            // The tint is drawn here, on the item's own full-width background, and only
            // the card inside it gets the usual 16dp side inset — that's what lets the
            // tint bleed edge to edge past the card's own margins while the card itself
            // stays completely unchanged (docs/design-spec-home.md, section 3).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) selectedTint else Color.Transparent)
                    .padding(horizontal = 16.dp),
            ) {
                TierListCard(
                    list = list,
                    onClick = {
                        if (isSelecting) onToggleSelected(list.id) else onTierListClick(list.id)
                    },
                    onLongClick = {
                        if (!isSelecting) onLongPressCard(list.id)
                    },
                    // Driven purely by isSelecting, which is shared by every card in
                    // this LazyColumn — that's what makes every unselected card grow its
                    // empty checkbox on the same frame the long-pressed one fills,
                    // rather than staggering in one at a time (docs/design-spec-home.md,
                    // section 11).
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

// Shared by the "no lists at all" and "no search results" empty states — both are a
// 56dp surfaceContainerLow circle holding a 28dp glyph, a headline and a body, centred
// with some amount of bottom offset so the block sits clear of the FAB or search field.
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
