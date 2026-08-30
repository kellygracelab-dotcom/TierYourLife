package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

/** One tier and the cards in it, as they arrive from the account. */
data class IncomingTier(val tier: TierEntity, val items: List<TierItemEntity>)

/**
 * Everything sync reads and writes, kept apart from [TierDao].
 *
 * The queries here differ from the app's in one way that matters: they include
 * what is in the trash. The app hides trashed boards and cards because nobody
 * wants to look at them, but a backup that dropped them would throw away
 * someone's undo along with their phone.
 */
@Dao
interface BoardSyncDao {

    @Query("SELECT * FROM board_sync")
    suspend fun allSyncRecords(): List<BoardSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remember(record: BoardSyncEntity)

    @Query("DELETE FROM board_sync WHERE listUid = :listUid")
    suspend fun forget(listUid: String)

    @Query("SELECT * FROM tier_lists")
    suspend fun allBoards(): List<TierListEntity>

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
     * Replaces a board's contents, keeping its row and its uid.
     *
     * Row by row would be the same work with much more of it: a board coming
     * back from the account is the whole board, and matching card against card
     * to find which three moved buys nothing when every one of them is written
     * either way. The cards go with their tiers -- `tiers` cascades.
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
