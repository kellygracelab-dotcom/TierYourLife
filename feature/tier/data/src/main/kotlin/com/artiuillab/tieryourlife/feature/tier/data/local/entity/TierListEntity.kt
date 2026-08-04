package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tier_lists")
data class TierListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    // Soft delete: epoch millis when the list was moved to trash, null = alive.
    val deletedAt: Long? = null,
    // Holds a TierListDisplayMode's own name, not its ordinal: a future fourth mode is
    // just a new name, so it can never relabel rows written under today's ordering.
    // Unrecognized values are mapped back to WRAP by TierListMapper instead of failing.
    @ColumnInfo(defaultValue = "'WRAP'")
    val displayMode: String = "WRAP",
)
