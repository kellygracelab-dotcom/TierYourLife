package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TierDatabase::class.java,
    )

    @Test
    fun migrate_1_to_2_backfills_source_from_image_url_and_keeps_existing_data() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode) " +
                    "VALUES (1, 'Films', NULL, 'WRAP')",
            )
            execSQL(
                "INSERT INTO tiers (id, tierListId, position, label, colorLight, colorDark, isPool, caption) " +
                    "VALUES (1, 1, 0, 'S', '#B03A32', '#F1948C', 0, NULL)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl, deletedAt) " +
                    "VALUES (1, 1, 0, 'Interstellar', 'https://image.tmdb.org/t/p/w500/poster.jpg', NULL)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl, deletedAt) " +
                    "VALUES (2, 1, 1, 'Hand-picked poster', '/data/user/0/app/files/tier_images/local.jpg', NULL)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl, deletedAt) " +
                    "VALUES (3, 1, 2, 'No poster yet', NULL, NULL)",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val itemsCursor = migratedDb.query(
            "SELECT id, title, imageUrl, source FROM tier_items ORDER BY id",
        )
        val items = itemsCursor.use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }.map {
                MigratedItem(
                    id = it.getLong(0),
                    title = it.getString(1),
                    imageUrl = if (it.isNull(2)) null else it.getString(2),
                    source = it.getString(3),
                )
            }.toList()
        }

        assertEquals(3, items.size)
        assertEquals(
            MigratedItem(1, "Interstellar", "https://image.tmdb.org/t/p/w500/poster.jpg", "TMDB"),
            items[0],
        )
        assertEquals(
            MigratedItem(2, "Hand-picked poster", "/data/user/0/app/files/tier_images/local.jpg", "MANUAL"),
            items[1],
        )
        assertEquals(MigratedItem(3, "No poster yet", null, "MANUAL"), items[2])

        val tierListCursor = migratedDb.query("SELECT title FROM tier_lists WHERE id = 1")
        val tierListTitle = tierListCursor.use { cursor ->
            check(cursor.moveToFirst())
            cursor.getString(0)
        }
        assertEquals("Films", tierListTitle)

        val tierCursor = migratedDb.query("SELECT label FROM tiers WHERE id = 1")
        val tierLabel = tierCursor.use { cursor ->
            check(cursor.moveToFirst())
            cursor.getString(0)
        }
        assertEquals("S", tierLabel)
    }

    private data class MigratedItem(
        val id: Long,
        val title: String,
        val imageUrl: String?,
        val source: String,
    )

    private companion object {
        const val TEST_DB = "tier-database-migration-test"
    }
}
