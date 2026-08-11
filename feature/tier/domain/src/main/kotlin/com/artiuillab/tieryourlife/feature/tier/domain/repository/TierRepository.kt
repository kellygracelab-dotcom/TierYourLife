package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

interface TierRepository {

    suspend fun getTierListById(id: Long): TierList?

    suspend fun getAllTierLists(): List<TierList>

    suspend fun createTierList(title: String): Long

    suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode)

    suspend fun renameTierList(id: Long, title: String)

    suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource = TierItemSource.MANUAL,
    ): Long

    suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>)

    suspend fun attachImageToItem(itemId: Long, sourceUri: String)

    suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int)

    suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long

    suspend fun renameTier(id: Long, label: String, caption: String?)

    suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String)

    suspend fun deleteTierToPool(id: Long)

    suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    )

    suspend fun reorderTiers(orderedTierIds: List<Long>)

    suspend fun deleteTierLists(ids: List<Long>)

    suspend fun restoreTierLists(ids: List<Long>)

    suspend fun deleteTierItem(id: Long)

    suspend fun restoreTierItem(id: Long)

    suspend fun deleteTierListPermanently(id: Long)

    suspend fun deleteTierItemPermanently(id: Long)

    suspend fun emptyTrash()

    suspend fun getTrashEntries(): List<TrashEntry>
}
