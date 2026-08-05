package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.NewPoolMovie
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
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
        return dao.getAllTierLists().map { it.toDomain() }
    }

    override suspend fun createTierList(title: String): Long {
        return dao.createTierListWithDefaultTier(title = title)
    }

    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) {
        dao.updateTierListDisplayMode(id, displayMode.name)
    }

    override suspend fun renameTierList(id: Long, title: String) {
        dao.updateTierListTitle(id, title)
    }

    override suspend fun addMovieToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?
    ): Long {
        return dao.addMovieToPool(tierListId, title, imageUrl)
    }

    override suspend fun addMoviesToPool(tierListId: Long, movies: List<PoolMovieDraft>) {
        dao.addMoviesToPool(
            tierListId,
            movies.map { NewPoolMovie(title = it.title, imageUrl = it.imageUrl) },
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

    override suspend fun deleteTier(id: Long) {
        val tier = dao.getTierById(id) ?: return
        // The list must always keep exactly one pool; the pool tier can't be deleted.
        if (tier.isPool) return
        dao.deleteTierById(id)
        // The cascade removed item rows silently; sweep their image copies.
        imageStore.deleteOrphans(dao.getAllImageRefs())
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
