package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.PictureSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

/** One tier and the cards in it, as they arrive from the account. */
data class IncomingTier(val tier: TierEntity, val items: List<TierItemEntity>)

/**
 * Sync's own queries, apart from [TierDao] in the one way that matters: they
 * include the trash. A backup that dropped it would throw away someone's undo
 * along with their phone.
 */
@Dao
interface BoardSyncDao {

    @Query("SELECT * FROM board_sync")
    suspend fun allSyncRecords(): List<BoardSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remember(record: BoardSyncEntity)

    @Query("DELETE FROM board_sync WHERE listUid = :listUid")
    suspend fun forget(listUid: String)

    @Query("DELETE FROM picture_sync")
    suspend fun forgetEveryPicture()

    @Query("SELECT pictureId FROM picture_sync")
    suspend fun sentPictureIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rememberPicture(record: PictureSyncEntity)

    /** Trashed cards included: restoring one to a blank tile is not restoring it. */
    @Query("SELECT DISTINCT imageUrl FROM tier_items WHERE imageUrl IS NOT NULL")
    suspend fun allImageUrls(): List<String>

    @Query("UPDATE tier_items SET imageUrl = :imageUrl WHERE uid = :itemUid")
    suspend fun setItemImage(itemUid: String, imageUrl: String)

    @Query("SELECT * FROM tier_lists")
    suspend fun allBoards(): List<TierListEntity>

    @Query("SELECT * FROM tier_lists WHERE deletedAt IS NULL")
    suspend fun boardsInUse(): List<TierListEntity>

    /** Not touching editedAt: nothing about the board changed, and stamping it would make every phone re-send an identical board. */
    @Query("UPDATE tier_lists SET publishedId = NULL WHERE publishedId IN (:goneIds)")
    suspend fun forgetPublished(goneIds: List<String>)

    @Query("UPDATE tier_lists SET title = :title WHERE uid = :uid")
    suspend fun renameBoard(uid: String, title: String)

    @Query("UPDATE tier_lists SET deletedAt = :deletedAt WHERE deletedAt IS NULL")
    suspend fun trashEveryBoard(deletedAt: Long)

    @Query("SELECT * FROM tier_lists WHERE uid = :uid")
    suspend fun boardByUid(uid: String): TierListEntity?

    @Query("SELECT * FROM tiers WHERE tierListId = :boardId ORDER BY position ASC, uid ASC")
    suspend fun tiersOf(boardId: Long): List<TierEntity>

    @Query(
        """
        SELECT i.* FROM tier_items i
            JOIN tiers t ON i.tierId = t.id
        WHERE t.tierListId = :boardId
        ORDER BY i.position ASC, i.uid ASC
        """,
    )
    suspend fun itemsOf(boardId: Long): List<TierItemEntity>

    @Insert
    suspend fun insertBoard(board: TierListEntity): Long

    @Insert
    suspend fun insertTier(tier: TierEntity): Long

    @Insert
    suspend fun insertItem(item: TierItemEntity)

    @Update
    suspend fun updateBoard(board: TierListEntity)

    @Query("DELETE FROM tiers WHERE tierListId = :boardId")
    suspend fun deleteTiersOf(boardId: Long)

    @Query("DELETE FROM tier_lists WHERE uid = :uid")
    suspend fun deleteBoardByUid(uid: String)

    /** A board arriving for the first time, under the uid the account keeps it as. */
    @Transaction
    suspend fun addBoard(board: TierListEntity, tiers: List<IncomingTier>) {
        writeContents(insertBoard(board.copy(id = 0)), tiers)
    }

    /**
     * Replaces a board's contents, keeping its row and uid. Whole board, not
     * row by row: matching card against card buys nothing when every one is
     * written either way. `tiers` cascades to the cards.
     */
    @Transaction
    suspend fun replaceContents(board: TierListEntity, tiers: List<IncomingTier>) {
        deleteTiersOf(board.id)
        updateBoard(board)
        writeContents(board.id, tiers)
    }

    suspend fun writeContents(boardId: Long, tiers: List<IncomingTier>) {
        tiers.forEach { incoming ->
            val tierId = insertTier(incoming.tier.copy(id = 0, tierListId = boardId))
            incoming.items.forEach { item -> insertItem(item.copy(id = 0, tierId = tierId)) }
        }
    }
}
