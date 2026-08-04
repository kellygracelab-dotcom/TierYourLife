package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.artiuillab.tieryourlife.feature.tier.data.local.DefaultTierColors
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers

@Dao
interface TierDao {

    @Transaction
    suspend fun addMovieToPool(tierListId: Long, title: String, imageUrl: String?): Long {
        val poolTier = getAllTiersByTierListId(tierListId).first { it.isPool }

        val lastPosition = getAllTierItemsByTierId(poolTier.id).maxOfOrNull { it.position }
        val nextPosition = (lastPosition ?: -1) + 1

        return insertTierItem(
            tierItem = TierItemEntity(
                tierId = poolTier.id,
                position = nextPosition,
                title = title,
                imageUrl = imageUrl,
            ),
        )
    }

    @Transaction
    suspend fun createTierListWithDefaultTier(title: String): Long {
        val tierListId = insertTierList(TierListEntity(title = title))

        val defaultTiers = listOf(
            TierEntity(
                tierListId = tierListId, position = 0, label = "S",
                colorLight = DefaultTierColors.S_LIGHT, colorDark = DefaultTierColors.S_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 1, label = "A",
                colorLight = DefaultTierColors.A_LIGHT, colorDark = DefaultTierColors.A_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 2, label = "B",
                colorLight = DefaultTierColors.B_LIGHT, colorDark = DefaultTierColors.B_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 3, label = "C",
                colorLight = DefaultTierColors.C_LIGHT, colorDark = DefaultTierColors.C_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 4, label = "D",
                colorLight = DefaultTierColors.D_LIGHT, colorDark = DefaultTierColors.D_DARK,
            ),
            TierEntity(
                tierListId = tierListId,
                position = 5,
                label = "Unranked",
                colorLight = DefaultTierColors.POOL_LIGHT,
                colorDark = DefaultTierColors.POOL_DARK,
                isPool = true,
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

    @Query("SELECT * FROM tier_items WHERE id = :id")
    suspend fun getTierItemById(id: Long): TierItemEntity?

    @Query("UPDATE tier_items SET position = :position WHERE id = :id")
    suspend fun updateTierItemPosition(id: Long, position: Int)

    @Query("UPDATE tier_items SET tierId = :tierId, position = :position WHERE id = :id")
    suspend fun updateTierItemTierAndPosition(id: Long, tierId: Long, position: Int)

    @Query("UPDATE tier_items SET position = position - 1 WHERE tierId = :tierId AND position > :afterPosition")
    suspend fun compactTierPositionsAfter(tierId: Long, afterPosition: Int)

    @Query("UPDATE tier_items SET position = position + 1 WHERE tierId = :tierId AND position >= :fromPosition")
    suspend fun shiftTierPositionsFrom(tierId: Long, fromPosition: Int)

    // The shift below runs as one UPDATE that touches every affected row, so mid-statement two
    // items in the same tier briefly share a position before the later rows settle one higher.
    // That's only safe because (tierId, position) has no unique index. Adding one would make
    // every non-append insert fail, since SQLite enforces UNIQUE per row, not at transaction end.
    @Transaction
    suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        val item = getTierItemById(itemId) ?: return
        val fromTierId = item.tierId

        if (fromTierId == toTierId) {
            val siblings = getAllTierItemsByTierId(fromTierId).filterNot { it.id == itemId }
            val clampedPosition = toPosition.coerceIn(0, siblings.size)
            val reordered = siblings.toMutableList().apply { add(clampedPosition, item) }
            reordered.forEachIndexed { index, tierItem ->
                if (tierItem.position != index) {
                    updateTierItemPosition(tierItem.id, index)
                }
            }
        } else {
            compactTierPositionsAfter(fromTierId, item.position)

            val targetSiblings = getAllTierItemsByTierId(toTierId)
            val clampedPosition = toPosition.coerceIn(0, targetSiblings.size)
            shiftTierPositionsFrom(toTierId, clampedPosition)

            updateTierItemTierAndPosition(itemId, toTierId, clampedPosition)
        }
    }
}
