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
     * What this board looked like the last time it was published.
     *
     * A published list is a snapshot and stays as it was published, which is
     * right -- somebody who opened it yesterday should not find a different
     * thing today. But the board goes on being edited, and without this there
     * is no way to know the two have parted: the only honest answer to "is the
     * published copy behind?" is to remember what was sent.
     *
     * Covers exactly what publishing sends, so changing the display mode --
     * which does not travel -- does not claim the copy is stale.
     */
    val publishedFingerprint: String? = null,
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
    /**
     * Set only on the copy kept after two phones edited the same board apart
     * from each other. Two boards with the same name and different insides are
     * unreadable without it; "from Pixel 7" is the whole difference.
     */
    val arrivedFrom: String? = null,
    /**
     * When anything about this board last changed, written by triggers rather
     * than by the code that does the changing -- there are dozens of those and
     * one forgotten call is a board with the wrong age on the one screen where
     * two copies have to be told apart.
     */
    val editedAt: Long? = null,
)
