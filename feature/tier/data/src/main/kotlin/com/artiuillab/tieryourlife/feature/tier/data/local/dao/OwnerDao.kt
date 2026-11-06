package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

/** Whose each board is. The one place boards change hands. */
@Dao
interface OwnerDao {

    @Query("SELECT uid FROM tier_lists WHERE ownerUid = '${TierListEntity.UNSETTLED}'")
    suspend fun unsettledBoards(): List<String>

    @Query("SELECT * FROM board_sync WHERE account != '${BoardSyncEntity.UNCLAIMED}'")
    suspend fun claimedSyncRecords(): List<BoardSyncEntity>

    @Query("UPDATE tier_lists SET ownerUid = :owner WHERE uid = :uid AND ownerUid = '${TierListEntity.UNSETTLED}'")
    suspend fun settleOwner(uid: String, owner: String?)

    @Query(
        "DELETE FROM board_sync WHERE listUid = :uid AND account != '${BoardSyncEntity.UNCLAIMED}' " +
            "AND (:owner IS NULL OR account != :owner)",
    )
    suspend fun forgetOtherAccountsRecords(uid: String, owner: String?)

    /**
     * The accounts that lost a board keep their copy of it, but no longer a
     * record of agreeing on it here: with one, their next run would read the
     * board as thrown away on this phone and take it out of them. Without, it
     * comes down to them as a copy beside the owner's.
     */
    @Transaction
    suspend fun settleOwners(owners: Map<String, String?>) {
        owners.forEach { (uid, owner) ->
            settleOwner(uid, owner)
            forgetOtherAccountsRecords(uid, owner)
        }
    }

    /** Boards made without an account, trash left out: what the question about them counts. */
    @Query("SELECT * FROM tier_lists WHERE ownerUid IS NULL AND deletedAt IS NULL")
    suspend fun guestBoardsInUse(): List<TierListEntity>

    /** The trash goes with them: it is part of the same library. */
    @Query("UPDATE tier_lists SET ownerUid = :owner WHERE ownerUid IS NULL")
    suspend fun moveGuestBoardsTo(owner: String): Int

    /** Its published lists and rankings went with the account, so no board here is out as one any more. */
    @Query(
        "UPDATE tier_lists SET ownerUid = NULL, publishedId = NULL, publishedFingerprint = NULL, rankingOf = NULL " +
            "WHERE ownerUid = :owner",
    )
    suspend fun makeGuestBoards(owner: String): Int

    @Query("DELETE FROM board_sync WHERE account = :account")
    suspend fun forgetSyncRecordsOf(account: String)

    @Query("DELETE FROM picture_sync WHERE account = :account")
    suspend fun forgetPicturesOf(account: String)

    /**
     * A deleted account's boards, trash and all, become this phone's own, as
     * "Delete account" promises: its uid is never signed into again, and
     * boards left under it could not be reached by anyone.
     */
    @Transaction
    suspend fun keepBoardsOfDeleted(account: String): Int {
        forgetSyncRecordsOf(account)
        forgetPicturesOf(account)
        return makeGuestBoards(account)
    }
}
