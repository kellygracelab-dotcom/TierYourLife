package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What this phone and the account last agreed about one board.
 *
 * Deliberately not a column on `tier_lists` and deliberately without a foreign
 * key: the row has to outlive the board. A board emptied out of the trash here
 * leaves nothing behind to compare, and this row is the only way the next run
 * can tell "thrown away" from "never sent" -- without it a delete comes
 * straight back from the account.
 */
@Entity(tableName = "board_sync")
data class BoardSyncEntity(
    @PrimaryKey
    val listUid: String,
    /** The account's revision at the moment the two agreed. */
    val revision: Int,
    /** What the board looked like then. Compared, never read. */
    val fingerprint: String,
    val syncedAt: Long,
)
