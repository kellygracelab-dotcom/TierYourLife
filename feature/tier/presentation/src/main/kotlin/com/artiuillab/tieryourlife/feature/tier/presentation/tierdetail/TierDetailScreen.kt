package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AddMovieSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FloatingDragTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.MoveItemSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.NoteAddIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PoolPanel
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierRow
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TrashTarget
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList
import kotlinx.coroutines.launch

// testTag constants shared between production UI and instrumentation tests, so
// the two never drift out of sync.
internal object TierDetailTestTags {
    const val LOADING = "tier_detail_loading"
    const val ADD_CHIP = "tier_detail_add_chip"
    const val POOL_ITEMS = "tier_detail_pool_items"
    const val POOL_PANEL = "tier_detail_pool_panel"
    const val MOVE_SHEET = "tier_detail_move_sheet"
    const val MOVE_SHEET_REMOVE = "tier_detail_move_sheet_remove"
    const val MOVE_SHEET_POOL = "tier_detail_move_sheet_pool"
    const val TRASH_TARGET = "tier_detail_trash_target"
    const val DELETED_ITEM_SNACKBAR = "tier_detail_deleted_item_snackbar"
    const val LIST_SETTINGS_SCREEN = "tier_detail_list_settings_screen"
    const val NEW_TIER_ROW = "tier_detail_new_tier_row"
    const val TIER_EDITOR_SHEET = "tier_detail_tier_editor_sheet"
    const val TIER_EDITOR_LABEL_FIELD = "tier_detail_tier_editor_label_field"
    const val TIER_EDITOR_CAPTION_FIELD = "tier_detail_tier_editor_caption_field"
    const val TIER_EDITOR_CANCEL = "tier_detail_tier_editor_cancel"
    const val TIER_EDITOR_SAVE = "tier_detail_tier_editor_save"
    const val TIER_EDITOR_CUSTOM_SWATCH = "tier_detail_tier_editor_custom_swatch"
    const val TIER_EDITOR_CUSTOM_TAB_LIGHT = "tier_detail_tier_editor_custom_tab_light"
    const val TIER_EDITOR_CUSTOM_TAB_DARK = "tier_detail_tier_editor_custom_tab_dark"
    const val TIER_EDITOR_HUE_SLIDER = "tier_detail_tier_editor_hue_slider"
    const val TIER_EDITOR_SATURATION_SLIDER = "tier_detail_tier_editor_saturation_slider"
    const val TIER_EDITOR_LIGHTNESS_SLIDER = "tier_detail_tier_editor_lightness_slider"
    const val TIER_EDITOR_HEX_FIELD = "tier_detail_tier_editor_hex_field"
    const val TIER_EDITOR_CONTRAST_READOUT = "tier_detail_tier_editor_contrast_readout"
    const val TIER_EDITOR_PREVIEW_LIGHT = "tier_detail_tier_editor_preview_light"
    const val TIER_EDITOR_PREVIEW_DARK = "tier_detail_tier_editor_preview_dark"
    fun tierRow(tierId: Long): String = "tier_detail_row_$tierId"
    fun tierItems(tierId: Long): String = "tier_detail_items_$tierId"
    fun tile(itemId: Long): String = "tier_detail_tile_$itemId"
    fun moveSheetTierOption(tierId: Long): String = "tier_detail_move_sheet_tier_$tierId"
    fun tierEditorPresetSwatch(index: Int): String = "tier_detail_tier_editor_preset_swatch_$index"
    fun sliderTrack(sliderTag: String): String = "${sliderTag}_track"
    fun sliderThumb(sliderTag: String): String = "${sliderTag}_thumb"
}

@Composable
fun TierDetailScreen(
    onBack: () -> Unit,
    viewModel: TierDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }

    TierDetailScreenContent(
        state = state,
        onBack = onBack,
        onAddClick = { addSheetVisible = true },
        onMoveItem = viewModel::moveItem,
        onDeleteItem = viewModel::deleteItem,
        onRestoreItem = viewModel::restoreItem,
        onAddTier = viewModel::addTier,
    )

    if (addSheetVisible) {
        AddMovieSheet(
            onDismiss = { addSheetVisible = false },
            onMovieSelected = { title, imageUrl ->
                viewModel.addMovieToPool(title, imageUrl)
                addSheetVisible = false
            },
        )
    }
}

@Composable
fun TierDetailScreenContent(
    state: TierDetailUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
    onDeleteItem: (itemId: Long) -> Unit = {},
    onRestoreItem: (itemId: Long) -> Unit = {},
    onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit = { _, _, _, _ -> },
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            is TierDetailUiState.Loading -> {
                Column(Modifier.fillMaxSize()) {
                    TierScreenTopBar(title = "", onBack = onBack, onManualAdd = onAddClick)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(TierDetailTestTags.LOADING),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is TierDetailUiState.Success -> {
                TierScreenBody(
                    list = state.list,
                    onBack = onBack,
                    onAddClick = onAddClick,
                    onMoveItem = onMoveItem,
                    onDeleteItem = onDeleteItem,
                    onRestoreItem = onRestoreItem,
                    onAddTier = onAddTier,
                )
            }

            is TierDetailUiState.Error -> {
                Column {
                    TierScreenTopBar(title = "", onBack = onBack, onManualAdd = onAddClick)
                    Text(
                        text = state.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TierScreenBody(
    list: TierList,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onRestoreItem: (itemId: Long) -> Unit,
    onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit,
) {
    var listSettingsVisible by remember { mutableStateOf(false) }

    if (listSettingsVisible) {
        TierListSettingsScreenContent(
            list = list,
            onBack = { listSettingsVisible = false },
            onAddTier = onAddTier,
        )
        return
    }

    val rankedTiers = list.tiers.filterNot { it.isPool }
    val pool = list.tiers.firstOrNull { it.isPool }
    val dragController = remember { TierDragController() }
    var chooserItemId by remember { mutableStateOf<Long?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val deletedMessageTemplate = stringResource(R.string.tier_detail_item_moved_to_trash)
    val undoLabel = stringResource(R.string.tier_detail_snackbar_undo)

    // The one place both delete paths (drag to trash, remove from the move sheet)
    // funnel through, so there is exactly one message per deletion.
    val deleteAndAnnounce: (Long) -> Unit = { itemId ->
        val title = list.tiers.flatMap { it.items }.firstOrNull { it.id == itemId }?.title.orEmpty()
        onDeleteItem(itemId)
        // Replace, don't queue: a second removal while the snackbar is up shouldn't
        // leave the user reaching for an Undo that belongs to a different poster.
        snackbarHostState.currentSnackbarData?.dismiss()
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = String.format(deletedMessageTemplate, title),
                actionLabel = undoLabel,
                // Deletion already happened; Undo is an offer, not a question, so this
                // must not use the implicit Indefinite default a non-null actionLabel
                // gets otherwise — that variant is for choices blocking further action.
                // Short, not Long: this fires on a frequent action (deleting posters one
                // after another), so it shouldn't linger in the way any longer than it has to.
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRestoreItem(itemId)
            }
        }
    }

    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TierScreenTopBar(
                title = list.title,
                onBack = onBack,
                onManualAdd = onAddClick,
                onMoreClick = { listSettingsVisible = true },
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rankedTiers, key = { it.id }) { tier ->
                    TierRow(
                        tier = tier,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = deleteAndAnnounce,
                        onDoubleTap = { itemId -> chooserItemId = itemId },
                    )
                }
            }

            if (pool != null) {
                PoolPanel(
                    pool = pool,
                    onAddClick = onAddClick,
                    dragController = dragController,
                    onMoveItem = onMoveItem,
                    onDeleteItem = deleteAndAnnounce,
                    onDoubleTap = { itemId -> chooserItemId = itemId },
                )
            }
        }

        if (dragController.isDragging) {
            TrashTarget(
                dragController = dragController,
                poolTierId = pool?.id,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            FloatingDragTile(dragController)
        }

        // Anchored 16dp above the pool's own measured top — same "raised, not resting
        // on the pool" idea the mock uses for the trash target — rather than a fixed
        // dp guess from the screen edge, which drifts into an overlap once the pool's
        // own navigationBarsPadding makes it taller than the mock's own layout assumed.
        val poolTop = pool?.id?.let { dragController.tierBounds(it)?.top }
        val bottomGap = if (poolTop != null) {
            (maxHeight - with(density) { poolTop.toDp() }) + 16.dp
        } else {
            16.dp
        }

        DeletedItemSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomGap),
        )
    }

    val chosenId = chooserItemId
    val currentTier = chosenId?.let { id -> list.tiers.firstOrNull { tier -> tier.items.any { it.id == id } } }
    val chosenItem = currentTier?.items?.firstOrNull { it.id == chosenId }
    if (chosenId != null && currentTier != null && chosenItem != null) {
        MoveItemSheet(
            item = chosenItem,
            currentTierId = currentTier.id,
            rankedTiers = rankedTiers,
            pool = pool,
            onMoveToTier = { toTierId, toPosition ->
                onMoveItem(chosenId, toTierId, toPosition)
                chooserItemId = null
            },
            onRemove = {
                deleteAndAnnounce(chosenId)
                chooserItemId = null
            },
            onDismiss = { chooserItemId = null },
        )
    }
}

@Composable
private fun TierScreenTopBar(
    title: String,
    onBack: () -> Unit,
    onManualAdd: () -> Unit,
    onMoreClick: () -> Unit = {},
) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
    val manualAddDescription = stringResource(R.string.tier_detail_content_description_manual_add)
    val moreDescription = stringResource(R.string.tier_detail_content_description_more)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
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
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onManualAdd,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = manualAddDescription },
        ) { NoteAddIcon() }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = moreDescription },
        ) { MoreIcon() }
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun TierScreenLightPreview() = TierYourLifeTheme(false) {
    TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList), onBack = {}, onAddClick = {})
}

@TierYourLifeDevicePreviews
@Composable
private fun TierScreenDarkPreview() = TierYourLifeTheme(true) {
    TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList), onBack = {}, onAddClick = {})
}

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenLoadingPreview() {
    TierYourLifeTheme {
        TierDetailScreenContent(state = TierDetailUiState.Loading, onBack = {}, onAddClick = {})
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenErrorPreview() {
    TierYourLifeTheme {
        TierDetailScreenContent(
            state = TierDetailUiState.Error(message = "No connection to server"),
            onBack = {},
            onAddClick = {},
        )
    }
}
