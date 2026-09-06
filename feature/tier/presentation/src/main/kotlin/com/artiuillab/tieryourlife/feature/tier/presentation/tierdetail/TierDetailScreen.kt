package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets.AddItemsSheet

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
