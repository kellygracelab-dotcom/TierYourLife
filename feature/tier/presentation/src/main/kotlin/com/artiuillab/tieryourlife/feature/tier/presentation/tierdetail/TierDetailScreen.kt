package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.share.BoardPalette
import com.artiuillab.tieryourlife.feature.tier.presentation.share.SHARE_LINK
import com.artiuillab.tieryourlife.feature.tier.presentation.share.shareBoard
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BoardIndex
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.FloatingDragTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TIER_LIST_ITEM_SPACING
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TrashTarget
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.PoolPanel
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.RankedList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.RankedPoolSection
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.TierRow
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.TierScreenTopBar
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.captionsExcept
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows.rememberBandContentWidth
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets.AddItemsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets.MoveItemSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets.TierEditorSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets.TierListSettingsScreenContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    onOpenList: (Long) -> Unit = {},
    onOpenAiStudio: (listTitle: String) -> Unit = {},
    addedItemIds: List<Long> = emptyList(),
    onAddedItemConsumed: () -> Unit = {},
    viewModel: TierDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val publishing by viewModel.publishing.collectAsStateWithLifecycle()
    val publishError by viewModel.publishError.collectAsStateWithLifecycle()
    val publishedIsBehind by viewModel.publishedIsBehind.collectAsStateWithLifecycle()
    val publicPending by viewModel.publicPending.collectAsStateWithLifecycle()
    val categoryWanted by viewModel.categoryWanted.collectAsStateWithLifecycle()
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }
    var manualEntryVisible by rememberSaveable { mutableStateOf(false) }

    TierDetailScreenContent(
        state = state,
        allLists = allLists,
        onOpenList = onOpenList,
        addedItemIds = addedItemIds,
        userMessages = viewModel.userMessages,
        actions = TierDetailActions(
            onBack = onBack,
            signedIn = signedIn,
            publishing = publishing,
            publishError = publishError,
            publicPending = publicPending,
            categoryWanted = categoryWanted,
            onSetPublic = viewModel::setPublic,
            publishedIsBehind = publishedIsBehind,
            onUpdatePublished = viewModel::updatePublished,
            onSetCategory = viewModel::setCategory,
            onCategoryNotChosen = viewModel::categoryNotChosen,
            onSetCover = viewModel::setCoverImageUrl,
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
            onOpenAiStudio = onOpenAiStudio,
            onConsumeAddedItem = {
                onAddedItemConsumed()
                viewModel.loadTierList()
            },
            onUndoAddedItem = viewModel::removeAddedItems,
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
fun TierDetailScreenContent(
    allLists: List<TierList> = emptyList(),
    onOpenList: (Long) -> Unit = {},
    state: TierDetailUiState,
    actions: TierDetailActions = TierDetailActions(),
    addedItemIds: List<Long> = emptyList(),
    userMessages: Flow<UserMessage> = emptyFlow(),
    readOnly: Boolean = false,
    subtitle: String? = null,
    onReaderMoreClick: (() -> Unit)? = null,
    belowTopBar: (@Composable () -> Unit)? = null,
) {
    // Beside the board once there is room. Held back on a short window too: a
    // phone in landscape is wide enough for the column and too short for the board.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val showsIndex = !readOnly &&
            currentWindowShape.holdsTwoPanes &&
            maxHeight >= INDEX_NEEDS_HEIGHT &&
            allLists.size > 1

        Row(Modifier.fillMaxSize()) {
            if (showsIndex) {
                BoardIndex(
                    lists = allLists,
                    selectedId = (state as? TierDetailUiState.Success)?.list?.id ?: -1L,
                    onSelect = onOpenList,
                )
            }
            TierDetailPane(state, actions, addedItemIds, userMessages, readOnly, subtitle, onReaderMoreClick, belowTopBar)
        }
    }
}

/** A phone turned sideways clears the width and has 360dp of height left, which is a board nobody can rank in. */
private val INDEX_NEEDS_HEIGHT = 480.dp

@Composable
private fun TierDetailPane(
    state: TierDetailUiState,
    actions: TierDetailActions,
    addedItemIds: List<Long>,
    userMessages: Flow<UserMessage>,
    readOnly: Boolean,
    subtitle: String?,
    onReaderMoreClick: (() -> Unit)?,
    belowTopBar: (@Composable () -> Unit)?,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            is TierDetailUiState.Loading -> {
                var spinnerVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(LOADING_SPINNER_DELAY_MILLIS)
                    spinnerVisible = true
                }

                // No top bar with an empty title: on a list just created it
                // appeared, then filled in, which is the flicker.
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

            is TierDetailUiState.Success -> {
                TierScreenBody(
                    list = state.list,
                    actions = actions,
                    addedItemIds = addedItemIds,
                    userMessages = userMessages,
                    readOnly = readOnly,
                    subtitle = subtitle,
                    onReaderMoreClick = onReaderMoreClick,
                    belowTopBar = belowTopBar,
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
    addedItemIds: List<Long> = emptyList(),
    userMessages: Flow<UserMessage> = emptyFlow(),
    readOnly: Boolean = false,
    subtitle: String? = null,
    onReaderMoreClick: (() -> Unit)? = null,
    belowTopBar: (@Composable () -> Unit)? = null,
) {
    val onBack = actions.onBack
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
    val onGenerateClick = { actions.onOpenAiStudio(list.title) }
    var listSettingsVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = listSettingsVisible) { listSettingsVisible = false }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val media = TierYourLifeMedia.current
    val palette = BoardPalette(
        surface = MaterialTheme.colorScheme.surface,
        onSurface = MaterialTheme.colorScheme.onSurface,
        onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
        outlineVariant = MaterialTheme.colorScheme.outlineVariant,
        onBand = media.onTierBand,
        unrankedBand = media.unrankedRibbon,
        isDark = media.isDark,
    )
    val shareCaption = stringResource(R.string.share_caption, list.title, SHARE_LINK)
    val shareFooter = stringResource(R.string.share_footer)

    if (listSettingsVisible) {
        TierListSettingsScreenContent(
            list = list,
            onBack = { listSettingsVisible = false },
            onAddTier = onAddTier,
            onSetDisplayMode = onSetDisplayMode,
            signedIn = actions.signedIn,
            publishing = actions.publishing,
            publishError = actions.publishError,
            publicPending = actions.publicPending,
            categoryWanted = actions.categoryWanted,
            onSetPublic = actions.onSetPublic,
            publishedIsBehind = actions.publishedIsBehind,
            onUpdatePublished = actions.onUpdatePublished,
            onSetCategory = actions.onSetCategory,
            onCategoryNotChosen = actions.onCategoryNotChosen,
            onSetCover = actions.onSetCover,
            onAddCards = {
                listSettingsVisible = false
                actions.onAddClick()
            },
            onShare = { scope.launch { shareBoard(context, list, palette, shareCaption, shareFooter) } },
        )
        return
    }

    val rankedTiers = list.tiers.filterNot { it.isPool }
    val bandContentWidth = rememberBandContentWidth(list.tiers)
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
    val resources = LocalResources.current
    val deletedMessageTemplate = stringResource(R.string.tier_detail_item_moved_to_trash)
    val undoLabel = stringResource(R.string.tier_detail_snackbar_undo)
    val actionFailedMessage = stringResource(R.string.snack_action_failed)

    LaunchedEffect(Unit) {
        userMessages.collect { snackbarHostState.showSnackbar(actionFailedMessage) }
    }
    val onConsumeAddedItem = actions.onConsumeAddedItem
    val onUndoAddedItem = actions.onUndoAddedItem
    var pendingAddedItemIds by remember { mutableStateOf(emptyList<Long>()) }

    LaunchedEffect(addedItemIds) {
        if (addedItemIds.isEmpty()) return@LaunchedEffect
        pendingAddedItemIds = addedItemIds
        onConsumeAddedItem()
    }

    LaunchedEffect(pendingAddedItemIds) {
        val itemIds = pendingAddedItemIds
        if (itemIds.isEmpty()) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val message = resources.getQuantityString(R.plurals.tier_detail_items_added_to_pool, itemIds.size, itemIds.size)
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        pendingAddedItemIds = emptyList()
        if (result == SnackbarResult.ActionPerformed) {
            onUndoAddedItem(itemIds)
        }
    }

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
                titleEditable = !readOnly,
                readOnly = readOnly,
                subtitle = subtitle,
                onReaderMoreClick = onReaderMoreClick,
            )

            // Under the bar, never over it: the bar carries the status-bar
            // inset, so anything placed above it sits beneath the clock.
            belowTopBar?.invoke()

            CenteredContent(max = ContentWidth.Board, modifier = Modifier.weight(1f)) {
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
                            onGenerateClick = onGenerateClick,
                            onSelect = { itemId -> chooserItemId = itemId },
                            readOnly = readOnly,
                        )
                    }
                } else {
                    val visualTierOrder = dragController.visualTierOrder
                    val displayedTiers = if ((dragController.isDraggingTier || dragController.isSettlingTier) && visualTierOrder.isNotEmpty()) {
                        val indexById = visualTierOrder.withIndex().associate { (index, id) -> id to index }
                        rankedTiers.sortedBy { indexById[it.id] ?: Int.MAX_VALUE }
                    } else {
                        rankedTiers
                    }

                    LazyColumn(
                        state = tierListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag(TierDetailTestTags.TIER_LIST)
                            .onGloballyPositioned { coordinates -> tierListBounds = coordinates.boundsInRoot() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(TIER_LIST_ITEM_SPACING),
                    ) {
                        items(displayedTiers, key = { it.id }) { tier ->
                            val itemModifier = when (tier.id) {
                                dragController.draggedTierId -> {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer {
                                            translationY = dragController.draggedTierOffsetYPx
                                            scaleX = 1.02f
                                            scaleY = 1.02f
                                        }
                                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                                }

                                dragController.settlingTierId -> {
                                    val settleOffset = remember(dragController.settlingTierId) {
                                        Animatable(dragController.draggedTierOffsetYPx)
                                    }
                                    LaunchedEffect(dragController.settlingTierId) {
                                        settleOffset.animateTo(0f, tween(durationMillis = 150))
                                        dragController.finishSettling()
                                    }
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer { translationY = settleOffset.value }
                                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                                }

                                else -> Modifier.animateItem()
                            }
                            TierRow(
                                tier = tier,
                                bandContentWidth = bandContentWidth,
                                modifier = itemModifier,
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
                            onGenerateClick = onGenerateClick,
                            dragController = dragController,
                            onMoveItem = onMoveItem,
                            onDeleteItem = deleteAndAnnounce,
                            onDoubleTap = { itemId -> chooserItemId = itemId },
                            readOnly = readOnly,
                        )
                    }
                }
            }
        }

        if (dragController.isDragging) {
            if (!readOnly) {
                // The trash sits at the edge of the board a finger is dragging
                // over, not at the edge of the window it is floating in.
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxHeight()
                        .atMost(ContentWidth.Board),
                ) {
                    TrashTarget(
                        dragController = dragController,
                        poolTierId = pool?.id,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
            FloatingDragTile(dragController)
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
            otherCaptions = list.captionsExcept(editingTier.id),
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
