package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

/** Under the oldest SQLite's limit of bound arguments in one statement. */
private const val UIDS_PER_STATEMENT = 500

/** Boards made without an account: moved into one at a sign-in, or held for one whose session ended. */
@Dao
interface GuestBoardsDao {

    /** Trash left out: what the question about them counts. Held boards are among them; the caller leaves those out. */
    @Query("SELECT * FROM tier_lists WHERE ownerUid IS NULL AND deletedAt IS NULL")
    suspend fun guestBoardsInUse(): List<TierListEntity>

    @Query("SELECT uid FROM tier_lists WHERE ownerUid IS NULL")
    suspend fun guestBoardUids(): List<String>

    @Query("SELECT uid FROM tier_lists WHERE ownerUid = :account")
    suspend fun boardsOwnedBy(account: String): List<String>

    @Query("SELECT uid FROM tier_lists")
    suspend fun everyBoardUid(): List<String>

    /** Records, published ids and rankings stay: the account they belong to may take the boards back. */
    @Query("UPDATE tier_lists SET ownerUid = NULL WHERE ownerUid = :account")
    suspend fun releaseBoardsOf(account: String): Int

    @Query("UPDATE tier_lists SET ownerUid = :owner WHERE ownerUid IS NULL AND uid IN (:uids)")
    suspend fun giveGuestBoardsTo(owner: String, uids: List<String>): Int

    /** The trash goes with them: it is part of the same library. [held] stay, whoever signs in. */
    @Transaction
    suspend fun moveGuestBoardsTo(owner: String, held: Set<String>): Int =
        giveBack(owner, guestBoardUids().filterNot { it in held })

    /** Only those still without an owner: one moved in elsewhere since stays where it went. */
    @Transaction
    suspend fun giveBack(owner: String, uids: Collection<String>): Int =
        uids.chunked(UIDS_PER_STATEMENT).sumOf { giveGuestBoardsTo(owner, it) }
}
