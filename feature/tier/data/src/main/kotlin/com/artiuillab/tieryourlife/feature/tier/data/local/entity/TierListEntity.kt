package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tier_lists")
data class TierListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "'WRAP'")
    val displayMode: String = "WRAP",
)
