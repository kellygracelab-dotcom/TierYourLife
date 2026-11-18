@file:OptIn(ExperimentalMaterial3Api::class)

package com.artiuillab.tieryourlife.feature.tier.board

import android.content.res.Resources
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.ScrollAwayHeader
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.theme.ui.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.core.theme.ui.ReplacingSnackbar
import com.artiuillab.tieryourlife.core.theme.ui.text
import com.artiuillab.tieryourlife.feature.tier.board.components.BoardIndex
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.FloatingDragTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TrashTarget
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.CardPicking
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.LocalCardPicking
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.LocalTierRowDensity
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.OWN_BAR_BESIDE_PUBLISH
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.OneByOnePanel
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.OwnBoardTopBar
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.POOL_COLUMN_MARGIN
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.PlayButton
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.PoolColumn
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.PoolSheet
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.PublishButton
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.RankedList
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.RankedPoolSection
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.SubtitleLink
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierRow
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierRowDensity
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierScreenTopBar
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.ViewButton
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.boardCounts
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.everyCardPlaced
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.publishingOf
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.rememberOneByOne
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.MoveItemSheet
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierEditorActions
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierEditorSheet
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.fixedInSettings
import com.artiuillab.tieryourlife.feature.tier.board.share.rememberShareBoard
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** 72 of a short window's 300 was a quarter of the board scrolling under the finger; 48 everywhere, speed rising toward the edge. */
private val TIER_LIST_AUTOSCROLL_EDGE = 48.dp
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
fun TierDetailScreenContent(
    allLists: List<TierListSummary> = emptyList(),
    onOpenList: (Long) -> Unit = {},
    /** Asked once the window has room for the column of boards beside this one: [allLists] is read for it only then. */
    onBoardsBesideWanted: () -> Unit = {},
    state: TierDetailUiState,
    actions: TierDetailActions = TierDetailActions(),
    addedItemIds: List<Long> = emptyList(),
    userMessages: Flow<UserMessage> = emptyFlow(),
    readOnly: Boolean = false,
    subtitle: String? = null,
    /** Tapped at the end of [subtitle]: how many others ranked the list, which opens their rankings. */
    subtitleLink: SubtitleLink? = null,
    onReaderMoreClick: (() -> Unit)? = null,
    belowTopBar: (@Composable () -> Unit)? = null,
    poolAccessories: (@Composable (folded: Boolean) -> Unit)? = null,
    /**
     * The same controls as [poolAccessories], for the bar when the pool sits
     * beside the rows or docks under them on a tablet; [BoardBar.Compact] is a
     * short window's single bar, which then also has to say what [belowTopBar]
     * would have said.
     */
    barExtras: (@Composable RowScope.(BoardBar) -> Unit)? = null,
    /** A line under the side or docked pool's title, where [poolAccessories]' sentence goes then. */
    poolNote: (@Composable () -> Unit)? = null,
    /** On a tablet held upright, the line under the title that takes [belowTopBar]'s place: who made it, in which section. */
    titleSubline: (@Composable () -> Unit)? = null,
    /** Under the last row, scrolling with them. */
    footer: (@Composable () -> Unit)? = null,
) {
    // Beside the board once there is room. Held back on a short window too: a
    // phone in landscape is wide enough for the column and too short for the board.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val roomForIndex = !readOnly &&
            currentWindowShape.holdsTwoPanes &&
            maxHeight >= INDEX_NEEDS_HEIGHT
        val wantBoardsBeside by rememberUpdatedState(onBoardsBesideWanted)
        LaunchedEffect(roomForIndex) { if (roomForIndex) wantBoardsBeside() }
        val showsIndex = roomForIndex && allLists.size > 1

        Row(Modifier.fillMaxSize()) {
            if (showsIndex) {
                BoardIndex(
                    lists = allLists,
                    selectedId = (state as? TierDetailUiState.Success)?.list?.id ?: -1L,
                    onSelect = onOpenList,
                )
            }
            TierDetailPane(
                state = state,
                actions = actions,
                addedItemIds = addedItemIds,
                userMessages = userMessages,
                readOnly = readOnly,
                subtitle = subtitle,
                subtitleLink = subtitleLink,
                onReaderMoreClick = onReaderMoreClick,
                belowTopBar = belowTopBar,
                poolAccessories = poolAccessories,
                barExtras = barExtras,
                poolNote = poolNote,
                titleSubline = titleSubline,
                footer = footer,
                indexBeside = showsIndex,
            )
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
    subtitleLink: SubtitleLink?,
    onReaderMoreClick: (() -> Unit)?,
    belowTopBar: (@Composable () -> Unit)?,
    poolAccessories: (@Composable (folded: Boolean) -> Unit)?,
    barExtras: (@Composable RowScope.(BoardBar) -> Unit)?,
    poolNote: (@Composable () -> Unit)?,
    titleSubline: (@Composable () -> Unit)?,
    footer: (@Composable () -> Unit)?,
    indexBeside: Boolean,
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

            is TierDetailUiState.Success -> BoardTips(state.list, actions, readOnly) { heard ->
                TierScreenBody(
                    list = state.list,
                    actions = heard,
                    addedItemIds = addedItemIds,
                    userMessages = userMessages,
                    readOnly = readOnly,
                    subtitle = subtitle,
                    subtitleLink = subtitleLink,
                    onReaderMoreClick = onReaderMoreClick,
                    belowTopBar = belowTopBar,
                    poolAccessories = poolAccessories,
                    barExtras = barExtras,
                    poolNote = poolNote,
                    titleSubline = titleSubline,
                    footer = footer,
                    indexBeside = indexBeside,
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

/** "Published", "Updated" or "In Rankings"; with pictures left out, the sentence that says so, which begins the same. */
private fun UserMessage.Published.text(resources: Resources): String = when {
    droppedPictures > 0 -> UserMessage.PicturesDropped(droppedPictures).text(resources)
    ranking -> resources.getString(R.string.board_ranked_short)
    updated -> resources.getString(R.string.board_updated_short)
    else -> resources.getString(R.string.board_published_short)
}

@Composable
private fun TierScreenBody(
    list: TierList,
    actions: TierDetailActions,
    addedItemIds: List<Long> = emptyList(),
    userMessages: Flow<UserMessage> = emptyFlow(),
    readOnly: Boolean = false,
    subtitle: String? = null,
    subtitleLink: SubtitleLink? = null,
    onReaderMoreClick: (() -> Unit)? = null,
    belowTopBar: (@Composable () -> Unit)? = null,
    poolAccessories: (@Composable (folded: Boolean) -> Unit)? = null,
    barExtras: (@Composable RowScope.(BoardBar) -> Unit)? = null,
    poolNote: (@Composable () -> Unit)? = null,
    titleSubline: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    indexBeside: Boolean = false,
) {
    val onBack = actions.onBack
    val onAddClick = actions.onAddClick
    val onManualAddClick = actions.onManualAddClick
    val onMoveItem = actions.onMoveItem
    val onDeleteItem = actions.onDeleteItem
    val onRestoreItem = actions.onRestoreItem
    val onForgetDeletedItem = actions.onForgetDeletedItem
    val onReorderTiers = actions.onReorderTiers
    val onDeleteTierToPool = actions.onDeleteTierToPool
    val onRestoreTier = actions.onRestoreTier
    val onAddTier = actions.onAddTier
    val onEditTier = actions.onEditTier
    val onSetDisplayMode = actions.onSetDisplayMode
    val onRenameList = actions.onRenameList
    val onGenerateClick = { actions.onOpenAiStudio(list.title) }
    var listSettingsVisible by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = listSettingsVisible) { listSettingsVisible = false }
    // A publish from the bar that needs a section asks for it where it is chosen.
    LaunchedEffect(actions.categoryWanted) { if (actions.categoryWanted) listSettingsVisible = true }
    // So does a refusal put right there: the Publication row says why.
    LaunchedEffect(actions.publishError) { if (actions.publishError?.fixedInSettings() == true) listSettingsVisible = true }

    val share = rememberShareBoard(actions.ownName)

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
            onPublish = actions.onPublish,
            onSetCategory = actions.onSetCategory,
            onCategoryNotChosen = actions.onCategoryNotChosen,
            onProposeSection = actions.onProposeSection,
            onChooseCover = actions.onChooseCover,
            onAdjustCover = actions.onAdjustCover,
            onChooseTopic = actions.onChooseTopic,
            onAddCards = {
                listSettingsVisible = false
                actions.onAddClick?.invoke()
            },
            onShare = { share(list) },
            onReturnAllToPool = actions.onReturnAllToPool,
            onSeeInCommunity = actions.onSeeInCommunity?.let { open -> list.publishedId?.let { id -> { open(id) } } },
            onOpenOriginal = actions.onOpenOriginal,
        )
        return
    }

    val rankedTiers = list.tiers.filterNot { it.isPool }
    val pool = list.tiers.firstOrNull { it.isPool }
    val layoutDirection = LocalLayoutDirection.current
    val dragController = remember { TierDragController() }
    var chooserItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    val onCardLifted by rememberUpdatedState(actions.onCardLifted)
    LaunchedEffect(dragController) {
        snapshotFlow { dragController.draggedPayload != null }.collect { lifted -> onCardLifted(lifted) }
    }
    var editingTierId by rememberSaveable { mutableStateOf<Long?>(null) }

    SideEffect {
        dragController.setValidTargets(
            tierIds = list.tiers.map { it.id },
            itemIds = list.tiers.flatMap { it.items }.map { it.id },
            rightToLeft = layoutDirection == LayoutDirection.Rtl,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val snackbar = remember(snackbarHostState, coroutineScope) { ReplacingSnackbar(snackbarHostState, coroutineScope) }
    val resources = LocalResources.current
    val deletedMessageTemplate = stringResource(R.string.tier_detail_item_removed)
    val undoLabel = stringResource(R.string.tier_detail_snackbar_undo)
    val actionFailedMessage = stringResource(ThemeR.string.snack_action_failed)
    val underReviewMessage = stringResource(ThemeR.string.snack_published_under_review)
    val viewLabel = stringResource(R.string.board_published_view)
    // What others see: the list in Community, or the Rankings of the list this one answers.
    val viewPublished by rememberUpdatedState(
        if (list.rankingOf != null) actions.onOpenOriginal else actions.onSeeInCommunity?.let { open -> list.publishedId?.let { id -> { open(id) } } },
    )

    LaunchedEffect(Unit) {
        userMessages.collect { message ->
            if (message is UserMessage.Published) {
                // The new id may reach the board a frame after the message: the tap reads it, not this.
                val canView = viewPublished != null || actions.onSeeInCommunity != null
                snackbar.show(message.text(resources), viewLabel.takeIf { canView }) {
                    if (it == SnackbarResult.ActionPerformed) viewPublished?.invoke()
                }
                return@collect
            }
            snackbar.show(
                when (message) {
                    UserMessage.PublishedUnderReview -> underReviewMessage
                    is UserMessage.PicturesDropped -> message.text(resources)
                    else -> actionFailedMessage
                },
            )
        }
    }
    val onConsumeAddedItem = actions.onConsumeAddedItem
    val onUndoAddedItem = actions.onUndoAddedItem

    LaunchedEffect(addedItemIds) {
        val itemIds = addedItemIds
        if (itemIds.isEmpty()) return@LaunchedEffect
        onConsumeAddedItem()
        val message = resources.getQuantityString(R.plurals.tier_detail_items_added_to_pool, itemIds.size, itemIds.size)
        snackbar.show(message, undoLabel) { if (it == SnackbarResult.ActionPerformed) onUndoAddedItem(itemIds) }
    }

    val deleteAndAnnounce: (Long) -> Unit = { itemId ->
        val title = list.tiers.flatMap { it.items }.firstOrNull { it.id == itemId }?.title.orEmpty()
        onDeleteItem(itemId)
        snackbar.show(String.format(deletedMessageTemplate, title), undoLabel) { result ->
            if (result == SnackbarResult.ActionPerformed) onRestoreItem(itemId) else onForgetDeletedItem(itemId)
        }
    }

    val tierDeletedMessageTemplate = stringResource(R.string.tier_detail_tier_removed)

    val deleteTierAndAnnounce: (Long) -> Unit = { tierId ->
        val tier = rankedTiers.firstOrNull { it.id == tierId }
        val poolId = pool?.id
        if (tier != null && poolId != null) {
            val position = rankedTiers.indexOfFirst { it.id == tierId }
            val itemIds = tier.items.map { it.id }
            onDeleteTierToPool(tierId)
            snackbar.show(String.format(tierDeletedMessageTemplate, tier.label), undoLabel) { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    onRestoreTier(tier.label, tier.caption, tier.colorLight, tier.colorDark, position, itemIds)
                }
            }
        }
    }

    val density = LocalDensity.current

    val tierListState = rememberLazyListState()
    ScrollToTierOnce(tierListState, rankedTiers, actions.focusTierId)
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
        // Height decides what stays pinned, width where the pool goes. A cover
        // shows the board and nothing to work it with. A short window keeps one
        // bar, which leaves with the rows, and lowers the rows; a wide window
        // held sideways gives the pool width instead of the rows' height.
        val glanceable = currentWindowShape.isGlanceable
        val short = WindowShape.isShort(maxHeight)
        val poolBeside = !glanceable && (short || (maxWidth >= WindowShape.WideFrom && maxWidth > maxHeight))
        // A tablet held upright: the pool docks under the rows, the bar grows and takes the author line in.
        val docked = !glanceable && !short && !poolBeside && maxWidth >= WindowShape.MediumFrom
        val readerSubline = titleSubline?.takeIf { docked }
        val barMode = when {
            short -> BoardBar.Compact
            poolBeside -> BoardBar.Beside
            docked -> BoardBar.Docked
            else -> null
        }
        val rowDensity = TierRowDensity.of(short, poolBeside, maxWidth, indexBeside)
        val layoutMemory = LocalBoardLayoutMemory.current
        val oneByOne = rememberOneByOne(onMoveItem)
        val oneByOneOffered = pool != null && !readOnly && !glanceable && list.displayMode != TierListDisplayMode.FLAT_RANKED
        val oneByOneOn = oneByOneOffered && layoutMemory.oneCardAtATime
        val picking = remember(oneByOneOn, oneByOne.selectedId) {
            if (oneByOneOn) CardPicking(oneByOne.selectedId, oneByOne::pick) else null
        }
        val oneByOnePanel: (@Composable (Boolean) -> Unit)? = if (oneByOneOffered) {
            { beside ->
                OneByOnePanel(
                    list,
                    oneByOne,
                    onExit = { layoutMemory.oneCardAtATime = false },
                    beside = beside,
                    // The phone's tray: neither docked under the rows nor in the column beside them.
                    strip = beside && !docked && !poolBeside,
                    column = !beside && poolBeside && short,
                )
            }
        } else {
            null
        }

        // A board on a phone held upright, one's own or a reader's: the bar in two storeys, the lower folding as the rows scroll.
        val ownTwoStorey = barMode == null && !glanceable && (onReaderMoreClick != null || !readOnly)
        val ownBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val playButton = actions.onPlay?.takeUnless { glanceable }?.let { onPlay ->
            PlayButton(cards = list.tiers.sumOf { it.items.size }, saved = actions.playSaved, onClick = onPlay)
        }
        val ownView = ViewButton(list.displayMode, onSetDisplayMode).takeIf { actions.onPublish != null && !glanceable && !readOnly }
        val bar: @Composable () -> Unit = if (ownTwoStorey) {
            {
                val reader = onReaderMoreClick
                OwnBoardTopBar(
                    title = list.title,
                    counts = if (reader != null) subtitle.orEmpty() else boardCounts(list),
                    countsLink = subtitleLink.takeIf { reader != null },
                    state = ownBarScroll.state,
                    onBack = onBack,
                    onManualAdd = onManualAddClick.takeIf { reader == null },
                    onMoreClick = reader ?: { listSettingsVisible = true },
                    onRenameList = onRenameList.takeIf { reader == null },
                    play = playButton,
                    publish = publishButton(list, actions, narrow = maxWidth < WindowShape.MediumFrom, room = maxWidth - OWN_BAR_BESIDE_PUBLISH)
                        ?.takeUnless { reader != null && readOnly },
                    view = ownView,
                    moreTag = TierDetailTestTags.MORE.takeIf { reader != null },
                )
            }
        } else {
            {
            TierScreenTopBar(
                title = list.title,
                onBack = onBack,
                onManualAdd = onManualAddClick,
                onMoreClick = { listSettingsVisible = true },
                onRenameList = onRenameList,
                titleEditable = !readOnly && !glanceable,
                readOnly = readOnly || glanceable,
                subtitle = subtitle,
                subtitleLink = subtitleLink,
                onReaderMoreClick = onReaderMoreClick.takeUnless { glanceable },
                compact = short,
                trailing = barMode?.let { mode -> barExtras?.let { extras -> { extras(mode) } } },
                insetStatusBar = !short,
                spanWindow = poolBeside,
                barHeight = when {
                    !docked -> null
                    readerSubline != null -> DOCKED_READER_BAR_HEIGHT
                    else -> DOCKED_BAR_HEIGHT
                },
                subline = readerSubline,
                play = playButton,
                publish = publishButton(list, actions, narrow = maxWidth < WindowShape.MediumFrom, room = maxWidth - BAR_BESIDE_PUBLISH)?.takeUnless { glanceable || readOnly },
                // One's own board only: a reader's page keeps the author's choice.
                view = ownView,
            )
            }
        }
        val board: @Composable (Modifier) -> Unit = { boardModifier ->
            Row(boardModifier) {
            CenteredContent(max = ContentWidth.Board, modifier = Modifier.weight(1f)) {

                if (list.displayMode == TierListDisplayMode.FLAT_RANKED) {

                    RankedList(
                        rankedTiers = rankedTiers,
                        onSelect = { itemId -> chooserItemId = itemId },
                        modifier = Modifier.weight(1f),
                        footer = footer,
                    )

                    if (pool != null && !glanceable) {

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
                        // The last tier clears the navigation bar unless the pool lies under the rows and does.
                        contentPadding = if (pool != null && !poolBeside && !glanceable) {
                            PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        } else {
                            systemBarsContentPadding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                        },
                        verticalArrangement = Arrangement.spacedBy(LocalTierRowDensity.current.rowGap),
                    ) {

                        itemsIndexed(displayedTiers, key = { _, tier -> tier.id }) { index, tier ->

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
                                poolHasCards = pool?.items?.isNotEmpty() == true,
                                listState = tierListState,
                                itemIndex = index,
                            )
                        }

                        if (footer != null) {
                            item { footer() }
                        }
                    }

                    if (pool != null && !poolBeside && !glanceable) {

                        PoolSheet(
                            pool = pool,
                            onAddClick = onAddClick,
                            onGenerateClick = onGenerateClick,
                            dragController = dragController,
                            onMoveItem = onMoveItem,
                            onDeleteItem = deleteAndAnnounce,
                            onDoubleTap = { itemId -> chooserItemId = itemId },
                            readOnly = readOnly,
                            accessories = poolAccessories.takeUnless { docked },
                            docked = docked,
                            note = poolNote.takeIf { docked },
                            side = actions.poolSide,
                            countWhenEmpty = actions.emptyPoolCounted,
                            oneByOnePanel = oneByOnePanel,
                            countsCardsLeft = actions.poolCountsCardsLeft,
                        )
                    }
                }
            }

                if (pool != null && poolBeside && list.displayMode != TierListDisplayMode.FLAT_RANKED) {
                    PoolColumn(
                        pool = pool,
                        onAddClick = onAddClick,
                        onGenerateClick = onGenerateClick,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = deleteAndAnnounce,
                        onDoubleTap = { itemId -> chooserItemId = itemId },
                        readOnly = readOnly,
                        roomy = !short,
                        indexBeside = indexBeside,
                        note = poolNote,
                        side = actions.poolSide,
                        oneByOnePanel = oneByOnePanel,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(POOL_COLUMN_MARGIN),
                    )
                }
            }
        }

        CompositionLocalProvider(LocalTierRowDensity provides rowDensity, LocalCardPicking provides picking) {
            if (short) {
                // The inset stays with the screen; the bar slides under it and back.
                ScrollAwayHeader(header = bar, modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    board(Modifier.fillMaxSize())
                }
            } else {
                Column(Modifier.fillMaxSize().then(if (ownTwoStorey) Modifier.nestedScroll(ownBarScroll.nestedScrollConnection) else Modifier)) {
                    bar()
                    // Under the bar, never over it: the bar carries the status-bar
                    // inset, so anything placed above it sits beneath the clock.
                    // A tablet's taller bar has already said it.
                    if (readerSubline == null) belowTopBar?.invoke()
                    board(Modifier.weight(1f))
                }
            }
        }

        // A pool beside the rows or docked under them carries the trash in its
        // head while a card is lifted; only the phone's strip needs it floating.
        val trashInPool = pool != null && (poolBeside || docked) && list.displayMode != TierListDisplayMode.FLAT_RANKED
        if (dragController.isDragging || dragController.returning != null) {
            if (!readOnly && !trashInPool && dragController.isDragging) {
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

        val poolTop = pool?.id?.takeUnless { poolBeside }?.let { dragController.tierBounds(it)?.top }

        RowChangeTick(dragController)
        val bottomGap = if (poolTop != null) {
            (maxHeight - with(density) { poolTop.toDp() }) + 16.dp
        } else {
            16.dp
        }

        DeletedItemSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (poolTop == null) Modifier.navigationBarsPadding() else Modifier)
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
            onAdjustPicture = actions.onAdjustPicture?.let { adjust ->
                {
                    adjust(chosenId)
                    chooserItemId = null
                }
            },
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
            actions = tierEditorActions(
                rankedTierIds = rankedTiers.map { it.id },
                tierId = editingTier.id,
                onReorderTiers = onReorderTiers,
                onRemove = {
                    editingTierId = null
                    deleteTierAndAnnounce(editingTier.id)
                },
            ),
        )
    }
}

/** Moves one place up or down, as dragging the band does; the ends have no move past them. */
internal fun tierEditorActions(
    rankedTierIds: List<Long>,
    tierId: Long,
    onReorderTiers: (List<Long>) -> Unit,
    onRemove: () -> Unit,
): TierEditorActions {
    val at = rankedTierIds.indexOf(tierId)
    fun moveTo(to: Int): (() -> Unit)? = if (at < 0 || to !in rankedTierIds.indices) {
        null
    } else {
        { onReorderTiers(rankedTierIds.toMutableList().apply { add(to, removeAt(at)) }) }
    }
    return TierEditorActions(onMoveUp = moveTo(at - 1), onMoveDown = moveTo(at + 1), onRemove = onRemove)
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

private val DOCKED_BAR_HEIGHT = 64.dp
private val DOCKED_READER_BAR_HEIGHT = 72.dp

/** Brings [focusTierId]'s row into view the first time the board has it, and never again: after that the person scrolls. */
@Composable
private fun ScrollToTierOnce(state: LazyListState, tiers: List<Tier>, focusTierId: Long?) {
    var done by rememberSaveable(focusTierId) { mutableStateOf(focusTierId == null) }
    val index = tiers.indexOfFirst { it.id == focusTierId }
    LaunchedEffect(index, done) {
        if (!done && index >= 0) {
            state.scrollToItem(index)
            done = true
        }
    }
}

/** A tick under the finger each time a lifted card crosses into another row; the lift itself already had one. */
@Composable
private fun RowChangeTick(dragController: TierDragController) {
    val haptic = LocalHapticFeedback.current
    val last = remember { arrayOfNulls<Long>(1) }
    val hovered = dragController.hoveredTierId
    val lifted = dragController.draggedPayload != null
    LaunchedEffect(hovered, lifted) {
        val before = last[0]
        val crossed = before != null && before != hovered
        if (lifted && hovered != null && crossed) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        last[0] = if (lifted) hovered else null
    }
}

/** Back, Play and the overflow, the bar's padding, and a few letters of the title. */
private val BAR_BESIDE_PUBLISH = 48.dp * 3 + 16.dp + 56.dp

private fun publishButton(list: TierList, actions: TierDetailActions, narrow: Boolean, room: Dp): PublishButton? {
    val onPublish = actions.onPublish ?: return null
    val kind = publishingOf(list, actions.publishedIsBehind) ?: return null
    return PublishButton(
        kind = kind,
        primary = everyCardPlaced(list),
        narrow = narrow,
        room = room,
        busy = actions.publishing,
        onClick = { onPublish(kind) },
    )
}
