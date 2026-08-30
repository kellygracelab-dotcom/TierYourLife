package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A picture this phone has already sent up.
 *
 * Without it every sync would re-upload every picture somebody owns, which is
 * the one part of a board that costs real bytes. Asking Storage what it
 * already holds would answer the same question over the network, once per
 * picture, every time.
 */
@Entity(tableName = "picture_sync")
data class PictureSyncEntity(
    @PrimaryKey
    val pictureId: String,
    val uploadedAt: Long,
)
