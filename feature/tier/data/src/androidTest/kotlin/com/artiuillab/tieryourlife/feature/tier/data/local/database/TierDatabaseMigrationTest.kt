package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val TEST_DB = "migration-test"

class TierDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TierDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesExistingRowsAndBackfillsDefaultCaptions() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO tier_lists (id, title) VALUES (1, 'Films')")
            execSQL(
                "INSERT INTO tiers (id, tierListId, position, label, colorLight, colorDark, isPool) VALUES " +
                    "(1, 1, 0, 'S', '#B03A32', '#F1948C', 0)," +
                    "(2, 1, 1, 'A', '#C06A25', '#E9A867', 0)," +
                    "(3, 1, 2, 'B', '#A98B1F', '#D8C05A', 0)," +
                    "(4, 1, 3, 'C', '#3F7F55', '#7FC393', 0)," +
                    "(5, 1, 4, 'D', '#3C6E99', '#86B8DE', 0)," +
                    "(6, 1, 5, 'Unranked', '#DAD7E0', '#46464F', 1)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl) VALUES " +
                    "(1, 1, 0, 'Interstellar', NULL)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val listCursor = migrated.query("SELECT title FROM tier_lists WHERE id = 1")
        listCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Films", it.getString(it.getColumnIndexOrThrow("title")))
        }

        val itemCursor = migrated.query("SELECT title FROM tier_items WHERE id = 1")
        itemCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Interstellar", it.getString(it.getColumnIndexOrThrow("title")))
        }

        val captionCursor = migrated.query("SELECT label, caption FROM tiers ORDER BY position ASC")
        captionCursor.use {
            val actual = generateSequence { if (it.moveToNext()) it else null }
                .map { row ->
                    val label = row.getString(row.getColumnIndexOrThrow("label"))
                    val captionIndex = row.getColumnIndexOrThrow("caption")
                    val caption = if (row.isNull(captionIndex)) null else row.getString(captionIndex)
                    label to caption
                }
                .toList()

            assertEquals(
                listOf(
                    "S" to "Masterpiece",
                    "A" to "Great",
                    "B" to "Good",
                    "C" to "Watchable",
                    "D" to "No",
                    "Unranked" to null,
                ),
                actual,
            )
        }
    }

    @Test
    fun migrate2To3_preservesExistingRowsAndLeavesThemAllAlive() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO tier_lists (id, title) VALUES (1, 'Films')")
            execSQL(
                "INSERT INTO tiers (id, tierListId, position, label, caption, colorLight, colorDark, isPool) VALUES " +
                    "(1, 1, 0, 'S', 'Masterpiece', '#B03A32', '#F1948C', 0)," +
                    "(2, 1, 1, 'Unranked', NULL, '#DAD7E0', '#46464F', 1)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl) VALUES " +
                    "(1, 1, 0, 'Interstellar', NULL)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        val listCursor = migrated.query("SELECT title, deletedAt FROM active_tier_lists WHERE id = 1")
        listCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Films", it.getString(it.getColumnIndexOrThrow("title")))
            assertEquals(true, it.isNull(it.getColumnIndexOrThrow("deletedAt")))
        }

        val itemCursor = migrated.query("SELECT title, deletedAt FROM active_tier_items WHERE id = 1")
        itemCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Interstellar", it.getString(it.getColumnIndexOrThrow("title")))
            assertEquals(true, it.isNull(it.getColumnIndexOrThrow("deletedAt")))
        }

        val tierCursor = migrated.query("SELECT caption FROM tiers WHERE id = 1")
        tierCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Masterpiece", it.getString(it.getColumnIndexOrThrow("caption")))
        }
    }

    @Test
    fun migrate3To4_preservesExistingRowsAndSetsWrapDisplayModeOnEveryList() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO tier_lists (id, title) VALUES (1, 'Films'), (2, 'Games')")
            execSQL(
                "INSERT INTO tiers (id, tierListId, position, label, caption, colorLight, colorDark, isPool) VALUES " +
                    "(1, 1, 0, 'S', 'Masterpiece', '#B03A32', '#F1948C', 0)," +
                    "(2, 1, 1, 'Unranked', NULL, '#DAD7E0', '#46464F', 1)",
            )
            execSQL(
                "INSERT INTO tier_items (id, tierId, position, title, imageUrl) VALUES " +
                    "(1, 1, 0, 'Interstellar', NULL)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        val listCursor = migrated.query("SELECT title, displayMode FROM active_tier_lists ORDER BY id ASC")
        listCursor.use {
            assertEquals(2, it.count)
            val rows = generateSequence { if (it.moveToNext()) it else null }
                .map { row ->
                    row.getString(row.getColumnIndexOrThrow("title")) to
                        row.getString(row.getColumnIndexOrThrow("displayMode"))
                }
                .toList()
            assertEquals(listOf("Films" to "WRAP", "Games" to "WRAP"), rows)
        }

        val itemCursor = migrated.query("SELECT title FROM active_tier_items WHERE id = 1")
        itemCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Interstellar", it.getString(it.getColumnIndexOrThrow("title")))
        }

        val tierCursor = migrated.query("SELECT caption FROM tiers WHERE id = 1")
        tierCursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Masterpiece", it.getString(it.getColumnIndexOrThrow("caption")))
        }
    }
}
