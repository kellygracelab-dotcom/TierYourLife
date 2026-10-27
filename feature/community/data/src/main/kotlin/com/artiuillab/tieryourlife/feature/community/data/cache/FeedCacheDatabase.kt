package com.artiuillab.tieryourlife.feature.community.data.cache

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

/** One feed's first page as the server sent it, and when it came and was last looked at. */
@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val fetchedAt: Long,
    val openedAt: Long,
)

@Dao
interface FeedCacheDao {

    @Query("SELECT * FROM feed_cache WHERE `key` = :key")
    suspend fun get(key: String): FeedCacheEntity?

    @Upsert
    suspend fun put(entity: FeedCacheEntity)

    @Query("UPDATE feed_cache SET openedAt = :at WHERE `key` = :key")
    suspend fun touch(key: String, at: Long)

    @Query("DELETE FROM feed_cache WHERE openedAt < :before")
    suspend fun dropOpenedBefore(before: Long)
}

/**
 * Its own small database rather than a table in the boards' one: nothing here
 * is anybody's work, so a version change may simply start it empty.
 */
@Database(entities = [FeedCacheEntity::class], version = 1, exportSchema = true)
abstract class FeedCacheDatabase : RoomDatabase() {
    abstract fun feedCache(): FeedCacheDao
}
