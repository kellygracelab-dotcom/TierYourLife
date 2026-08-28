package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode

internal data class TierDetailActions(
    val onBack: () -> Unit = {},
    val onAddClick: () -> Unit = {},
    val onManualAddClick: () -> Unit = {},
    val onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
    val onDeleteItem: (itemId: Long) -> Unit = {},
    val onRestoreItem: (itemId: Long) -> Unit = {},
    val onReorderTiers: (orderedTierIds: List<Long>) -> Unit = {},
    val onDeleteTierToPool: (tierId: Long) -> Unit = {},
    val onRestoreTier: (
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) -> Unit = { _, _, _, _, _, _ -> },
    val onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit = { _, _, _, _ -> },
    val onEditTier: (id: Long, label: String, caption: String?, colorLight: String, colorDark: String) -> Unit =
        { _, _, _, _, _ -> },
    val onSetDisplayMode: (displayMode: TierListDisplayMode) -> Unit = {},
    val onRenameList: (title: String) -> Unit = {},
    val onOpenAiStudio: (listTitle: String) -> Unit = {},
    val onConsumeAddedItem: () -> Unit = {},
    val onUndoAddedItem: (itemIds: List<Long>) -> Unit = {},
)
