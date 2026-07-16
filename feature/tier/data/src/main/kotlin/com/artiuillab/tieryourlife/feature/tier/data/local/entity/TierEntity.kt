package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tiers",
    foreignKeys = [
        ForeignKey(
            entity = TierListEntity::class,
            parentColumns = ["id"],
            childColumns = ["tierListId"],
            onDelete = CASCADE,
        )
    ],
    indices = [
        Index("tierListId")
    ]
)
data class TierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tierListId: Long,
    val position: Int,
    val label: String,
    val color: String
)
