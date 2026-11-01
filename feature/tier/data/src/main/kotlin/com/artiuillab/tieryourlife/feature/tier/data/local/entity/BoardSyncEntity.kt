package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.Entity

/**
 * What this phone and one account last agreed about one board. No foreign
 * key on purpose: the row has to outlive the board, being the only way the
 * next run tells "thrown away" from "never sent".
 */
@Entity(tableName = "board_sync", primaryKeys = ["account", "listUid"])
data class BoardSyncEntity(
    /**
     * Whose agreement it is, by `Account.SignedIn.recordKey`: a row read for
     * another account turned a board gone from this phone into a delete there.
     * [UNCLAIMED] for a row from before rows were told apart.
     */
    val account: String,
    val listUid: String,
    /** The account's revision at the moment the two agreed. */
    val revision: Int,
    /** What the board looked like then. Compared, never read. */
    val fingerprint: String,
    val syncedAt: Long,
) {
    companion object {
        const val UNCLAIMED = ""
    }
}
