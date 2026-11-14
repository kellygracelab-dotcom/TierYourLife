package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.board.play.BoardPlay
import com.artiuillab.tieryourlife.feature.tier.board.play.NoPlay
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AddItemsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BoardPublicationDialogs

@Composable
fun TierDetailScreen(
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit = {},
    onOpenAiStudio: (listTitle: String) -> Unit = {},
    onChooseCover: () -> Unit = {},
    coverChanged: Boolean = false,
    onCoverChangeConsumed: () -> Unit = {},
    onChooseTopic: () -> Unit = {},
    topicChanged: Boolean = false,
    onTopicChangeConsumed: () -> Unit = {},
    /** The published list this board is out as a ranking of, from the credit under the title. */
    onOpenPublishedList: (String) -> Unit = {},
    /** This board's own published list, as others see it. */
    onSeeInCommunity: ((String) -> Unit)? = null,
    addedItemIds: List<Long> = emptyList(),
    onAddedItemConsumed: () -> Unit = {},
    focusTierId: Long? = null,
    /** Play over this board; navigation hands in the real one. */
    play: BoardPlay = NoPlay,
    /** The proposal form, from the end of the category picker. */
    onProposeSection: ((String?) -> Unit)? = null,
    onSignIn: () -> Unit = {},
    viewModel: TierDetailViewModel = hiltViewModel(),
    originalLinks: OriginalLinkViewModel = hiltViewModel(),
) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val publication by viewModel.publicationState.collectAsStateWithLifecycle()
    val state = screen.board
    val publish = screen.publish
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }
    var manualEntryVisible by rememberSaveable { mutableStateOf(false) }
    var playOpen by rememberSaveable { mutableStateOf(false) }
    val playRequest = (state as? TierDetailUiState.Success)?.list?.let { remember(it) { PlayRequest.ownBoard(it) } }

    // The picker is a screen of its own; the board reads its row again when it comes back changed.
    LaunchedEffect(coverChanged) {
        if (coverChanged) {
            viewModel.loadTierList()
            onCoverChangeConsumed()
        }
    }
    LaunchedEffect(topicChanged) {
        if (topicChanged) {
            viewModel.loadTierList()
            onTopicChangeConsumed()
        }
    }

    val loadedList = (state as? TierDetailUiState.Success)?.list
    val originalLink = originalLinkOf(loadedList, originalLinks, onOpenPublishedList)

    TierDetailScreenContent(
        state = state,
        allLists = screen.allLists,
        onOpenList = onOpenList,
        onBoardsBesideWanted = viewModel::wantBoardsBeside,
        addedItemIds = addedItemIds,
        userMessages = viewModel.userMessages,
        belowTopBar = originalLink,
        actions = TierDetailActions(
            onBack = onBack,
            signedIn = screen.signedIn,
            publishing = publish.publishing,
            publishError = publish.error,
            publicPending = publish.publicPending,
            categoryWanted = publish.categoryWanted,
            onSetPublic = viewModel::setPublic,
            onSetAnonymous = viewModel::setAnonymous,
            publishedIsBehind = publish.publishedIsBehind,
            onUpdatePublished = viewModel::updatePublished,
            onSeeInCommunity = onSeeInCommunity,
            onOpenOriginal = loadedList?.original()?.let { id -> { onOpenPublishedList(id) } },
            onSetCategory = viewModel::setCategory,
            onCategoryNotChosen = viewModel::categoryNotChosen,
            onProposeSection = onProposeSection,
            onChooseCover = onChooseCover,
            onChooseTopic = onChooseTopic,
            onAddClick = { addSheetVisible = true },
            onManualAddClick = { manualEntryVisible = true },
            onMoveItem = viewModel::moveItem,
            onReturnAllToPool = viewModel::returnAllToPool,
            onDeleteItem = viewModel::deleteItem,
            onRestoreItem = viewModel::restoreItem,
            onForgetDeletedItem = viewModel::forgetDeletedItem,
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
            focusTierId = focusTierId,
            onPlay = { playOpen = true },
            playSaved = playRequest?.let { play.isSaved(it.key) } == true,
        ),
    )

    BoardPublicationDialogs(
        state = publication,
        signedIn = screen.signedIn,
        onConfirm = viewModel::confirmPublication,
        onDismissPreview = viewModel::dismissPublication,
        onPublic = { viewModel.shareRankingOffer(onSignIn) },
        onDismissOffer = viewModel::dismissRankingOffer,
        onDismissError = viewModel::dismissPublicationError,
        onRecheckOriginal = viewModel::recheckOriginal,
        onSignIn = onSignIn,
    )

    if (playRequest != null) {
        play.Layer(
            request = playRequest,
            open = playOpen,
            // The moves went through the board's own repository; it only has to read itself again.
            onArranged = { _, _ -> viewModel.loadTierList() },
            onClose = { playOpen = false },
        )
    }

    // A board in columns (a short window, or one wide enough for the pool beside it) adds from the side, in view of the pool.
    val windowSize = LocalWindowInfo.current.containerSize
    val columnLayout = with(LocalDensity.current) {
        WindowShape.isShort(windowSize.height.toDp()) || windowSize.width.toDp() >= WindowShape.WideFrom
    }
    if (addSheetVisible) {
        AddItemsSheet(
            side = columnLayout,
            listTitle = (state as? TierDetailUiState.Success)?.list?.title.orEmpty(),
            category = (state as? TierDetailUiState.Success)?.list?.category,
            onDismiss = { addSheetVisible = false },
            onItemsConfirmed = { items ->
                viewModel.addItemsToPool(items)
                addSheetVisible = false
            },
            onUploadInstead = {
                addSheetVisible = false
                manualEntryVisible = true
            },
            onSignIn = onSignIn,
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
