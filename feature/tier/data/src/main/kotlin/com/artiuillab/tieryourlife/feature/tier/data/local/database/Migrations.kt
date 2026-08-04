package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// These captions were hardcoded in the presentation layer before tiers carried
// their own caption column; this backfills existing rows by the same label match.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tiers ADD COLUMN caption TEXT")
        db.execSQL("UPDATE tiers SET caption = 'Masterpiece' WHERE label = 'S'")
        db.execSQL("UPDATE tiers SET caption = 'Great' WHERE label = 'A'")
        db.execSQL("UPDATE tiers SET caption = 'Good' WHERE label = 'B'")
        db.execSQL("UPDATE tiers SET caption = 'Watchable' WHERE label = 'C'")
        db.execSQL("UPDATE tiers SET caption = 'No' WHERE label = 'D'")
    }
}

// New columns default to NULL, so every existing row is "alive" without a backfill.
// View SQL must match Room's generated schema exactly or migration validation fails.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE tier_items ADD COLUMN deletedAt INTEGER")
        db.execSQL("CREATE VIEW `active_tier_lists` AS SELECT * FROM tier_lists WHERE deletedAt IS NULL")
        db.execSQL("CREATE VIEW `active_tier_items` AS SELECT * FROM tier_items WHERE deletedAt IS NULL")
    }
}

// Every existing list keeps rendering exactly as it does today: WRAP is both the new
// column's default and the value already-live lists get backfilled to. active_tier_lists
// is defined as `SELECT * FROM tier_lists`, and SQLite re-resolves that `*` against the
// table's current columns on every read, so the view exposes displayMode automatically —
// its own CREATE VIEW text does not need to change.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tier_lists ADD COLUMN displayMode TEXT NOT NULL DEFAULT 'WRAP'")
    }
}
