package com.artiuillab.tieryourlife.feature.tier.presentation.community

import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository

internal data class SavedTemplate(
    val title: String,
    val authorName: String,
    val tiers: List<Tier>,
    val items: List<TierItem>,
)

internal class FakeTierRepositoryForCommunity : TierRepository {

    val templates = mutableListOf<SavedTemplate>()

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
    ): Long {
        templates += SavedTemplate(title, authorName, tiers, items)
        return 7L
    }

    override suspend fun setPublishedId(id: Long, publishedId: String?) = Unit

    override suspend fun getTierListById(id: Long): TierList? = null
    override suspend fun getAllTierLists(): List<TierList> = emptyList()
    override suspend fun createTierList(title: String): Long = 0
    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = Unit
    override suspend fun renameTierList(id: Long, title: String) = Unit
    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long = 0
    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = Unit
    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) = Unit
    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) = Unit
    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long = 0
    override suspend fun renameTier(id: Long, label: String, caption: String?) = Unit
    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) = Unit
    override suspend fun deleteTierToPool(id: Long) = Unit
    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) = Unit
    override suspend fun reorderTiers(orderedTierIds: List<Long>) = Unit
    override suspend fun deleteTierLists(ids: List<Long>) = Unit
    override suspend fun restoreTierLists(ids: List<Long>) = Unit
    override suspend fun deleteTierItem(id: Long) = Unit
    override suspend fun restoreTierItem(id: Long) = Unit
    override suspend fun deleteTierListPermanently(id: Long) = Unit
    override suspend fun deleteTierItemPermanently(id: Long) = Unit
    override suspend fun emptyTrash() = Unit
    override suspend fun getTrashEntries(): List<TrashEntry> = emptyList()
}
