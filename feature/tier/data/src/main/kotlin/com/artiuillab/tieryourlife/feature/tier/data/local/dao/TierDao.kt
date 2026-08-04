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

    // All movies land in one transaction with consecutive positions appended to the
    // pool, so an interruption cannot leave a half-inserted batch. Positions are
    // computed from getAllTierItemsByTierId, which already reads through the active
    // items view, so a trashed pool item never reserves or collides with a position.
    // The target list is read through its own active view too, so a bulk add against
    // a trashed list's pool is silently skipped instead of resurrecting it.
    @Transaction
    suspend fun addMoviesToPool(tierListId: Long, movies: List<NewPoolMovie>) {
        if (getTierListById(tierListId) == null) return
        val poolTier = getAllTiersByTierListId(tierListId).first { it.isPool }
        var nextPosition = (getAllTierItemsByTierId(poolTier.id).maxOfOrNull { it.position } ?: -1) + 1

        movies.forEach { movie ->
            insertTierItem(
                TierItemEntity(
                    tierId = poolTier.id,
                    position = nextPosition++,
                    title = movie.title,
                    imageUrl = movie.imageUrl,
                ),
            )
        }
    }

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
        SELECT l.id AS id, l.title AS title, l.deletedAt AS deletedAt,
            (SELECT COUNT(*) FROM tier_items i
                JOIN tiers t ON i.tierId = t.id
                WHERE t.tierListId = l.id AND i.deletedAt IS NULL) AS itemCount
        FROM tier_lists l
        WHERE l.deletedAt IS NOT NULL
        ORDER BY l.deletedAt DESC
        """,
    )
    suspend fun getDeletedTierLists(): List<DeletedTierListRow>

    // One transaction: either the whole trash is gone or none of it.
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

    // The caller sends the final sequence of tier ids in one call; positions are
    // rewritten to the sequence indices inside a single transaction, so no
    // intermediate state with duplicated positions is ever visible. A stale id for a
    // tier that no longer exists (already hard-deleted) is dropped before indices are
    // assigned, so it can't waste a slot and leave a gap in the real tiers' positions.
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

    @Query("UPDATE tiers SET position = position + 1 WHERE tierListId = :tierListId AND position >= :fromPosition")
    suspend fun shiftTiersFrom(tierListId: Long, fromPosition: Int)

    // rankedCount is also the pool's current position (it always sits last), so
    // shifting from there makes room without disturbing the ranked tiers before it.
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

    // Raw table on purpose: the image of a trashed item must still be found when the
    // item is deleted for good.
    @Query("SELECT imageUrl FROM tier_items WHERE id = :id")
    suspend fun getTierItemImageById(id: Long): String?

    // Raw table and unfiltered by soft delete on purpose: a trashed item's image copy
    // is still referenced (and must not be swept as an orphan) until the item itself
    // is permanently removed.
    @Query("SELECT imageUrl FROM tier_items WHERE imageUrl IS NOT NULL")
    suspend fun getAllImageRefs(): List<String>

    // Items whose list is also trashed are represented by the list row, not listed twice.
    @Query(
        """
        SELECT i.id AS id, i.title AS title, i.deletedAt AS deletedAt,
            t.isPool AS wasInPool, l.title AS listTitle
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

    // Relies on (tierId, position) having no unique index: the UPDATE below briefly gives two
    // rows the same position mid-statement. Adding that index would break every non-append insert.
    @Transaction
    suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        // Both reads go through the active views, so a deleted item or a tier whose
        // list is trashed cancels the move here instead of relocating it.
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
