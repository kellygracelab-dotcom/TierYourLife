package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tier_lists",
    indices = [Index(value = ["uid"], unique = true)],
)
data class TierListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "'WRAP'")
    val displayMode: String = "WRAP",
    /** Set once this list has been published; the id the server keeps it under. */
    val publishedId: String? = null,
    /**
     * What was sent when last published: the only honest answer to "is the
     * published copy behind?". Covers exactly what publishing sends, so a
     * display-mode change does not claim the copy is stale.
     */
    val publishedFingerprint: String? = null,
    /** Set on a copy taken from someone else's published list. */
    val authorName: String? = null,
    val category: String? = null,
    val coverImageUrl: String? = null,
    /** Stable across devices, unlike [id], this database's own counter. */
    @ColumnInfo(defaultValue = "''")
    val uid: String = UUID.randomUUID().toString(),
    /** Set on the copy kept after two phones edited the same board apart; "from Pixel 7" is the whole difference. */
    val arrivedFrom: String? = null,
    /** Written by triggers, not by the code that changes things; see MIGRATION_8_9. */
    val editedAt: Long? = null,
    /**
     * A time, not a flag, so starred boards keep an order among themselves. Not
     * watched by the edited-at trigger: starring is not editing.
     */
    val favouritedAt: Long? = null,
)
