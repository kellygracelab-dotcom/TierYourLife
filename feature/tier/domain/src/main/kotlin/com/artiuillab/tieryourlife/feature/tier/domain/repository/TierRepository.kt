package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

interface TierRepository {

    suspend fun getTierListById(id: Long): TierList?

    suspend fun getAllTierLists(): List<TierList>

    suspend fun createTierList(title: String): Long

    /** Builds a list from someone else's published template, items unranked. */
    suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
    ): Long

    suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?)

    /**
     * The boards whose published copy no longer matches them, by the id the
     * feed keeps that copy under.
     *
     * A board published before this was recorded says nothing: not knowing
     * what was sent is not the same as knowing it was something else, and
     * showing it as behind would send somebody to republish a list that was
     * already right.
     */
    suspend fun publishedCopiesLeftBehind(): Set<String>

    /** The board this published copy came from, if this phone still has it. */
    suspend fun boardPublishedAs(publishedId: String): TierList?

    suspend fun setCategory(id: Long, category: ListCategory?)

    suspend fun setCoverImageUrl(id: Long, coverImageUrl: String?)

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
