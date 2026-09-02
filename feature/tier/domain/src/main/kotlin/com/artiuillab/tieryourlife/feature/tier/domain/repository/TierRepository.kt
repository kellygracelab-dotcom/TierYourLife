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
     * How each published copy stands against the board it came from.
     *
     * Two different questions, because they have different answers for a board
     * published before any of this was recorded: that copy *can* be sent again
     * -- there is a board right here to send -- but nobody knows whether it
     * needs to be.
     *
     * The first mistake here was treating "we do not know" as "do not offer".
     * That is honest about the line of type and wrong about the button: the
     * cost of an unnecessary update is one republish, and the cost of never
     * offering one is that everybody who published before the change can never
     * update anything again.
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
