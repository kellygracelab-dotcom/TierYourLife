package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

interface TierRepository {

    suspend fun getTierListById(id: Long): TierList?

    suspend fun getAllTierLists(): List<TierList>

    suspend fun createTierList(title: String): Long

    suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode)

    suspend fun renameTierList(id: Long, title: String)

    suspend fun addMovieToPool(tierListId: Long, title: String, imageUrl: String?): Long

    suspend fun addMoviesToPool(tierListId: Long, movies: List<PoolMovieDraft>)

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

    // No-ops if the target is the list's pool: exactly one pool must always remain.
    suspend fun deleteTier(id: Long)

    // Accepts the whole final order at once so no intermediate state has duplicate positions.
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