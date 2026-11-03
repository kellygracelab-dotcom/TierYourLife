package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.Entity

/**
 * A picture this phone has already sent up, and to which account.
 *
 * Without it every sync would re-upload every picture somebody owns, which is
 * the one part of a board that costs real bytes. Asking Storage what it
 * already holds would answer the same question over the network, once per
 * picture, every time. Per account, since each keeps its own pictures: sent
 * to one is not sent to the next.
 */
@Entity(tableName = "picture_sync", primaryKeys = ["account", "pictureId"])
data class PictureSyncEntity(
    /** By `Account.SignedIn.recordKey`. */
    val account: String,
    val pictureId: String,
    val uploadedAt: Long,
)
