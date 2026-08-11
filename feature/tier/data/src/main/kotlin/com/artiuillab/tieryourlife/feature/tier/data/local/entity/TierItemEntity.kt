package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tier_items",
    foreignKeys = [
        ForeignKey(
            entity = TierEntity::class,
            parentColumns = ["id"],
            childColumns = ["tierId"],
            onDelete = CASCADE,
        )
    ],
    indices = [
        Index("tierId")
    ]
)
data class TierItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tierId: Long,
    val position: Int,
    val title: String,
    val imageUrl: String?,
    @ColumnInfo(defaultValue = "'MANUAL'")
    val source: String = "MANUAL",
    val deletedAt: Long? = null,
)
