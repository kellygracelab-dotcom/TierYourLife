package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The account keeps 19 tiers on a board besides the pool; a 20th made the board refused on every run. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TierLimitTest {

    private lateinit var database: TierDatabase
    private lateinit var dao: TierDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, TierDatabase::class.java).build()
        dao = database.tierDao()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun boardWith(ranked: Int): Long {
        val board = dao.insertTierList(TierListEntity(title = "Films"))
        repeat(ranked + 1) { index ->
            dao.insertTier(TierEntity(tierListId = board, position = index, label = "T$index", colorLight = "#111111", colorDark = "#EEEEEE", isPool = index == ranked))
        }
        return board
    }

    @Test
    fun aTierFitsUpToTheLimit() = runBlocking {
        val board = boardWith(TierList.MAX_RANKED_TIERS - 1)

        val added = dao.addTier(board, "New", null, "#222222", "#DDDDDD")

        assertNotEquals(TierDao.NO_ROW, added)
        assertEquals(TierList.MAX_RANKED_TIERS + 1, dao.getAllTiersByTierListId(board).size)
    }

    @Test
    fun aFullBoardTakesNoMoreTiers() = runBlocking {
        val board = boardWith(TierList.MAX_RANKED_TIERS)

        val added = dao.addTier(board, "One too many", null, "#222222", "#DDDDDD")

        assertEquals(TierDao.NO_ROW, added)
        assertEquals(TierList.MAX_RANKED_TIERS + 1, dao.getAllTiersByTierListId(board).size)
    }
}
