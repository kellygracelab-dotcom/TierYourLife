package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AddItemsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FloatingDragRow
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FloatingDragTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.MoveItemSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PoolPanel
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.RankedList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.RankedPoolSection
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierEditorSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierRow
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TrashTarget
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

internal object TierDetailTestTags {
    const val LOADING = "tier_detail_loading"
    const val HEADER_TITLE = "tier_detail_header_title"
    const val ADD_CHIP = "tier_detail_add_chip"
    const val POOL_ITEMS = "tier_detail_pool_items"
    const val POOL_PANEL = "tier_detail_pool_panel"
    const val MOVE_SHEET = "tier_detail_move_sheet"
    const val MOVE_SHEET_REMOVE = "tier_detail_move_sheet_remove"
    const val MOVE_SHEET_POOL = "tier_detail_move_sheet_pool"
    const val TRASH_TARGET = "tier_detail_trash_target"
    const val FLOATING_DRAG_TILE = "tier_detail_floating_drag_tile"
    const val DELETED_ITEM_SNACKBAR = "tier_detail_deleted_item_snackbar"
    const val RANKED_LIST = "tier_detail_ranked_list"
    const val RANKED_HEADER = "tier_detail_ranked_header"
    const val RANKED_POOL_COLLAPSED = "tier_detail_ranked_pool_collapsed"
    const val RANKED_POOL_ITEMS = "tier_detail_ranked_pool_items"
    const val LIST_SETTINGS_SCREEN = "tier_detail_list_settings_screen"
    const val LIST_SETTINGS_MODE_WRAP = "tier_detail_list_settings_mode_wrap"
    const val LIST_SETTINGS_MODE_STRIP = "tier_detail_list_settings_mode_strip"
    const val LIST_SETTINGS_MODE_RANKED = "tier_detail_list_settings_mode_ranked"
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
    const val ITEM_SEARCH_FIELD = "tier_detail_item_search_field"
    const val ITEM_SEARCH_CLOSE = "tier_detail_item_search_close"
    const val ITEM_SEARCH_CLEAR = "tier_detail_item_search_clear"
    const val ITEM_SEARCH_SELECTED_COUNT = "tier_detail_item_search_selected_count"
    const val ITEM_SEARCH_CONFIRM = "tier_detail_item_search_confirm"
    const val ITEM_SEARCH_TRY_AGAIN = "tier_detail_item_search_try_again"
    const val ITEM_SEARCH_RESULTS_LIST = "tier_detail_item_search_results_list"
    const val ITEM_SEARCH_BOTTOM_BAR = "tier_detail_item_search_bottom_bar"
    const val MANUAL_ADD_BUTTON = "tier_detail_manual_add_button"
    const val MANUAL_ENTRY_DIALOG = "tier_detail_manual_entry_dialog"
    const val MANUAL_ENTRY_NAME_FIELD = "tier_detail_manual_entry_name_field"
    const val MANUAL_ENTRY_PHOTO_FRAME = "tier_detail_manual_entry_photo_frame"
    const val MANUAL_ENTRY_CHOOSE_PHOTO = "tier_detail_manual_entry_choose_photo"
    const val MANUAL_ENTRY_REMOVE_PHOTO = "tier_detail_manual_entry_remove_photo"
    const val MANUAL_ENTRY_CANCEL = "tier_detail_manual_entry_cancel"
    const val MANUAL_ENTRY_SAVE = "tier_detail_manual_entry_save"
    fun itemSearchResult(id: String): String = "tier_detail_item_search_result_$id"
    fun tierRow(tierId: Long): String = "tier_detail_row_$tierId"
    fun tierBand(tierId: Long): String = "tier_detail_band_$tierId"
    fun rankedRow(itemId: Long): String = "tier_detail_ranked_row_$itemId"
    fun rankedPoolItem(itemId: Long): String = "tier_detail_ranked_pool_item_$itemId"
    fun tierItems(tierId: Long): String = "tier_detail_items_$tierId"
    fun tile(itemId: Long): String = "tier_detail_tile_$itemId"
    fun moveSheetTierOption(tierId: Long): String = "tier_detail_move_sheet_tier_$tierId"
    fun tierEditorPresetSwatch(index: Int): String = "tier_detail_tier_editor_preset_swatch_$index"
    fun sliderTrack(sliderTag: String): String = "${sliderTag}_track"
    fun sliderThumb(sliderTag: String): String = "${sliderTag}_thumb"
}

private val TIER_LIST_AUTOSCROLL_EDGE = 72.dp
private const val TIER_LIST_AUTOSCROLL_MAX_SPEED_DP_PER_SEC = 600

private const val LOADING_SPINNER_DELAY_MILLIS = 150L

private fun autoScrollSpeedPx(pointerY: Float, top: Float, bottom: Float, edgePx: Float, maxSpeedPx: Float): Float {
    val distanceFromTop = pointerY - top
    val distanceFromBottom = bottom - pointerY
    return when {
        distanceFromTop <= edgePx -> -maxSpeedPx * (1f - distanceFromTop / edgePx).coerceIn(0f, 1f)
        distanceFromBottom <= edgePx -> maxSpeedPx * (1f - distanceFromBottom / edgePx).coerceIn(0f, 1f)
        else -> 0f
    }
}

@Composable
fun TierDetailScreen(
    onBack: () -> Unit,
    startInTitleEdit: Boolean = false,
    viewModel: TierDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canDiscard by viewModel.canDiscard.collectAsStateWithLifecycle()
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }
    var manualEntryVisible by rememberSaveable { mutableStateOf(false) }

    TierDetailScreenContent(
        state = state,
        startInTitleEdit = startInTitleEdit,
        canDiscard = canDiscard,
        actions = TierDetailActions(
            onBack = onBack,
            onDiscard = { viewModel.discardList(onBack) },
            onTitleEditStarted = viewModel::markTouched,
            onAddClick = { addSheetVisible = true },
            onManualAddClick = { manualEntryVisible = true },
            onMoveItem = viewModel::moveItem,
            onDeleteItem = viewModel::deleteItem,
            onRestoreItem = viewModel::restoreItem,
            onReorderTiers = viewModel::reorderTiers,
            onDeleteTierToPool = viewModel::deleteTierToPool,
            onRestoreTier = viewModel::restoreTier,
            onAddTier = viewModel::addTier,
            onEditTier = viewModel::editTier,
            onSetDisplayMode = viewModel::setDisplayMode,
            onRenameList = viewModel::renameTierList,
        ),
    )

    if (addSheetVisible) {
        AddItemsSheet(
            listTitle = (state as? TierDetailUiState.Success)?.list?.title.orEmpty(),
            onDismiss = { addSheetVisible = false },
            onItemsConfirmed = { items ->
                viewModel.addItemsToPool(items)
                addSheetVisible = false
            },
        )
    }

    if (manualEntryVisible) {
        ManualEntryDialog(
            onDismiss = { manualEntryVisible = false },
            onSave = { title, photoUris ->
                viewModel.addManualItem(title, photoUris)
                manualEntryVisible = false
            },
        )
    }
}

@Composable
internal fun TierDetailScreenContent(
    state: TierDetailUiState,
    actions: TierDetailActions = TierDetailActions(),
    startInTitleEdit: Boolean = false,
    canDiscard: Boolean = false,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            is TierDetailUiState.Loading -> {
                var spinnerVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(LOADING_SPINNER_DELAY_MILLIS)
                    spinnerVisible = true
                }

                Column(Modifier.fillMaxSize()) {
                    TierScreenTopBar(title = "", onBack = actions.onBack, onManualAdd = actions.onManualAddClick)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(TierDetailTestTags.LOADING),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (spinnerVisible) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            is TierDetailUiState.Success -> {
                TierScreenBody(
                    list = state.list,
                    actions = actions,
                    startInTitleEdit = startInTitleEdit,
                    canDiscard = canDiscard,
                )
            }

            TierDetailUiState.Error -> {
                Column {
                    TierScreenTopBar(title = "", onBack = actions.onBack, onManualAdd = actions.onManualAddClick)
                    Text(
                        text = stringResource(R.string.tier_detail_not_found),
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
    actions: TierDetailActions,
    startInTitleEdit: Boolean = false,
    canDiscard: Boolean = false,
) {
    val onBack = actions.onBack
    val onDiscard = actions.onDiscard
    val onTitleEditStarted = actions.onTitleEditStarted
    val onAddClick = actions.onAddClick
    val onManualAddClick = actions.onManualAddClick
    val onMoveItem = actions.onMoveItem
    val onDeleteItem = actions.onDeleteItem
    val onRestoreItem = actions.onRestoreItem
    val onReorderTiers = actions.onReorderTiers
    val onDeleteTierToPool = actions.onDeleteTierToPool
    val onRestoreTier = actions.onRestoreTier
    val onAddTier = actions.onAddTier
    val onEditTier = actions.onEditTier
    val onSetDisplayMode = actions.onSetDisplayMode
    val onRenameList = actions.onRenameList
    var listSettingsVisible by remember { mutableStateOf(false) }

    if (listSettingsVisible) {
        TierListSettingsScreenContent(
            list = list,
            onBack = { listSettingsVisible = false },
            onAddTier = onAddTier,
            onSetDisplayMode = onSetDisplayMode,
        )
        return
    }

    val rankedTiers = list.tiers.filterNot { it.isPool }
    val pool = list.tiers.firstOrNull { it.isPool }
    val layoutDirection = LocalLayoutDirection.current
    val dragController = remember { TierDragController() }
    var chooserItemId by remember { mutableStateOf<Long?>(null) }
    var editingTierId by remember { mutableStateOf<Long?>(null) }

    SideEffect {
        dragController.setValidTargets(
            tierIds = list.tiers.map { it.id },
            itemIds = list.tiers.flatMap { it.items }.map { it.id },
            rightToLeft = layoutDirection == LayoutDirection.Rtl,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val deletedMessageTemplate = stringResource(R.string.tier_detail_item_moved_to_trash)
    val undoLabel = stringResource(R.string.tier_detail_snackbar_undo)

    val deleteAndAnnounce: (Long) -> Unit = { itemId ->
        val title = list.tiers.flatMap { it.items }.firstOrNull { it.id == itemId }?.title.orEmpty()
        onDeleteItem(itemId)
        snackbarHostState.currentSnackbarData?.dismiss()
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = String.format(deletedMessageTemplate, title),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRestoreItem(itemId)
            }
        }
    }

    val tierDeletedMessageTemplate = stringResource(R.string.tier_detail_tier_moved_to_trash)

    val deleteTierAndAnnounce: (Long) -> Unit = { tierId ->
        val tier = rankedTiers.firstOrNull { it.id == tierId }
        val poolId = pool?.id
        if (tier != null && poolId != null) {
            val position = rankedTiers.indexOfFirst { it.id == tierId }
            val itemIds = tier.items.map { it.id }
            onDeleteTierToPool(tierId)
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = String.format(tierDeletedMessageTemplate, tier.label),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onRestoreTier(tier.label, tier.caption, tier.colorLight, tier.colorDark, position, itemIds)
                }
            }
        }
    }

    val density = LocalDensity.current

    val tierListState = rememberLazyListState()
    var tierListBounds by remember { mutableStateOf(Rect.Zero) }

    val autoscrollEdgePx = with(density) { TIER_LIST_AUTOSCROLL_EDGE.toPx() }
    val autoscrollMaxSpeedPx = with(density) { TIER_LIST_AUTOSCROLL_MAX_SPEED_DP_PER_SEC.dp.toPx() }

    fun autoscrollSpeedNow(): Float {
        if (tierListBounds == Rect.Zero) return 0f
        val raw = autoScrollSpeedPx(
            pointerY = dragController.pointerPositionInRoot.y,
            top = tierListBounds.top,
            bottom = tierListBounds.bottom,
            edgePx = autoscrollEdgePx,
            maxSpeedPx = autoscrollMaxSpeedPx,
        )
        return when {
            raw < 0f && !tierListState.canScrollBackward -> 0f
            raw > 0f && !tierListState.canScrollForward -> 0f
            else -> raw
        }
    }

    val isNearAutoscrollEdge = dragController.isDragging && !dragController.isDraggingTier && autoscrollSpeedNow() != 0f

    LaunchedEffect(isNearAutoscrollEdge) {
        if (!isNearAutoscrollEdge) return@LaunchedEffect
        var lastFrameNanos = -1L
        while (isActive) {
            val delta = withFrameNanos { frameNanos ->
                val dtSeconds = if (lastFrameNanos < 0L) 0f else (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos
                autoscrollSpeedNow() * dtSeconds
            }
            if (delta != 0f) {
                tierListState.scrollBy(delta)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TierScreenTopBar(
                title = list.title,
                onBack = onBack,
                onManualAdd = onManualAddClick,
                onMoreClick = { listSettingsVisible = true },
                onRenameList = onRenameList,
                titleEditable = true,
                startInTitleEdit = startInTitleEdit,
                canDiscard = canDiscard,
                onDiscard = onDiscard,
                onTitleEditStarted = onTitleEditStarted,
            )

            if (list.displayMode == TierListDisplayMode.FLAT_RANKED) {
                RankedList(
                    rankedTiers = rankedTiers,
                    onSelect = { itemId -> chooserItemId = itemId },
                    modifier = Modifier.weight(1f),
                )

                if (pool != null) {
                    RankedPoolSection(
                        pool = pool,
                        onAddClick = onAddClick,
                        onSelect = { itemId -> chooserItemId = itemId },
                    )
                }
            } else {
                LazyColumn(
                    state = tierListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates -> tierListBounds = coordinates.boundsInRoot() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rankedTiers, key = { it.id }) { tier ->
                        TierRow(
                            tier = tier,
                            displayMode = list.displayMode,
                            dragController = dragController,
                            rankedTierIds = rankedTiers.map { it.id },
                            onMoveItem = onMoveItem,
                            onDeleteItem = deleteAndAnnounce,
                            onReorderTiers = onReorderTiers,
                            onDeleteTier = deleteTierAndAnnounce,
                            onDoubleTap = { itemId -> chooserItemId = itemId },
                            onEditTier = { tierId -> editingTierId = tierId },
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
        }

        if (dragController.isDragging) {
            TrashTarget(
                dragController = dragController,
                poolTierId = pool?.id,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            FloatingDragTile(dragController)
            FloatingDragRow(dragController, rankedTiers.firstOrNull { it.id == dragController.draggedTierId })
        }

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

    val editingTier = editingTierId?.let { id -> list.tiers.firstOrNull { it.id == id } }
    if (editingTier != null) {
        TierEditorSheet(
            initialTier = editingTier,
            onDismiss = { editingTierId = null },
            onSave = { label, caption, colorLight, colorDark ->
                onEditTier(editingTier.id, label, caption, colorLight, colorDark)
                editingTierId = null
            },
        )
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun TierScreenLightPreview() = TierYourLifeTheme(false) {
    TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList))
}

@TierYourLifeDevicePreviews
@Composable
private fun TierScreenDarkPreview() = TierYourLifeTheme(true) {
    TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList))
}

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenLoadingPreview() {
    TierYourLifeTheme {
        TierDetailScreenContent(state = TierDetailUiState.Loading)
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenErrorPreview() {
    TierYourLifeTheme {
        TierDetailScreenContent(
            state = TierDetailUiState.Error,
        )
    }
}
