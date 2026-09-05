package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Starred boards. A time rather than a boolean: null is "not starred", and the time orders the starred ones for free. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN favouritedAt INTEGER")
    }
}

/**
 * Stamped by SQL triggers rather than at every edit site: the first site
 * forgotten is a board whose age is quietly wrong. The board's trigger names
 * its columns because Room enables recursive triggers, and one firing on any
 * update would fire on the stamp it had just written.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN publishedFingerprint TEXT")
        // Null on boards already published: the screen says nothing rather
        // than claim a copy is stale when it is not.
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN editedAt INTEGER")
        db.execSQL("UPDATE tier_lists SET editedAt = CAST(strftime('%s','now') AS INTEGER) * 1000")

        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS tier_lists_edited
            AFTER UPDATE OF title, deletedAt, displayMode, category, coverImageUrl, authorName, arrivedFrom
            ON tier_lists
            BEGIN
                UPDATE tier_lists SET editedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
                WHERE id = NEW.id;
            END
            """.trimIndent(),
        )
        listOf("INSERT" to "NEW", "UPDATE" to "NEW", "DELETE" to "OLD").forEach { (verb, row) ->
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tiers_edited_${verb.lowercase()} AFTER $verb ON tiers
                BEGIN
                    UPDATE tier_lists SET editedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
                    WHERE id = $row.tierListId;
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tier_items_edited_${verb.lowercase()} AFTER $verb ON tier_items
                BEGIN
                    UPDATE tier_lists SET editedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
                    WHERE id = (SELECT tierListId FROM tiers WHERE id = $row.tierId);
                END
                """.trimIndent(),
            )
        }
    }
}

/** Which pictures have gone up. No foreign key: the row is about a file that outlives the card pointing at it. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `picture_sync` (" +
                "`pictureId` TEXT NOT NULL, `uploadedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`pictureId`))",
        )
    }
}

/**
 * The copy an account keeps. `board_sync` has no foreign key on purpose: the
 * row must outlive the board, being the only thing that tells "thrown away
 * here" from "never sent".
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN arrivedFrom TEXT")
        // Written the way Room writes it, so the migration test compares like with like.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `board_sync` (" +
                "`listUid` TEXT NOT NULL, `revision` INTEGER NOT NULL, " +
                "`fingerprint` TEXT NOT NULL, `syncedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`listUid`))",
        )
    }
}

/**
 * A uid per row that means the same on another device; primary keys are this
 * database's own counters, so two phones both hand out id 1. Backfilled in
 * SQL so a large library is not read into memory; randomblob is per row.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("tier_lists", "tiers", "tier_items").forEach { table ->
            db.execSQL("ALTER TABLE $table ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE $table SET uid = lower(hex(randomblob(16)))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uid ON $table (uid)")
        }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN category TEXT")
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN coverImageUrl TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN publishedId TEXT")
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN authorName TEXT")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_items ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
        db.execSQL("UPDATE tier_items SET source = 'TMDB' WHERE imageUrl LIKE 'http%'")
    }
}
