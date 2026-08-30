package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Records when a board was last touched, and does it in SQL.
 *
 * Triggers rather than a call at every place that edits something. A rename, a
 * drag, a colour, emptying a tier, restoring a card from the trash -- there are
 * dozens of them, and the first one anybody forgets is a board whose age is
 * quietly wrong on the one screen where two copies have to be told apart. The
 * database cannot forget.
 *
 * The board's own trigger names the columns it watches. Room turns recursive
 * triggers on, so a trigger that fired on any update would fire on the stamp it
 * had just written -- and within the same second the new value equals the old
 * one, so no comparison can stop it. Naming the columns does.
 */
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

/**
 * Remembers which pictures have already gone up.
 *
 * No foreign key here either, and for the mirror of the reason board_sync has
 * none: the row is about a file, and the card that used to point at it may be
 * long gone while the file is still up there waiting to be swept.
 */
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
 * Makes room for the copy an account keeps.
 *
 * `board_sync` has no foreign key on purpose: the row has to outlive the
 * board. A board emptied out of the trash leaves nothing behind to compare,
 * and this row is the only thing that can tell "thrown away here" from "never
 * sent" -- without it, a delete comes straight back from the account.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN arrivedFrom TEXT")
        // Written the way Room writes it, so the migration test compares like
        // with like rather than arguing about where the primary key is said.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `board_sync` (" +
                "`listUid` TEXT NOT NULL, `revision` INTEGER NOT NULL, " +
                "`fingerprint` TEXT NOT NULL, `syncedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`listUid`))",
        )
    }
}

/**
 * Gives every row a name that means the same thing on another device. The
 * primary keys are this database's own counters: two phones each hand out
 * id 1, so nothing can be matched up across them.
 *
 * Backfilled in SQL rather than in Kotlin so a large library does not have
 * to be read into memory during an upgrade. randomblob is evaluated per
 * row, which is what makes the values differ.
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
