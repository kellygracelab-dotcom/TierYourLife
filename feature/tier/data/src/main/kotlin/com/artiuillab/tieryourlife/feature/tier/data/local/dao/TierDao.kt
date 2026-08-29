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
    suspend fun addItemToPool(tierListId: Long, title: String, imageUrl: String?, source: String = "MANUAL"): Long {
        val poolTier = getAllTiersByTierListId(tierListId).first { it.isPool }

        val position = if (source == "GENERATED") {
            shiftTierPositionsFrom(poolTier.id, 0)
            0
        } else {
            val lastPosition = getAllTierItemsByTierId(poolTier.id).maxOfOrNull { it.position }
            (lastPosition ?: -1) + 1
        }

        return insertTierItem(
            tierItem = TierItemEntity(
                tierId = poolTier.id,
                position = position,
                title = title,
                imageUrl = imageUrl,
                source = source,
            ),
        )
    }

    @Transaction
    suspend fun addItemsToPool(tierListId: Long, items: List<NewPoolItem>) {
        if (getTierListById(tierListId) == null) return
        val poolTier = getAllTiersByTierListId(tierListId).first { it.isPool }
        var nextPosition = (getAllTierItemsByTierId(poolTier.id).maxOfOrNull { it.position } ?: -1) + 1

        items.forEach { item ->
            insertTierItem(
                TierItemEntity(
                    tierId = poolTier.id,
                    position = nextPosition++,
                    title = item.title,
                    imageUrl = item.imageUrl,
                    source = "TMDB",
                ),
            )
        }
    }

    @Transaction
    suspend fun createTierListFromTemplate(
        title: String,
        authorName: String,
        tiers: List<NewTemplateTier>,
        items: List<NewPoolItem>,
    ): Long {
        val tierListId = insertTierList(TierListEntity(title = title, authorName = authorName))

        tiers.forEachIndexed { index, tier ->
            insertTier(
                TierEntity(
                    tierListId = tierListId,
                    position = index,
                    label = tier.label,
                    caption = tier.caption,
                    colorLight = tier.colorLight,
                    colorDark = tier.colorDark,
                ),
            )
        }

        val poolTierId = insertTier(
            TierEntity(
                tierListId = tierListId,
                position = tiers.size,
                label = "Unranked",
                colorLight = DefaultTierColors.POOL_LIGHT,
                colorDark = DefaultTierColors.POOL_DARK,
                isPool = true,
            ),
        )

        items.forEachIndexed { index, item ->
            insertTierItem(
                TierItemEntity(
                    tierId = poolTierId,
                    position = index,
                    title = item.title,
                    imageUrl = item.imageUrl,
                    source = if (item.imageUrl?.startsWith("http") == true) "TMDB" else "MANUAL",
                ),
            )
        }

        return tierListId
    }

    @Query("SELECT COUNT(*) FROM tier_lists WHERE publishedId IS NOT NULL AND deletedAt IS NULL")
    suspend fun countPublishedLists(): Int

    @Query(
        """
        SELECT DISTINCT i.imageUrl FROM tier_items i
            JOIN tiers t ON i.tierId = t.id
            JOIN tier_lists l ON t.tierListId = l.id
        WHERE i.imageUrl LIKE 'https://%'
            AND i.deletedAt IS NULL
            AND l.deletedAt IS NULL
        LIMIT :limit
        """,
    )
    suspend fun cardImageUrls(limit: Int): List<String>

    @Query("UPDATE tier_lists SET publishedId = :publishedId WHERE id = :id")
    suspend fun setPublishedId(id: Long, publishedId: String?)

    @Query("UPDATE tier_lists SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: String?)

    @Query("UPDATE tier_lists SET coverImageUrl = :coverImageUrl WHERE id = :id")
    suspend fun setCoverImageUrl(id: Long, coverImageUrl: String?)

    @Transaction
    suspend fun createTierListWithDefaultTier(title: String): Long {
        val tierListId = insertTierList(TierListEntity(title = title))

        val defaultTiers = listOf(
            TierEntity(
                tierListId = tierListId, position = 0, label = "S", caption = "Masterpiece",
                colorLight = DefaultTierColors.S_LIGHT, colorDark = DefaultTierColors.S_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 1, label = "A", caption = "Great",
                colorLight = DefaultTierColors.A_LIGHT, colorDark = DefaultTierColors.A_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 2, label = "B", caption = "Good",
                colorLight = DefaultTierColors.B_LIGHT, colorDark = DefaultTierColors.B_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 3, label = "C", caption = "Watchable",
                colorLight = DefaultTierColors.C_LIGHT, colorDark = DefaultTierColors.C_DARK,
            ),
            TierEntity(
                tierListId = tierListId, position = 4, label = "D", caption = "No",
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

    @Query("SELECT * FROM active_tier_lists WHERE id = :id")
    suspend fun getTierListById(id: Long): TierListEntity?

    @Query("SELECT * FROM active_tier_lists ORDER BY id ASC")
    suspend fun getAllTierLists(): List<TierListEntity>

    @Transaction
    @Query("SELECT * FROM active_tier_lists ORDER BY id ASC")
    suspend fun getAllTierListsWithTiers(): List<TierListWithTiers>

    @Transaction
    @Query("SELECT * FROM active_tier_lists WHERE id = :id")
    suspend fun getTierListWithTiers(id: Long): TierListWithTiers?

    @Query("DELETE FROM tier_lists WHERE id = :id")
    suspend fun deleteTierListById(id: Long): Int

    @Query("UPDATE tier_lists SET deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun markTierListsDeleted(ids: List<Long>, deletedAt: Long)

    @Query("UPDATE tier_lists SET deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreTierLists(ids: List<Long>)

    @Query("UPDATE tier_lists SET displayMode = :displayMode WHERE id = :id")
    suspend fun updateTierListDisplayMode(id: Long, displayMode: String)

    @Query("UPDATE tier_lists SET title = :title WHERE id = :id")
    suspend fun updateTierListTitle(id: Long, title: String)

    @Query(
        """
        SELECT l.id AS id, l.title AS title, l.deletedAt AS deletedAt, l.publishedId AS publishedId,
            (SELECT COUNT(*) FROM tier_items i
                JOIN tiers t ON i.tierId = t.id
                WHERE t.tierListId = l.id AND i.deletedAt IS NULL) AS itemCount
        FROM tier_lists l
        WHERE l.deletedAt IS NOT NULL
        ORDER BY l.deletedAt DESC
        """,
    )
    suspend fun getDeletedTierLists(): List<DeletedTierListRow>

    @Transaction
    suspend fun emptyTrash() {
        deleteAllTrashedTierLists()
        deleteAllTrashedTierItems()
    }

    @Query("DELETE FROM tier_lists WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllTrashedTierLists()

    @Query("DELETE FROM tier_items WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllTrashedTierItems()

    @Insert
    suspend fun insertTier(tier: TierEntity): Long

    @Query("UPDATE tiers SET label = :label, caption = :caption WHERE id = :id")
    suspend fun renameTier(id: Long, label: String, caption: String?)

    @Query("UPDATE tiers SET colorLight = :colorLight, colorDark = :colorDark WHERE id = :id")
    suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String)

    @Query("UPDATE tiers SET position = :position WHERE id = :id")
    suspend fun updateTierPosition(id: Long, position: Int)

    @Transaction
    suspend fun reorderTiers(orderedTierIds: List<Long>) {
        val existingIds = orderedTierIds.filter { getTierById(it) != null }
        existingIds.forEachIndexed { index, tierId ->
            updateTierPosition(tierId, index)
        }
    }

    @Query("SELECT * FROM tiers WHERE id = :id")
    suspend fun getTierById(id: Long): TierEntity?

    @Query("SELECT * FROM tiers WHERE tierListId = :tierListId ORDER BY position ASC")
    suspend fun getAllTiersByTierListId(tierListId: Long): List<TierEntity>

    @Query("DELETE FROM tiers WHERE id = :id")
    suspend fun deleteTierById(id: Long): Int

    @Query("SELECT * FROM tier_items WHERE tierId = :tierId AND deletedAt IS NOT NULL")
    suspend fun getTrashedItemsByTierId(tierId: Long): List<TierItemEntity>

    @Query("UPDATE tier_items SET tierId = :toTierId, position = :position WHERE id = :id")
    suspend fun reassignTierItem(id: Long, toTierId: Long, position: Int)

    @Transaction
    suspend fun deleteTierToPool(tierId: Long) {
        val tier = getTierById(tierId) ?: return
        if (tier.isPool) return
        val pool = getAllTiersByTierListId(tier.tierListId).firstOrNull { it.isPool }
            ?: return

        var nextPosition = (getAllTierItemsByTierId(pool.id).maxOfOrNull { it.position } ?: -1) + 1
        getAllTierItemsByTierId(tierId).forEach { item ->
            reassignTierItem(id = item.id, toTierId = pool.id, position = nextPosition++)
        }
        getTrashedItemsByTierId(tierId).forEach { item ->
            reassignTierItem(id = item.id, toTierId = pool.id, position = nextPosition++)
        }

        deleteTierById(tierId)
    }

    @Query("UPDATE tiers SET position = position + 1 WHERE tierListId = :tierListId AND position >= :fromPosition")
    suspend fun shiftTiersFrom(tierListId: Long, fromPosition: Int)

    @Transaction
    suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long {
        val rankedCount = getAllTiersByTierListId(tierListId).count { !it.isPool }
        shiftTiersFrom(tierListId, rankedCount)

        return insertTier(
            TierEntity(
                tierListId = tierListId,
                position = rankedCount,
                label = label,
                caption = caption,
                colorLight = colorLight,
                colorDark = colorDark,
            ),
        )
    }

    @Transaction
    suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ): Long {
        if (getTierListById(tierListId) == null) return 0

        val tierId = addTier(tierListId, label, caption, colorLight, colorDark)
        val rankedIds = getAllTiersByTierListId(tierListId)
            .filterNot { it.isPool }
            .map { it.id }
            .toMutableList()
        rankedIds.remove(tierId)
        rankedIds.add(position.coerceIn(0, rankedIds.size), tierId)
        reorderTiers(rankedIds)

        itemIds.forEachIndexed { index, itemId ->
            moveItem(itemId, tierId, index)
        }
        return tierId
    }

    @Insert
    suspend fun insertTierItem(tierItem: TierItemEntity): Long

    @Query("SELECT * FROM active_tier_items WHERE tierId = :tierId ORDER BY position ASC")
    suspend fun getAllTierItemsByTierId(tierId: Long): List<TierItemEntity>

    @Query("SELECT * FROM active_tier_items WHERE id = :id")
    suspend fun getTierItemById(id: Long): TierItemEntity?

    @Query("DELETE FROM tier_items WHERE id = :id")
    suspend fun deleteTierItemById(id: Long): Int

    @Query("UPDATE tier_items SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun markTierItemDeleted(id: Long, deletedAt: Long)

    @Query("UPDATE tier_items SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreTierItem(id: Long)

    @Query("UPDATE tier_items SET imageUrl = :imageUrl WHERE id = :id")
    suspend fun updateTierItemImage(id: Long, imageUrl: String?)

    @Query("SELECT imageUrl FROM tier_items WHERE id = :id")
    suspend fun getTierItemImageById(id: Long): String?

    @Query("SELECT imageUrl FROM tier_items WHERE imageUrl IS NOT NULL")
    suspend fun getAllImageRefs(): List<String>

    @Query(
        """
        SELECT i.id AS id, i.title AS title, i.deletedAt AS deletedAt,
            t.isPool AS wasInPool, l.title AS listTitle, i.imageUrl AS imageUrl
        FROM tier_items i
        JOIN tiers t ON i.tierId = t.id
        JOIN tier_lists l ON t.tierListId = l.id
        WHERE i.deletedAt IS NOT NULL AND l.deletedAt IS NULL
        ORDER BY i.deletedAt DESC
        """,
    )
    suspend fun getDeletedTierItems(): List<DeletedTierItemRow>

    @Query("UPDATE tier_items SET position = :position WHERE id = :id")
    suspend fun updateTierItemPosition(id: Long, position: Int)

    @Query("UPDATE tier_items SET tierId = :tierId, position = :position WHERE id = :id")
    suspend fun updateTierItemTierAndPosition(id: Long, tierId: Long, position: Int)

    @Query("UPDATE tier_items SET position = position - 1 WHERE tierId = :tierId AND position > :afterPosition")
    suspend fun compactTierPositionsAfter(tierId: Long, afterPosition: Int)

    @Query("UPDATE tier_items SET position = position + 1 WHERE tierId = :tierId AND position >= :fromPosition")
    suspend fun shiftTierPositionsFrom(tierId: Long, fromPosition: Int)

    @Transaction
    suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        val item = getTierItemById(itemId) ?: return
        val targetTier = getTierById(toTierId) ?: return
        if (getTierListById(targetTier.tierListId) == null) return

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
