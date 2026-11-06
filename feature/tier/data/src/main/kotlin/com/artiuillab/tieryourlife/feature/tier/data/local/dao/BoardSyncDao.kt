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
import java.util.UUID

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

    @Query("SELECT * FROM board_sync WHERE account = :account")
    suspend fun syncRecordsOf(account: String): List<BoardSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remember(record: BoardSyncEntity)

    @Query("DELETE FROM board_sync WHERE account = :account AND listUid = :listUid")
    suspend fun forget(account: String, listUid: String)

    @Query("DELETE FROM board_sync WHERE account = '${BoardSyncEntity.UNCLAIMED}' AND listUid IN (:listUids)")
    suspend fun forgetUnclaimed(listUids: List<String>)

    /** [considered] are the unclaimed rows looked at; the ones not [claimed] among them go. */
    @Transaction
    suspend fun claim(considered: List<String>, claimed: List<BoardSyncEntity>) {
        forgetUnclaimed(considered)
        claimed.forEach { remember(it) }
    }

    @Query("DELETE FROM picture_sync WHERE account = :account")
    suspend fun forgetPicturesOf(account: String)

    @Query("SELECT pictureId FROM picture_sync WHERE account = :account")
    suspend fun sentPictureIds(account: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rememberPicture(record: PictureSyncEntity)

    /** Trashed cards included: restoring one to a blank tile is not restoring it. */
    @Query(
        """
        SELECT DISTINCT i.imageUrl FROM tier_items i
            JOIN tiers t ON i.tierId = t.id
            JOIN tier_lists l ON t.tierListId = l.id
        WHERE i.imageUrl IS NOT NULL AND l.ownerUid = :owner
        """,
    )
    suspend fun imageUrlsOf(owner: String): List<String>

    @Query("UPDATE tier_items SET imageUrl = :imageUrl WHERE uid = :itemUid")
    suspend fun setItemImage(itemUid: String, imageUrl: String)

    /** Every board on the phone, whoever's: only for questions about the phone as a whole. */
    @Query("SELECT * FROM tier_lists")
    suspend fun allBoards(): List<TierListEntity>

    /** One account's boards, trash included. Another's are never sent, whoever is signed in. */
    @Query("SELECT * FROM tier_lists WHERE ownerUid = :owner")
    suspend fun boardsOf(owner: String): List<TierListEntity>

    /** Not touching editedAt: nothing about the board changed, and stamping it would make every phone re-send an identical board. */
    @Query("UPDATE tier_lists SET publishedId = NULL WHERE publishedId IN (:goneIds) AND ownerUid = :owner")
    suspend fun forgetPublished(owner: String, goneIds: List<String>)

    /** As [forgetPublished], leaves editedAt alone; only a board that claims nothing takes the id. */
    @Query(
        "UPDATE tier_lists SET publishedId = :publishedId " +
            "WHERE uid = :uid AND ownerUid = :owner AND publishedId IS NULL AND rankingOf IS NULL AND deletedAt IS NULL",
    )
    suspend fun relinkPublished(owner: String, uid: String, publishedId: String)

    @Query("UPDATE tier_lists SET title = :title WHERE uid = :uid")
    suspend fun renameBoard(uid: String, title: String)

    // The settle* writes change a text only if it is still what was read, so
    // an edit made in between is not undone.

    @Query("UPDATE tier_lists SET title = :now WHERE id = :id AND title = :was")
    suspend fun settleBoardTitle(id: Long, was: String, now: String)

    @Query("UPDATE tiers SET label = :now WHERE id = :id AND label = :was")
    suspend fun settleTierLabel(id: Long, was: String, now: String)

    @Query("UPDATE tiers SET caption = :now WHERE id = :id AND caption = :was")
    suspend fun settleTierCaption(id: Long, was: String, now: String?)

    @Query("UPDATE tier_items SET title = :now WHERE id = :id AND title = :was")
    suspend fun settleItemTitle(id: Long, was: String, now: String)

    @Query("UPDATE tier_items SET credit = :now WHERE id = :id AND credit = :was")
    suspend fun settleItemCredit(id: Long, was: String, now: String?)

    /**
     * Whatever [block] writes lands together or not at all: a board and the
     * record of agreeing on it, written apart, left a run killed in between
     * with a duplicate next time, or a trashed board deleted for good.
     */
    @Transaction
    suspend fun together(block: suspend () -> Unit) = block()

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

    @Query("DELETE FROM tier_lists WHERE uid = :uid")
    suspend fun deleteBoardByUid(uid: String)

    @Query("UPDATE tier_lists SET deletedAt = :deletedAt WHERE uid = :uid")
    suspend fun trashBoard(uid: String, deletedAt: Long)

    /** A board arriving for the first time, under the uid the account keeps it as. */
    @Transaction
    suspend fun addBoard(board: TierListEntity, tiers: List<IncomingTier>) {
        writeContents(insertBoard(board.copy(id = 0)), tiers)
    }

    /** Together, so a run that dies between the two does not bring the same board down again as another copy. */
    @Transaction
    suspend fun addBoardRemembering(board: TierListEntity, tiers: List<IncomingTier>, record: BoardSyncEntity) {
        addBoard(board, tiers)
        remember(record)
    }

    /**
     * Replaces a board's contents, keeping its row and uid, and the row of
     * every tier and card that is still on it, matched by uid: a screen open
     * on the board holds those ids, and a Play session that applied its order
     * to ids written over found no cards and moved nothing.
     */
    @Transaction
    suspend fun replaceContents(board: TierListEntity, tiers: List<IncomingTier>) {
        updateBoard(board)
        val tiersHere = tiersOf(board.id).associateBy { it.uid }
        val itemsHere = itemsOf(board.id).associateBy { it.uid }
        val keptTiers = mutableSetOf<Long>()
        val keptItems = mutableSetOf<Long>()
        tiers.forEach { incoming ->
            val tierHere = tiersHere[incoming.tier.uid]
            val tierId = if (tierHere != null) {
                updateTier(incoming.tier.copy(id = tierHere.id, tierListId = board.id))
                tierHere.id
            } else {
                insertTierWithFreeUid(incoming.tier.copy(tierListId = board.id))
            }
            keptTiers += tierId
            incoming.items.forEach { item ->
                val itemHere = itemsHere[item.uid]
                if (itemHere != null) {
                    updateItem(item.copy(id = itemHere.id, tierId = tierId))
                    keptItems += itemHere.id
                } else {
                    insertItemWithFreeUid(item.copy(tierId = tierId))
                }
            }
        }
        // Cards first: one moved out of a tier that goes has already been rewritten above.
        deleteItems(itemsHere.values.map { it.id }.filterNot { it in keptItems })
        deleteTiers(tiersHere.values.map { it.id }.filterNot { it in keptTiers })
    }

    @Update
    suspend fun updateTier(tier: TierEntity)

    @Update
    suspend fun updateItem(item: TierItemEntity)

    @Query("DELETE FROM tier_items WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<Long>)

    @Query("DELETE FROM tiers WHERE id IN (:ids)")
    suspend fun deleteTiers(ids: List<Long>)

    /**
     * A uid another board here already holds is replaced: the account keeps
     * uids per board, so the same game started on the site and on the phone
     * arrives under the uid it already has here.
     */
    suspend fun writeContents(boardId: Long, tiers: List<IncomingTier>) {
        tiers.forEach { incoming ->
            val tierId = insertTierWithFreeUid(incoming.tier.copy(tierListId = boardId))
            incoming.items.forEach { item -> insertItemWithFreeUid(item.copy(tierId = tierId)) }
        }
    }

    suspend fun insertTierWithFreeUid(tier: TierEntity): Long {
        val uid = if (tierUidTaken(tier.uid)) UUID.randomUUID().toString() else tier.uid
        return insertTier(tier.copy(id = 0, uid = uid))
    }

    suspend fun insertItemWithFreeUid(item: TierItemEntity) {
        val uid = if (itemUidTaken(item.uid)) UUID.randomUUID().toString() else item.uid
        insertItem(item.copy(id = 0, uid = uid))
    }

    @Query("SELECT EXISTS(SELECT 1 FROM tiers WHERE uid = :uid)")
    suspend fun tierUidTaken(uid: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM tier_items WHERE uid = :uid)")
    suspend fun itemUidTaken(uid: String): Boolean
}
