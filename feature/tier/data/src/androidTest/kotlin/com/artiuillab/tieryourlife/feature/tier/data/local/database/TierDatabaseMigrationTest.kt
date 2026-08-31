package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // A list that predates categories keeps its rows; it simply has no category
    // until its owner picks one, which is what blocks publishing rather than a
    // silent default.
    @Test
    fun migrate_3_to_4_adds_category_and_cover_without_touching_existing_lists() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName) " +
                    "VALUES (1, 'Films', NULL, 'WRAP', 'published-1', 'Olena M.')",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        val cursor = migratedDb.query(
            "SELECT title, publishedId, authorName, category, coverImageUrl FROM tier_lists WHERE id = 1",
        )
        cursor.use {
            check(it.moveToFirst())
            assertEquals("Films", it.getString(0))
            assertEquals("published-1", it.getString(1))
            assertEquals("Olena M.", it.getString(2))
            assertEquals(true, it.isNull(3))
            assertEquals(true, it.isNull(4))
        }
    }

    @Test
    fun migrate_4_to_5_gives_every_row_its_own_stable_id_and_keeps_the_data() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName, " +
                    "category, coverImageUrl) VALUES (1, 'Films', NULL, 'WRAP', NULL, NULL, NULL, NULL)",
            )
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName, " +
                    "category, coverImageUrl) VALUES (2, 'Games', NULL, 'WRAP', NULL, NULL, NULL, NULL)",
            )
            execSQL(
                "INSERT INTO tiers (id, tierListId, position, label, colorLight, colorDark, isPool, caption) " +
                    "VALUES (1, 1, 0, 'S', '#B03A32', '#F1948C', 0, NULL)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl, source, deletedAt) " +
                    "VALUES (1, 1, 0, 'Arrival', NULL, 'MANUAL', NULL)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl, source, deletedAt) " +
                    "VALUES (2, 1, 1, 'Moonlight', NULL, 'MANUAL', NULL)",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // The titles are still there: this only adds a name, it moves nothing.
        migratedDb.query("SELECT title FROM tier_lists ORDER BY id").use {
            check(it.moveToFirst())
            assertEquals("Films", it.getString(0))
            check(it.moveToNext())
            assertEquals("Games", it.getString(0))
        }

        listOf("tier_lists" to 2, "tiers" to 1, "tier_items" to 2).forEach { (table, rows) ->
            val ids = migratedDb.query("SELECT uid FROM $table").use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { it.getString(0) }
                    .toList()
            }
            assertEquals("$table should have kept its rows", rows, ids.size)
            assertTrue("$table left a row without an id", ids.none { it.isEmpty() })
            assertEquals("$table handed out the same id twice", ids.size, ids.distinct().size)
        }
    }

    @Test
    fun migrate_5_to_6_makes_room_for_the_account_copy_without_moving_anything() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName, " +
                    "category, coverImageUrl, uid) " +
                    "VALUES (1, 'Films', NULL, 'WRAP', NULL, NULL, NULL, NULL, 'board-1')",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        migratedDb.query("SELECT title, uid, arrivedFrom FROM tier_lists WHERE id = 1").use {
            check(it.moveToFirst())
            assertEquals("Films", it.getString(0))
            assertEquals("board-1", it.getString(1))
            // Nothing arrived from anywhere: this board has only ever been here.
            assertEquals(true, it.isNull(2))
        }

        // The table has to be empty rather than absent. A phone that has never
        // synced must read as "never sent", not as "thrown away".
        migratedDb.query("SELECT COUNT(*) FROM board_sync").use {
            check(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate_6_to_7_remembers_nothing_and_loses_nothing() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName, " +
                    "category, coverImageUrl, uid, arrivedFrom) " +
                    "VALUES (1, 'Films', NULL, 'WRAP', NULL, NULL, NULL, NULL, 'board-1', NULL)",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        migratedDb.query("SELECT title FROM tier_lists WHERE id = 1").use {
            check(it.moveToFirst())
            assertEquals("Films", it.getString(0))
        }

        // Empty, not absent. A phone that has never sent a picture must read
        // as "none sent yet" rather than as a broken database.
        migratedDb.query("SELECT COUNT(*) FROM picture_sync").use {
            check(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    // The point of doing this in SQL: there are dozens of places that edit a
    // board, and the database cannot forget any of them.
    @Test
    fun migrate_7_to_8_stamps_a_board_whenever_anything_in_it_changes() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName, " +
                    "category, coverImageUrl, uid, arrivedFrom) " +
                    "VALUES (1, 'Films', NULL, 'WRAP', NULL, NULL, NULL, NULL, 'board-1', NULL)",
            )
            execSQL(
                "INSERT INTO tiers (id, tierListId, position, label, colorLight, colorDark, isPool, " +
                    "caption, uid) VALUES (1, 1, 0, 'S', '#B03A32', '#F1948C', 0, NULL, 'tier-1')",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        // Every board that already existed is stamped, so nothing reads as
        // never having been touched.
        val backfilled = migratedDb.query("SELECT editedAt FROM tier_lists WHERE id = 1").use {
            check(it.moveToFirst())
            it.getLong(0)
        }
        assertTrue("a board that predates the column should still have an age", backfilled > 0)

        migratedDb.execSQL("UPDATE tier_lists SET editedAt = 1 WHERE id = 1")
        migratedDb.execSQL(
            "INSERT INTO tier_items (id, tierId, position, title, imageUrl, source, deletedAt, uid) " +
                "VALUES (1, 1, 0, 'Arrival', NULL, 'MANUAL', NULL, 'item-1')",
        )
        assertTrue("adding a card should age the board", editedAt(migratedDb) > 1)

        migratedDb.execSQL("UPDATE tier_lists SET editedAt = 1 WHERE id = 1")
        migratedDb.execSQL("UPDATE tier_items SET position = 3 WHERE id = 1")
        assertTrue("moving a card should age the board", editedAt(migratedDb) > 1)

        migratedDb.execSQL("UPDATE tier_lists SET editedAt = 1 WHERE id = 1")
        migratedDb.execSQL("DELETE FROM tier_items WHERE id = 1")
        assertTrue("removing a card should age the board", editedAt(migratedDb) > 1)

        migratedDb.execSQL("UPDATE tier_lists SET editedAt = 1 WHERE id = 1")
        migratedDb.execSQL("UPDATE tier_lists SET title = 'Shows' WHERE id = 1")
        assertTrue("renaming the board should age it", editedAt(migratedDb) > 1)
    }

    /**
     * A board published before this column existed says nothing about what was
     * sent, and null is the right answer: claiming its published copy is
     * behind would send somebody to republish a list that was already right.
     */
    @Test
    fun migration8To9_leavesAnAlreadyPublishedBoardSayingNothingAboutWhatWasSent() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                "INSERT INTO tier_lists (id, title, deletedAt, displayMode, publishedId, authorName, " +
                    "category, coverImageUrl, uid, arrivedFrom, editedAt) " +
                    "VALUES (1, 'Films', NULL, 'WRAP', 'published-1', NULL, NULL, NULL, 'board-1', NULL, 1)",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        migratedDb.query("SELECT publishedId, publishedFingerprint FROM tier_lists WHERE id = 1").use {
            check(it.moveToFirst())
            assertEquals("published-1", it.getString(0))
            assertTrue("nothing is known about what was sent", it.isNull(1))
        }
    }

    private fun editedAt(db: androidx.sqlite.db.SupportSQLiteDatabase): Long =
        db.query("SELECT editedAt FROM tier_lists WHERE id = 1").use {
            check(it.moveToFirst())
            it.getLong(0)
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
