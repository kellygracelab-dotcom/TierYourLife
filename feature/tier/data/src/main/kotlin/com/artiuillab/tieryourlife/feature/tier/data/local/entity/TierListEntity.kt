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
    /** Set on a copy taken from someone else's published list. */
    val authorName: String? = null,
    val category: String? = null,
    val coverImageUrl: String? = null,
    /**
     * Stable across devices, unlike [id], which is this database's own
     * counter and would collide the moment two phones are involved.
     */
    @ColumnInfo(defaultValue = "''")
    val uid: String = UUID.randomUUID().toString(),
)
