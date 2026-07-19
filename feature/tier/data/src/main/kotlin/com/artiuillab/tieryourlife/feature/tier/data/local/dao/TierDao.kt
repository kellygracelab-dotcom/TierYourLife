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

    @Insert
    suspend fun insertTierList(tierList: TierListEntity): Long

    @Query("SELECT * FROM tier_lists WHERE id = :id")
    suspend fun getTierListById(id: Long): TierListEntity?

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
