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

    /**
     * Builds a board from somebody else's published list, keeping the ranking
     * it was handed: each of [tiers] arrives with the cards ranked into it,
     * and [items] are the ones left in the pool.
     */
    suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
    ): Long

    suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?)

    /**
     * Two questions, because a board published before this was recorded can
     * be sent again but nobody knows whether it needs to be; "we do not know"
     * must not mean "do not offer".
     */
    suspend fun publishedStanding(): PublishedStanding

    /** The board this published copy came from, if this phone still has it. */
    suspend fun boardPublishedAs(publishedId: String): TierList?

    suspend fun setCategory(id: Long, category: ListCategory?)

    /** Starred at [at], or unstarred when it is null. */
    suspend fun setFavouritedAt(id: Long, at: Long?)

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

/**
 * [canUpdate] is every published copy this phone still has the board for.
 * [knownBehind] is the subset we can prove has been left behind.
 */
data class PublishedStanding(
    val canUpdate: Set<String> = emptySet(),
    val knownBehind: Set<String> = emptySet(),
)
