package com.artiuillab.tieryourlife.feature.tier.presentation.coverpicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt
import com.artiuillab.tieryourlife.feature.tier.board.components.MOSAIC_CELLS
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchViewModel
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.FieldFillDark
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.FieldFillLight
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.SelectedRowTintDark
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.SelectedRowTintLight
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.CenteredMessage
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.ResultsList
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.SearchField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private const val PREVIEW_ASPECT = 16f / 9f
private const val CANDIDATE_ASPECT = 3f / 4f
private val CANDIDATE_MIN_WIDTH = 78.dp
private val BADGE_SIZE = 22.dp

/**
 * Up to six of the board's pictures, in the order they are tapped, drawn at
 * the top exactly as the feed will draw them. A gallery photograph is the
 * cover by itself: it travels by being uploaded, which a collage does not do.
 */
@Composable
fun CoverPickerScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CoverPickerViewModel = hiltViewModel(),
    searchViewModel: CatalogueSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickFromGallery(uri.toString())
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    CoverPickerScreenContent(
        state = state,
        searchState = searchState,
        query = query,
        onBack = onBack,
        onDone = viewModel::save,
        onReset = viewModel::reset,
        onShowTab = viewModel::showTab,
        onToggle = viewModel::toggle,
        onGallery = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onQueryChange = {
            query = it
            searchViewModel.onQueryChange(it)
        },
        onSearchClick = { searchViewModel.search(query) },
        onLoadMore = searchViewModel::loadMore,
        userMessages = viewModel.userMessages,
    )
}

@Composable
fun CoverPickerScreenContent(
    state: CoverPickerUiState,
    searchState: CatalogueSearchUiState,
    query: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onReset: () -> Unit = {},
    onShowTab: (CoverPickerTab) -> Unit = {},
    onToggle: (String) -> Unit = {},
    onGallery: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    Box(Modifier.fillMaxSize().testTag(CoverPickerTestTags.SCREEN)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            CenteredContent(ContentWidth.Reading) {
                PickerTopBar(onBack = onBack, onDone = onDone, saving = state.saving)
                ListArt(
                    coverImageUrl = state.coverImageUrl,
                    previewImages = state.previewImages,
                    tierColors = state.tierColors,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .aspectRatio(PREVIEW_ASPECT)
                        .clip(RoundedCornerShape(16.dp)),
                    testTag = CoverPickerTestTags.PREVIEW,
                )
                StatusLine(state = state, onReset = onReset)
                Tabs(state = state, onShowTab = onShowTab)
            }
            when (state.tab) {
                CoverPickerTab.Cards -> CardsTab(
                    state = state,
                    onToggle = onToggle,
                    onGallery = onGallery,
                    modifier = Modifier.weight(1f),
                )

                CoverPickerTab.Search -> SearchTab(
                    chosen = state.chosen,
                    searchState = searchState,
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearchClick = onSearchClick,
                    onLoadMore = onLoadMore,
                    onToggle = onToggle,
                    onCloseSearch = { onShowTab(CoverPickerTab.Cards) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ActionFailedSnackbar(userMessages)
    }
}

@Composable
private fun PickerTopBar(onBack: () -> Unit, onDone: () -> Unit, saving: Boolean) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription },
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.cover_picker_title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            onClick = onDone,
            enabled = !saving,
            modifier = Modifier.testTag(CoverPickerTestTags.DONE),
        ) {
            Text(stringResource(R.string.cover_picker_done))
        }
    }
}

@Composable
private fun StatusLine(state: CoverPickerUiState, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when (state.chosen.size) {
                0 -> stringResource(R.string.cover_picker_status_automatic)
                1 -> stringResource(R.string.cover_picker_status_one)
                else -> pluralStringResource(
                    R.plurals.cover_picker_status_collage,
                    state.chosen.size,
                    state.chosen.size,
                    MOSAIC_CELLS,
                )
            },
            modifier = Modifier
                .weight(1f)
                .testTag(CoverPickerTestTags.STATUS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.chosen.isNotEmpty()) {
            TextButton(onClick = onReset, modifier = Modifier.testTag(CoverPickerTestTags.RESET)) {
                Text(stringResource(R.string.cover_picker_reset))
            }
        }
    }
}

@Composable
private fun Tabs(state: CoverPickerUiState, onShowTab: (CoverPickerTab) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(40.dp),
    ) {
        CoverPickerTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = state.tab == tab,
                onClick = { onShowTab(tab) },
                shape = SegmentedButtonDefaults.itemShape(index, CoverPickerTab.entries.size),
                modifier = Modifier.testTag(
                    if (tab == CoverPickerTab.Cards) CoverPickerTestTags.TAB_CARDS else CoverPickerTestTags.TAB_SEARCH,
                ),
            ) {
                Text(
                    text = if (tab == CoverPickerTab.Cards) {
                        stringResource(R.string.cover_picker_tab_cards, state.candidates.size)
                    } else {
                        stringResource(R.string.cover_picker_tab_search)
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CardsTab(
    state: CoverPickerUiState,
    onToggle: (String) -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredContent(ContentWidth.Reading, modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(CANDIDATE_MIN_WIDTH),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = systemBarsContentPadding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.candidates.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.cover_picker_no_cards),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.candidates, key = { it }) { url ->
                Candidate(
                    url = url,
                    number = state.chosen.indexOf(url).takeIf { it >= 0 }?.plus(1),
                    onClick = { onToggle(url) },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    TextButton(
                        onClick = onGallery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(CoverPickerTestTags.GALLERY),
                    ) {
                        Text(stringResource(R.string.cover_picker_gallery))
                    }
                    Text(
                        text = stringResource(R.string.cover_picker_gallery_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

/** A chosen picture wears its place in the collage; the number is the order of the taps. */
@Composable
private fun Candidate(url: String, number: Int?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .aspectRatio(CANDIDATE_ASPECT)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (number != null) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .selectable(selected = number != null, onClick = onClick)
            .testTag(CoverPickerTestTags.candidate(url)),
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (number != null) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(BADGE_SIZE)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .wrapContentSize(Alignment.Center),
            )
        }
    }
}

/** The same catalogue the cards come from; a result is chosen by its picture, like a card. */
@Composable
private fun SearchTab(
    chosen: List<String>,
    searchState: CatalogueSearchUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onLoadMore: () -> Unit,
    onToggle: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = TierYourLifeMedia.current.isDark
    Column(modifier) {
        CenteredContent(ContentWidth.Reading) {
            Spacer(Modifier.height(4.dp))
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSearchClick = onSearchClick,
                onClose = onCloseSearch,
                fill = if (isDark) FieldFillDark else FieldFillLight,
            )
            if (searchState is CatalogueSearchUiState.Loading) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (searchState) {
                CatalogueSearchUiState.Initial -> CenteredMessage(text = stringResource(R.string.item_search_min_query))

                CatalogueSearchUiState.Loading -> Unit

                is CatalogueSearchUiState.Empty -> CenteredMessage(
                    text = stringResource(R.string.item_search_empty_title, searchState.query),
                )

                is CatalogueSearchUiState.Success -> ResultsList(
                    items = searchState.items,
                    selectedIds = searchState.items.filter { it.imageUrl in chosen }.mapTo(mutableSetOf()) { it.id },
                    selectedTint = if (isDark) SelectedRowTintDark else SelectedRowTintLight,
                    onToggle = { item -> item.imageUrl?.let(onToggle) },
                    loadingMore = searchState.loadingMore,
                    onNearEnd = onLoadMore,
                )

                CatalogueSearchUiState.Error -> CenteredMessage(
                    text = stringResource(R.string.item_search_error_title),
                    body = stringResource(R.string.item_search_error_body),
                    actionLabel = stringResource(R.string.item_search_try_again),
                    onAction = onSearchClick,
                )
            }
        }
    }
}

@Preview(name = "Three chosen", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun CoverPickerPreview() {
    val pictures = List(5) { "https://example.test/$it.jpg" }
    TierYourLifeTheme {
        CoverPickerScreenContent(
            state = CoverPickerUiState(
                title = "Space films that hold up",
                candidates = pictures,
                automatic = pictures,
                tierColors = listOf("#B03A32", "#C06A25", "#A98B1F", "#3F7F55", "#3C6E99"),
                chosen = pictures.take(3),
            ),
            searchState = CatalogueSearchUiState.Initial,
            query = "",
            onBack = {},
            onDone = {},
        )
    }
}
