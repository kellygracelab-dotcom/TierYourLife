package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierItemRow
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierListRow
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomTierRepositoryDeleteTest {

    @Test
    fun `delete marks rows with the injected current time`() = runBlocking {
        val dao = RecordingTierDao()
        val fixedNow = 1_700_000_000_000L
        val repository = RoomTierRepository(dao) { fixedNow }

        repository.deleteTierLists(listOf(1L, 2L))
        repository.deleteTierItem(7L)

        assertEquals(listOf(listOf(1L, 2L) to fixedNow), dao.markedListDeletes)
        assertEquals(listOf(7L to fixedNow), dao.markedItemDeletes)
    }

    @Test
    fun `permanent delete removes single entries by id`() = runBlocking {
        val dao = RecordingTierDao()
        val repository = RoomTierRepository(dao) { 0L }

        repository.deleteTierListPermanently(3L)
        repository.deleteTierItemPermanently(9L)

        assertEquals(listOf(3L), dao.permanentlyDeletedListIds)
        assertEquals(listOf(9L), dao.permanentlyDeletedItemIds)
    }
}

private class RecordingTierDao : TierDao {
    val markedListDeletes = mutableListOf<Pair<List<Long>, Long>>()
    val markedItemDeletes = mutableListOf<Pair<Long, Long>>()
    val permanentlyDeletedListIds = mutableListOf<Long>()
    val permanentlyDeletedItemIds = mutableListOf<Long>()

    override suspend fun markTierListsDeleted(ids: List<Long>, deletedAt: Long) {
        markedListDeletes += ids to deletedAt
    }

    override suspend fun markTierItemDeleted(id: Long, deletedAt: Long) {
        markedItemDeletes += id to deletedAt
    }

    override suspend fun deleteTierListById(id: Long): Int {
        permanentlyDeletedListIds += id
        return 1
    }

    override suspend fun deleteTierItemById(id: Long): Int {
        permanentlyDeletedItemIds += id
        return 1
    }

    override suspend fun insertTierList(tierList: TierListEntity): Long = 0L
    override suspend fun getTierListById(id: Long): TierListEntity? = null
    override suspend fun getAllTierLists(): List<TierListEntity> = emptyList()
    override suspend fun getTierListWithTiers(id: Long): TierListWithTiers? = null
    override suspend fun restoreTierLists(ids: List<Long>) = Unit
    override suspend fun updateTierListDisplayMode(id: Long, displayMode: String) = Unit
    override suspend fun updateTierListTitle(id: Long, title: String) = Unit
    override suspend fun restoreTierItem(id: Long) = Unit
    override suspend fun getDeletedTierLists(): List<DeletedTierListRow> = emptyList()
    override suspend fun getDeletedTierItems(): List<DeletedTierItemRow> = emptyList()
    override suspend fun deleteAllTrashedTierLists() = Unit
    override suspend fun deleteAllTrashedTierItems() = Unit
    override suspend fun insertTier(tier: TierEntity): Long = 0L
    override suspend fun getTierById(id: Long): TierEntity? = null
    override suspend fun getAllTiersByTierListId(tierListId: Long): List<TierEntity> = emptyList()
    override suspend fun deleteTierById(id: Long): Int = 0
    override suspend fun shiftTiersFrom(tierListId: Long, fromPosition: Int) = Unit
    override suspend fun insertTierItem(tierItem: TierItemEntity): Long = 0L
    override suspend fun getAllTierItemsByTierId(tierId: Long): List<TierItemEntity> = emptyList()
    override suspend fun getTierItemById(id: Long): TierItemEntity? = null
    override suspend fun updateTierItemPosition(id: Long, position: Int) = Unit
    override suspend fun updateTierItemTierAndPosition(id: Long, tierId: Long, position: Int) = Unit
    override suspend fun compactTierPositionsAfter(tierId: Long, afterPosition: Int) = Unit
    override suspend fun shiftTierPositionsFrom(tierId: Long, fromPosition: Int) = Unit
}
