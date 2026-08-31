package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.NewPoolItem
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.NewTemplateTier
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.sync.PublishFingerprint
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
import javax.inject.Inject

class RoomTierRepository internal constructor(
    private val dao: TierDao,
    private val imageStore: TierImageStore,
    private val nowMillis: () -> Long,
) : TierRepository {

    @Inject constructor(dao: TierDao, imageStore: TierImageStore) :
        this(dao, imageStore, System::currentTimeMillis)

    override suspend fun getTierListById(id: Long): TierList? {
        return dao.getTierListWithTiers(id)?.toDomain()
    }

    override suspend fun getAllTierLists(): List<TierList> {
        return dao.getAllTierListsWithTiers().map { it.toDomain() }
    }

    override suspend fun createTierList(title: String): Long {
        return dao.createTierListWithDefaultTier(title = title)
    }

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
    ): Long = dao.createTierListFromTemplate(
        title = title,
        authorName = authorName,
        tiers = tiers.map { NewTemplateTier(it.label, it.caption, it.colorLight, it.colorDark) },
        items = items.map { NewPoolItem(it.title, it.imageUrl) },
    )

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) {
        dao.setPublished(id, publishedId, fingerprint)
    }

    override suspend fun publishedStanding(): PublishedStanding {
        val published = dao.getAllTierListsWithTiers()
            .map { it.toDomain() }
            .filter { board -> board.publishedId != null }
        return PublishedStanding(
            canUpdate = published.mapNotNull { it.publishedId }.toSet(),
            knownBehind = published
                .filter { board -> board.publishedFingerprint != null }
                .filter { board -> PublishFingerprint.of(board, imageStore::pictureIdOf) != board.publishedFingerprint }
                .mapNotNull { board -> board.publishedId }
                .toSet(),
        )
    }

    override suspend fun boardPublishedAs(publishedId: String): TierList? =
        dao.getAllTierListsWithTiers()
            .map { it.toDomain() }
            .firstOrNull { board -> board.publishedId == publishedId }

    override suspend fun setCategory(id: Long, category: ListCategory?) {
        dao.setCategory(id, category?.id)
    }

    override suspend fun setCoverImageUrl(id: Long, coverImageUrl: String?) {
        dao.setCoverImageUrl(id, coverImageUrl)
    }

    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) {
        dao.updateTierListDisplayMode(id, displayMode.name)
    }

    override suspend fun renameTierList(id: Long, title: String) {
        dao.updateTierListTitle(id, title)
    }

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long {
        return dao.addItemToPool(tierListId, title, imageUrl, source.name)
    }

    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) {
        dao.addItemsToPool(
            tierListId,
            items.map { NewPoolItem(title = it.title, imageUrl = it.imageUrl) },
        )
    }

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) {
        val copyPath = imageStore.copyToInternalStorage(sourceUri)
        val previousImage = dao.getTierItemImageById(itemId)
        dao.updateTierItemImage(itemId, copyPath)
        if (previousImage != null) {
            imageStore.deleteCopy(previousImage)
        }
    }

    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        dao.moveItem(itemId, toTierId, toPosition)
    }

    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long {
        return dao.addTier(tierListId, label, caption, colorLight, colorDark)
    }

    override suspend fun renameTier(id: Long, label: String, caption: String?) {
        dao.renameTier(id, label, caption)
    }

    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) {
        dao.updateTierColors(id, colorLight, colorDark)
    }

    override suspend fun deleteTierToPool(id: Long) {
        dao.deleteTierToPool(id)
    }

    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) {
        dao.restoreTier(tierListId, label, caption, colorLight, colorDark, position, itemIds)
    }

    override suspend fun reorderTiers(orderedTierIds: List<Long>) {
        dao.reorderTiers(orderedTierIds)
    }

    override suspend fun deleteTierLists(ids: List<Long>) {
        dao.markTierListsDeleted(ids, nowMillis())
    }

    override suspend fun restoreTierLists(ids: List<Long>) {
        dao.restoreTierLists(ids)
    }

    override suspend fun deleteTierItem(id: Long) {
        dao.markTierItemDeleted(id, nowMillis())
    }

    override suspend fun restoreTierItem(id: Long) {
        dao.restoreTierItem(id)
    }

    override suspend fun deleteTierListPermanently(id: Long) {
        dao.deleteTierListById(id)
        imageStore.deleteOrphans(dao.getAllImageRefs())
    }

    override suspend fun deleteTierItemPermanently(id: Long) {
        val imagePath = dao.getTierItemImageById(id)
        dao.deleteTierItemById(id)
        if (imagePath != null) {
            imageStore.deleteCopy(imagePath)
        }
    }

    override suspend fun emptyTrash() {
        dao.emptyTrash()
        imageStore.deleteOrphans(dao.getAllImageRefs())
    }

    override suspend fun getTrashEntries(): List<TrashEntry> {
        val lists = dao.getDeletedTierLists().map { it.toDomain() }
        val items = dao.getDeletedTierItems().map { it.toDomain() }
        return (lists + items).sortedByDescending { it.deletedAtMillis }
    }
}
