package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import javax.inject.Inject

class RoomTierRepository internal constructor(
    private val dao: TierDao,
    private val nowMillis: () -> Long,
) : TierRepository {

    @Inject constructor(dao: TierDao) : this(dao, System::currentTimeMillis)

    override suspend fun getTierListById(id: Long): TierList? {
        return dao.getTierListWithTiers(id)?.toDomain()
    }

    override suspend fun getAllTierLists(): List<TierList> {
        return dao.getAllTierLists().map { it.toDomain() }
    }

    override suspend fun createTierList(title: String): Long {
        return dao.createTierListWithDefaultTier(title = title)
    }

    override suspend fun addMovieToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?
    ): Long {
        return dao.addMovieToPool(tierListId, title, imageUrl)
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

    // Permanent removal happens only when the user deletes a trashed entry again;
    // nothing expires on its own.
    override suspend fun deleteTierListPermanently(id: Long) {
        dao.deleteTierListById(id)
    }

    override suspend fun deleteTierItemPermanently(id: Long) {
        dao.deleteTierItemById(id)
    }

    override suspend fun emptyTrash() {
        dao.emptyTrash()
    }

    override suspend fun getTrashEntries(): List<TrashEntry> {
        val lists = dao.getDeletedTierLists().map { it.toDomain() }
        val items = dao.getDeletedTierItems().map { it.toDomain() }
        return (lists + items).sortedByDescending { it.deletedAtMillis }
    }
}
