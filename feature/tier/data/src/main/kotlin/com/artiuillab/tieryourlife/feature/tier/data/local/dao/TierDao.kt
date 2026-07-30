package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers

@Dao
interface TierDao {

    @Transaction
    suspend fun createTierListWithDefaultTier(title: String): Long {
        val tierListId = insertTierList(TierListEntity(title = title))

        val defaultTiers = listOf(
            TierEntity(tierListId = tierListId, position = 0, label = "S", color = "#FF7F7F"),
            TierEntity(tierListId = tierListId, position = 1, label = "A", color = "#FFBF7F"),
            TierEntity(tierListId = tierListId, position = 2, label = "B", color = "#FFDF7F"),
            TierEntity(tierListId = tierListId, position = 3, label = "C", color = "#FFFF7F"),
            TierEntity(tierListId = tierListId, position = 4, label = "D", color = "#BFFF7F"),
            TierEntity(
                tierListId = tierListId,
                position = 5,
                label = "Unranked",
                color = "",
                isPool = true
            ),
        )

        defaultTiers.forEach { insertTier(it) }

        return tierListId
    }

    @Insert
    suspend fun insertTierList(tierList: TierListEntity): Long

    @Query("SELECT * FROM tier_lists WHERE id = :id")
    suspend fun getTierListById(id: Long): TierListEntity?

    @Query("SELECT * FROM tier_lists ORDER BY id ASC")
    suspend fun getAllTierLists(): List<TierListEntity>

    @Transaction
    @Query("SELECT * FROM tier_lists WHERE id = :id")
    suspend fun getTierListWithTiers(id: Long): TierListWithTiers?

    @Query("DELETE FROM tier_lists WHERE id = :id")
    suspend fun deleteTierListById(id: Long): Int

    @Insert
    suspend fun insertTier(tier: TierEntity): Long

    @Query("SELECT * FROM tiers WHERE tierListId = :tierListId ORDER BY position ASC")
    suspend fun getAllTiersByTierListId(tierListId: Long): List<TierEntity>

    @Query("DELETE FROM tiers WHERE id = :id")
    suspend fun deleteTierById(id: Long): Int

    @Insert
    suspend fun insertTierItem(tierItem: TierItemEntity): Long

    @Query("SELECT * FROM tier_items WHERE tierId = :tierId ORDER BY position ASC")
    suspend fun getAllTierItemsByTierId(tierId: Long): List<TierItemEntity>
}
