package com.artiuillab.tieryourlife.feature.tier.board

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode

/** Public because [TierDetailScreenContent] is, and this is its default. */
data class TierDetailActions(
    val onBack: () -> Unit = {},
    val signedIn: Boolean = false,
    val publishing: Boolean = false,
    val publishError: PublishError? = null,
    val categoryWanted: Boolean = false,
    val publicPending: Boolean? = null,
    val onSetPublic: (Boolean) -> Unit = {},
    val onSetAnonymous: (Boolean) -> Unit = {},
    /** True only where the published copy is known to differ from the board. */
    val publishedIsBehind: Boolean = false,
    val onUpdatePublished: () -> Unit = {},
    /** A section id. */
    val onSetCategory: (String) -> Unit = {},
    val onCategoryNotChosen: () -> Unit = {},
    /** The proposal form, with the board's section to file lists under meanwhile; null where it cannot open. */
    val onProposeSection: ((String?) -> Unit)? = null,
    val onChooseCover: () -> Unit = {},
    val onChooseTopic: () -> Unit = {},
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
    /** The tier the board opens on, once: the one the cover was showing when the phone was opened. */
    val focusTierId: Long? = null,
    /** Null where there is no Play: the button is left out of the bar then. */
    val onPlay: (() -> Unit)? = null,
    /** A game of this board is put down: the button carries a dot. */
    val playSaved: Boolean = false,
)
