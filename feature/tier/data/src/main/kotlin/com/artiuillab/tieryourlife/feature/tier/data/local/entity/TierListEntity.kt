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
    /** The chosen collage, one web address per line; null while the cover is one picture or left to the cards. */
    val coverImageUrls: String? = null,
    /** The community topic once the server named it, and the name beside it so no screen needs a second read. */
    val topicId: String? = null,
    val topicName: String? = null,
    /** A topic the author named that has not been published yet. */
    val newTopicName: String? = null,
    /** The published list this copy was taken from. */
    val takenFrom: String? = null,
    /** The list this board is out as a ranking of; null while it is a list of its own, or private. */
    val rankingOf: String? = null,
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
    /** Published with no name or face. A column of its own, so it syncs and survives a change of phone. */
    @ColumnInfo(defaultValue = "0")
    val anonymous: Boolean = false,
    /**
     * The saved Play game, as the account sent it: JSON this phone does not
     * read yet, only keeps and sends back untouched, since the account stores
     * a board as it is written and would otherwise lose the game.
     */
    val play: String? = null,
)
