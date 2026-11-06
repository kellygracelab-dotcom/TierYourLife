package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

/** Boards made without an account: moved into one at a sign-in, or held for one whose session ended. */
@Dao
interface GuestBoardsDao {

    /** Boards made without an account, trash left out: what the question about them counts. */
    @Query("SELECT * FROM tier_lists WHERE ownerUid IS NULL AND deletedAt IS NULL")
    suspend fun guestBoardsInUse(): List<TierListEntity>

    @Query("UPDATE tier_lists SET ownerUid = :owner WHERE ownerUid IS NULL")
    suspend fun giveGuestBoardsTo(owner: String): Int

    @Query(
        "DELETE FROM board_sync WHERE account != :owner AND account != '${BoardSyncEntity.UNCLAIMED}' " +
            "AND listUid IN (SELECT uid FROM tier_lists WHERE ownerUid IS NULL)",
    )
    suspend fun forgetOtherAccountsRecordsOfGuestBoards(owner: String)

    /**
     * The trash goes with them: it is part of the same library. An account
     * that agreed on one of them before loses the record of it, as in
     * [OwnerDao.settleOwners]; otherwise its next run would take the board's
     * absence from its own for a delete here.
     */
    @Transaction
    suspend fun moveGuestBoardsTo(owner: String): Int {
        forgetOtherAccountsRecordsOfGuestBoards(owner)
        return giveGuestBoardsTo(owner)
    }

    /**
     * A session that ended without a sign-out by choice: the boards are this
     * phone's own from then on, but keep their records with [account], so
     * signing back into it [reclaimHeldBoards] finds each with its copy.
     */
    @Query("UPDATE tier_lists SET ownerUid = NULL WHERE ownerUid = :account")
    suspend fun holdBoardsOf(account: String): Int

    @Query(
        "UPDATE tier_lists SET ownerUid = :account " +
            "WHERE ownerUid IS NULL AND uid IN (SELECT listUid FROM board_sync WHERE account = :account)",
    )
    suspend fun reclaimHeldBoards(account: String): Int
}
