package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
