package com.artiuillab.tieryourlife.feature.tier.domain

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository

data class SavedTemplate(
    val title: String,
    val authorName: String,
    val tiers: List<Tier>,
    val items: List<TierItem>,
    val takenFrom: String? = null,
)

class RecordingTierRepository(
    /** Stands in for a local copy that knows it was published. */
    private val publishedIdOfFirstList: String? = null,
    /** What getTierListById finds. */
    private val boards: List<TierList> = emptyList(),
) : TierRepository {

    val templates = mutableListOf<SavedTemplate>()
    var clearedPublishedId: Long? = null

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
        takenFrom: String?,
    ): Long {
        templates += SavedTemplate(title, authorName, tiers, items, takenFrom)
        return 7L
    }

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) {
        if (publishedId == null) clearedPublishedId = id
    }

    /** Every ranking mark set, as (id, listId). */
    val rankings = mutableListOf<Pair<Long, String?>>()

    override suspend fun setRankingOf(id: Long, listId: String?) {
        rankings += id to listId
    }

    override suspend fun publishedStanding(): PublishedStanding = PublishedStanding()

    override suspend fun boardPublishedAs(publishedId: String): TierList? = null

    override suspend fun setFavouritedAt(id: Long, at: Long?) = Unit

    override suspend fun setCategory(id: Long, category: ListCategory?) = Unit

    val covers = mutableListOf<Pair<Long, List<String>>>()

    /** Every topic set, as (id, topicId, topicName, newTopicName). */
    val topics = mutableListOf<List<Any?>>()

    override suspend fun setTopic(id: Long, topicId: String?, topicName: String?, newTopicName: String?) {
        topics += listOf(id, topicId, topicName, newTopicName)
    }

    override suspend fun setCover(id: Long, pictures: List<String>) {
        covers += id to pictures
    }

    override suspend fun getTierListById(id: Long): TierList? = boards.firstOrNull { it.id == id }
    override suspend fun getAllTierLists(): List<TierList> = publishedIdOfFirstList
        ?.let { listOf(TierList(id = 7, title = "Local copy", tiers = emptyList(), publishedId = it)) }
        .orEmpty()
    override suspend fun createTierList(title: String, captions: List<String>): Long = 0
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
