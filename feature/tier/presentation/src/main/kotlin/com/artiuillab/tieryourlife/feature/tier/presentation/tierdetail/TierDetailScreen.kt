package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AddMovieSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FloatingDragRow
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FloatingDragTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.MoveItemSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.NoteAddIcon
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

// testTag constants shared between production UI and instrumentation tests, so
// the two never drift out of sync.
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
    const val MOVIE_SEARCH_FIELD = "tier_detail_movie_search_field"
    const val MOVIE_SEARCH_CLOSE = "tier_detail_movie_search_close"
    const val MOVIE_SEARCH_CLEAR = "tier_detail_movie_search_clear"
    const val MOVIE_SEARCH_SELECTED_COUNT = "tier_detail_movie_search_selected_count"
    const val MOVIE_SEARCH_CONFIRM = "tier_detail_movie_search_confirm"
    const val MOVIE_SEARCH_TRY_AGAIN = "tier_detail_movie_search_try_again"
    const val MOVIE_SEARCH_RESULTS_LIST = "tier_detail_movie_search_results_list"
    const val MOVIE_SEARCH_BOTTOM_BAR = "tier_detail_movie_search_bottom_bar"
    const val MANUAL_ADD_BUTTON = "tier_detail_manual_add_button"
    const val MANUAL_ENTRY_DIALOG = "tier_detail_manual_entry_dialog"
    const val MANUAL_ENTRY_NAME_FIELD = "tier_detail_manual_entry_name_field"
    const val MANUAL_ENTRY_PHOTO_FRAME = "tier_detail_manual_entry_photo_frame"
    const val MANUAL_ENTRY_CHOOSE_PHOTO = "tier_detail_manual_entry_choose_photo"
    const val MANUAL_ENTRY_REMOVE_PHOTO = "tier_detail_manual_entry_remove_photo"
    const val MANUAL_ENTRY_CANCEL = "tier_detail_manual_entry_cancel"
    const val MANUAL_ENTRY_SAVE = "tier_detail_manual_entry_save"
    fun movieSearchResult(id: String): String = "tier_detail_movie_search_result_$id"
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

// docs/design-spec-turns-8-9.md, section 3 / 8a: "Drag auto-scroll within 72dp of the
// top/bottom at up to 600dp/s".
private val TIER_LIST_AUTOSCROLL_EDGE = 72.dp
private const val TIER_LIST_AUTOSCROLL_MAX_SPEED_DP_PER_SEC = 600

// How long a read may take before it is worth telling the user it is happening.
private const val LOADING_SPINNER_DELAY_MILLIS = 150L

// Positive scrolls down, negative scrolls up. A pointer within edgePx of an edge (or past
// it entirely — a lifted tile can be dragged beyond the list's own bounds) scrolls that
// direction, ramping linearly from 0 at the edge of the zone to maxSpeedPx right at (or
// past) the edge itself; 0 once the pointer is back in the middle of the list.
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
    val state by viewModel.state.collectAsState()
    val canDiscard by viewModel.canDiscard.collectAsState()
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }
    var manualEntryVisible by rememberSaveable { mutableStateOf(false) }

    TierDetailScreenContent(
        state = state,
        onBack = onBack,
        startInTitleEdit = startInTitleEdit,
        canDiscard = canDiscard,
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
    )

    if (addSheetVisible) {
        AddMovieSheet(
            listTitle = (state as? TierDetailUiState.Success)?.list?.title.orEmpty(),
            onDismiss = { addSheetVisible = false },
            onMoviesConfirmed = { movies ->
                viewModel.addMoviesToPool(movies)
                addSheetVisible = false
            },
        )
    }

    if (manualEntryVisible) {
        ManualEntryDialog(
            onDismiss = { manualEntryVisible = false },
            onSave = { title, photoUri ->
                viewModel.addManualItem(title, photoUri)
                manualEntryVisible = false
            },
        )
    }
}

@Composable
fun TierDetailScreenContent(
    state: TierDetailUiState,
    onBack: () -> Unit,
    startInTitleEdit: Boolean = false,
    canDiscard: Boolean = false,
    onDiscard: () -> Unit = {},
    onTitleEditStarted: () -> Unit = {},
    onAddClick: () -> Unit,
    onManualAddClick: () -> Unit = {},
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
    onDeleteItem: (itemId: Long) -> Unit = {},
    onRestoreItem: (itemId: Long) -> Unit = {},
    onReorderTiers: (orderedTierIds: List<Long>) -> Unit = {},
    onDeleteTierToPool: (tierId: Long, poolId: Long, poolSize: Int, itemIds: List<Long>) -> Unit = { _, _, _, _ -> },
    onRestoreTier: (
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) -> Unit = { _, _, _, _, _, _ -> },
    onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit = { _, _, _, _ -> },
    onEditTier: (id: Long, label: String, caption: String?, colorLight: String, colorDark: String) -> Unit =
        { _, _, _, _, _ -> },
    onSetDisplayMode: (displayMode: TierListDisplayMode) -> Unit = {},
    onRenameList: (title: String) -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            is TierDetailUiState.Loading -> {
                // Reading one list out of Room takes a handful of milliseconds, so a
                // spinner drawn the instant this state appears is on screen for two or
                // three frames and reads as a flash rather than as progress — worse
                // than showing nothing. The bar is drawn immediately so the screen is
                // never blank; only the spinner waits, and it appears solely on the
                // reads slow enough to be worth reporting.
                var spinnerVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(LOADING_SPINNER_DELAY_MILLIS)
                    spinnerVisible = true
                }

                Column(Modifier.fillMaxSize()) {
                    TierScreenTopBar(title = "", onBack = onBack, onManualAdd = onManualAddClick)
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
                    onBack = onBack,
                    startInTitleEdit = startInTitleEdit,
                    canDiscard = canDiscard,
                    onDiscard = onDiscard,
                    onTitleEditStarted = onTitleEditStarted,
                    onAddClick = onAddClick,
                    onManualAddClick = onManualAddClick,
                    onMoveItem = onMoveItem,
                    onDeleteItem = onDeleteItem,
                    onRestoreItem = onRestoreItem,
                    onReorderTiers = onReorderTiers,
                    onDeleteTierToPool = onDeleteTierToPool,
                    onRestoreTier = onRestoreTier,
                    onAddTier = onAddTier,
                    onEditTier = onEditTier,
                    onSetDisplayMode = onSetDisplayMode,
                    onRenameList = onRenameList,
                )
            }

            is TierDetailUiState.Error -> {
                Column {
                    TierScreenTopBar(title = "", onBack = onBack, onManualAdd = onManualAddClick)
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
    startInTitleEdit: Boolean = false,
    canDiscard: Boolean = false,
    onDiscard: () -> Unit = {},
    onTitleEditStarted: () -> Unit = {},
    onAddClick: () -> Unit,
    onManualAddClick: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onRestoreItem: (itemId: Long) -> Unit,
    onReorderTiers: (orderedTierIds: List<Long>) -> Unit,
    onDeleteTierToPool: (tierId: Long, poolId: Long, poolSize: Int, itemIds: List<Long>) -> Unit,
    onRestoreTier: (
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) -> Unit,
    onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit,
    onEditTier: (id: Long, label: String, caption: String?, colorLight: String, colorDark: String) -> Unit,
    onSetDisplayMode: (displayMode: TierListDisplayMode) -> Unit,
    onRenameList: (title: String) -> Unit,
) {
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
    val dragController = remember { TierDragController() }
    var chooserItemId by remember { mutableStateOf<Long?>(null) }
    var editingTierId by remember { mutableStateOf<Long?>(null) }

    // The second layer against a stale drag-target registry: refreshed every
    // recomposition with whatever this exact list currently holds, so a target that no
    // longer exists is never chosen even if something failed to unregister its bounds.
    SideEffect {
        dragController.setValidTargets(
            tierIds = list.tiers.map { it.id },
            itemIds = list.tiers.flatMap { it.items }.map { it.id },
        )
    }

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

    val tierDeletedMessageTemplate = stringResource(R.string.tier_detail_tier_moved_to_trash)

    // Same shape as deleteAndAnnounce above, for a tier instead of an item: everything
    // undo needs (label, caption, colours, the tier's position among the ranked tiers,
    // and which items it held) is captured here, synchronously, from list — the same
    // data the screen is already showing — before the tier and its position are gone.
    val deleteTierAndAnnounce: (Long) -> Unit = { tierId ->
        val tier = rankedTiers.firstOrNull { it.id == tierId }
        val poolId = pool?.id
        if (tier != null && poolId != null) {
            val position = rankedTiers.indexOfFirst { it.id == tierId }
            val itemIds = tier.items.map { it.id }
            onDeleteTierToPool(tierId, poolId, pool.items.size, itemIds)
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

    // Hoisted so a drag near its top/bottom edge can scroll it programmatically (see the
    // autoscroll effect below) — bounds tracked the same way every other drag-target
    // rect in this screen is, via onGloballyPositioned on the LazyColumn itself.
    val tierListState = rememberLazyListState()
    var tierListBounds by remember { mutableStateOf(Rect.Zero) }

    val autoscrollEdgePx = with(density) { TIER_LIST_AUTOSCROLL_EDGE.toPx() }
    val autoscrollMaxSpeedPx = with(density) { TIER_LIST_AUTOSCROLL_MAX_SPEED_DP_PER_SEC.dp.toPx() }

    // Speed clamped to zero once there's nowhere further to scroll in that direction — a
    // tile lifted from the very first row sits within 72dp of the list's own top edge by
    // construction (that's just where row one is), and that must not read as "scroll up"
    // when the list is already at the top; symmetrically for the last row and the bottom.
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

    // Rows now grow taller than the screen (FlowRow wrapping), so a poster dragged toward
    // a tier that scrolled out of view had nothing to carry it there — this is what does.
    // Keyed on "currently near a scrollable edge" specifically, not merely "a drag is
    // happening": the loop below awaits a fresh frame every iteration for as long as it
    // runs, which a Compose UI test's idling check treats as perpetually busy — scoping it
    // this way means an ordinary drag that never approaches a scrollable edge never starts
    // it, and it cancels the instant the pointer moves away (or the list runs out of room).
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

@Composable
private fun TierScreenTopBar(
    title: String,
    onBack: () -> Unit,
    onManualAdd: () -> Unit,
    onMoreClick: () -> Unit = {},
    onRenameList: (String) -> Unit = {},
    titleEditable: Boolean = false,
    startInTitleEdit: Boolean = false,
    canDiscard: Boolean = false,
    onDiscard: () -> Unit = {},
    onTitleEditStarted: () -> Unit = {},
) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
    val discardDescription = stringResource(R.string.tier_detail_content_description_discard)
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
        // A brand-new, untouched list gets a close cross that hard-deletes it instead
        // of the ordinary back arrow — the moment the list is touched, canDiscard flips
        // for good and this reverts to the plain back arrow (docs/design-spec-home.md,
        // section 12).
        if (canDiscard) {
            IconButton(
                onClick = onDiscard,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = discardDescription },
            ) { ClearIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = backDescription },
            ) { BackIcon() }
        }
        if (titleEditable) {
            EditableListTitle(
                title = title,
                onRename = onRenameList,
                initiallyEditing = startInTitleEdit,
                onEditStarted = onTitleEditStarted,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
        } else {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(
            onClick = onManualAdd,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = manualAddDescription }
                .testTag(TierDetailTestTags.MANUAL_ADD_BUTTON),
        ) { NoteAddIcon() }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = moreDescription },
        ) { MoreIcon() }
    }
}

// One path to one action: the list's title is edited in place here, and the
// settings screen's old "Rename list" row is gone. At rest this is exactly the
// same Text the header always had (same size, same padding, same position) so
// entering and leaving edit mode never shifts anything else in the bar — only
// whether the space is typable and whether a cursor sits in it.
//
// A plain OutlinedTextField was ruled out: its own box padding and min height
// would make the bar taller the moment editing starts, which is exactly the
// "jump" this is required not to do. BasicTextField has no such chrome, so it
// can share the read state's own text style pixel-for-pixel.
// docs/design-spec-home.md, section 4: 60 characters is the only limit on the title.
private const val TITLE_MAX_LENGTH = 60

@Composable
private fun EditableListTitle(
    title: String,
    onRename: (String) -> Unit,
    initiallyEditing: Boolean = false,
    onEditStarted: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(initiallyEditing) }

    if (isEditing) {
        var fieldValue by remember {
            mutableStateOf(
                if (initiallyEditing) {
                    // A brand-new list opened straight into edit mode: the whole
                    // placeholder title is selected so the first keystroke replaces it
                    // outright, rather than appending after it (docs/design-spec-home.md,
                    // section 4).
                    TextFieldValue(text = title, selection = TextRange(0, title.length))
                } else {
                    // Renaming an existing list: cursor at the end is the least
                    // destructive default — the mock for this state is gone (see
                    // docs/design-spec-turns-8-9.md, section 9) — and the user can still
                    // select all themselves to replace the title outright.
                    TextFieldValue(text = title, selection = TextRange(title.length))
                },
            )
        }
        // Distinguishes "never focused yet" from "focused, then lost it": onFocusChanged
        // fires once immediately on composition with isFocused = false, before the
        // LaunchedEffect below has a chance to actually request focus. Without this,
        // that first callback would read as the user tapping away and close edit mode
        // before it ever visibly opened.
        var hasFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        // Guarded by isEditing so the IME "Done" action and a subsequent focus-loss
        // callback (the field's own node is torn down once isEditing flips to false)
        // can't both fire onRename — this is what makes saving happen exactly once.
        fun commit() {
            if (!isEditing) return
            val trimmed = fieldValue.text.trim()
            // Same rule the tier editor's Label field already enforces on a blank
            // value: nothing is saved. There is no separate Save button to disable
            // here, so a blank title just closes edit mode without calling onRename —
            // the header falls back to showing `title`, which is still whatever it
            // was before, because nothing changed it.
            if (trimmed.isNotEmpty()) {
                onRename(trimmed)
            }
            isEditing = false
        }

        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                // The discard latch flips here, on the very first keystroke, rather
                // than waiting for commit() — a character typed and then deleted back
                // to nothing never calls onRename, but it must still count as touching
                // the list (docs/design-spec-home.md, section 12: "the latch has
                // already flipped").
                //
                // Only a change to the text counts. onValueChange also fires when just
                // the selection moves — tapping into the field, or dragging the caret,
                // with nothing typed — and that is the same kind of non-event as
                // opening the search sheet and adding nothing, which section 12 says
                // explicitly is not a touch.
                if (newValue.text != fieldValue.text) {
                    onEditStarted()
                }
                // A keystroke past the 60-character cap is simply rejected — the field
                // never shows more than the limit (docs/design-spec-home.md, section 4).
                if (newValue.text.length <= TITLE_MAX_LENGTH) {
                    fieldValue = newValue
                }
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hasFocused = true
                    } else if (hasFocused) {
                        commit()
                    }
                }
                .testTag(TierDetailTestTags.HEADER_TITLE),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
        )

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        val editDescription = stringResource(R.string.tier_detail_content_description_edit_title)
        Text(
            text = title,
            modifier = modifier
                .clickable { isEditing = true }
                .semantics { contentDescription = editDescription }
                .testTag(TierDetailTestTags.HEADER_TITLE),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
