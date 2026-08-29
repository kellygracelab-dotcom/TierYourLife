package com.artiuillab.tieryourlife.feature.tier.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tiers",
    foreignKeys = [
        ForeignKey(
            entity = TierListEntity::class,
            parentColumns = ["id"],
            childColumns = ["tierListId"],
            onDelete = CASCADE,
        ),
    ],
    indices = [
        Index("tierListId"),
        Index(value = ["uid"], unique = true),
    ],
)
data class TierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tierListId: Long,
    val position: Int,
    val label: String,
    val colorLight: String,
    val colorDark: String,
    val isPool: Boolean = false,
    val caption: String? = null,
    /**
     * Stable across devices, unlike [id], which is this database's own
     * counter and would collide the moment two phones are involved.
     */
    @ColumnInfo(defaultValue = "''")
    val uid: String = UUID.randomUUID().toString(),
)
