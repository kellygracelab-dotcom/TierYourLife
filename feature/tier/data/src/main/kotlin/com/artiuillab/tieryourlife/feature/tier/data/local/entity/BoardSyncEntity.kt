package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What this phone and the account last agreed about one board. No foreign
 * key on purpose: the row has to outlive the board, being the only way the
 * next run tells "thrown away" from "never sent".
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
