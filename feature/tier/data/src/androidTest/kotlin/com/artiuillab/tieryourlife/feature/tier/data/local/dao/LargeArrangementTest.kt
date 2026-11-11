package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Actual SQLite persistence, including reopen and a rejected stale arrangement. */
@RunWith(AndroidJUnit4::class)
class LargeArrangementTest {
    @Test
    fun twoThousandCardsApplyAtomically_andSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "qa-large-arrangement-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, TierDatabase::class.java, name).build()
        var db = open()
        try {
            var dao = db.tierDao()
            val id = dao.createTierListWithDefaultTier("QA 2000", listOf("Best", "Great", "Good", "Okay", "Worst"), "ann")
            val tiers = dao.getAllTiersByTierListId(id)
            val s = tiers.first { !it.isPool }
            val ids = db.withTransaction {
                (0 until 2000).map { dao.insertTierItem(TierItemEntity(tierId = s.id, position = it, title = "Card $it", imageUrl = null)) }
            }
            val before = tiers.associate { it.id to if (it.id == s.id) ids else emptyList() }
            val after = before + (s.id to ids.reversed())
            assertFalse(dao.arrangeBoard(id, "bob", before, after))
            assertFalse(dao.arrangeBoard(id, "ann", before, after + (s.id to ids.dropLast(1))))
            val started = System.nanoTime()
            assertTrue(dao.arrangeBoard(id, "ann", before, after))
            println("SQLite atomic reverse of 2000 cards: ${(System.nanoTime() - started) / 1_000_000} ms")
            assertFalse(dao.arrangeBoard(id, "ann", before, after))
            db.close()
            db = open()
            dao = db.tierDao()
            assertEquals(ids.reversed(), dao.getAllTierItemsByTierId(s.id).map { it.id })
            assertEquals((0 until 2000).toList(), dao.getAllTierItemsByTierId(s.id).map { it.position })
            assertTrue(dao.arrangeBoard(id, "ann", after, before))
            assertEquals(ids, dao.getAllTierItemsByTierId(s.id).map { it.id })
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER qa_fail_arrangement BEFORE UPDATE OF position ON tier_items " +
                    "WHEN NEW.position = 1000 BEGIN SELECT RAISE(ABORT, 'QA injected write failure'); END",
            )
            assertNotNull(runCatching { dao.arrangeBoard(id, "ann", before, after) }.exceptionOrNull())
            assertEquals(ids, dao.getAllTierItemsByTierId(s.id).map { it.id })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
