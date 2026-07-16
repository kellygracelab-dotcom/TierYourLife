package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import org.junit.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDaoTest {

    private lateinit var database: TierDatabase

    private lateinit var dao: TierDao

    private lateinit var list: TierListEntity


    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            TierDatabase::class.java,
        ).build()

        dao = database.tierDao()

        list = TierListEntity(
            title = "Films"
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_tier_list_and_read_it_by_generated_id_returns_saved_data() {
        runBlocking {
            val generatedId = dao.insertTierList(list)
            assertTrue(generatedId > 0)
            val loadedList = dao.getTierListById(generatedId)
            assertNotNull(loadedList)
            assertEquals(generatedId, loadedList?.id)
            assertEquals(list.title, loadedList?.title)
        }
    }

    @Test
    fun get_tiers_by_list_id_returns_tiers_ordered_by_position() {
        runBlocking {
            val generatedId = dao.insertTierList(list)
            val list2 = TierListEntity(
                title = "Games"
            )
            val generatedIdList2 = dao.insertTierList(list2)
            val tier1 = TierEntity(
                tierListId = generatedId,
                position = 1,
                label = "S",
                color = "GOLD"
            )
            val tier2 = TierEntity(
                tierListId = generatedId,
                position = 2,
                label = "A",
                color = "GREEN"
            )
            val tier3 = TierEntity(
                tierListId = generatedIdList2,
                position = 2,
                label = "B",
                color = "RED"
            )

            dao.insertTier(tier2)
            dao.insertTier(tier1)
            dao.insertTier(tier3)

            val tiers1 = dao.getAllTiersByTierListId(generatedId)
            val tiers2 = dao.getAllTiersByTierListId(generatedIdList2)

            assertEquals(2, tiers1.size)
            assertEquals(1, tiers2.size)

            assertEquals(1, tiers1[0].position)
            assertEquals(2, tiers1[1].position)

            assertTrue(tiers1.all { it.tierListId == generatedId })
            assertTrue(tiers2.all { it.tierListId == generatedIdList2 })
        }
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_tier_with_nonexistent_tier_list_id_throws_constraint_exception() {
        runBlocking {
            val tier = TierEntity(
                tierListId = 12651234,
                position = 1,
                label = "S",
                color = "GOLD"
            )
            dao.insertTier(tier)
        }
    }

    @Test
    fun delete_tier_list_cascade_deletes_its_tiers() {
        runBlocking {
            val deletedId = dao.insertTierList(list)

            val tier1 = TierEntity(
                tierListId = deletedId,
                position = 1,
                label = "S",
                color = "GOLD"
            )

            dao.insertTier(tier1)
            val deletedRows = dao.deleteTierListById(deletedId)
            assertEquals(1, deletedRows)

            assertEquals(null, dao.getTierListById(deletedId))

            assertTrue(dao.getAllTiersByTierListId(deletedId).isEmpty())
        }
    }

    @Test
    fun delete_tier_cascade_deletes_only_its_items() {
        runBlocking {
            val generatedId = dao.insertTierList(list)
            val tier1 = TierEntity(
                tierListId = generatedId,
                position = 1,
                label = "S",
                color = "GOLD"
            )
            val tier2 = TierEntity(
                tierListId = generatedId,
                position = 2,
                label = "A",
                color = "GREEN"
            )

            val tier1Id = dao.insertTier(tier1)
            val tier2Id = dao.insertTier(tier2)

            val tierItem1 = TierItemEntity(
                tierId = tier1Id,
                position = 1,
                title = "The Shawshank Redemption",
                imageUrl = "https://example.com/shawshank.jpg"
            )
            val tierItem2 = TierItemEntity(
                tierId = tier2Id,
                position = 1,
                title = "The Matrix",
                imageUrl = "https://example.com/matrix.jpg"
            )

            dao.insertTierItem(tierItem1)
            dao.insertTierItem(tierItem2)

            val deletedRows = dao.deleteTierById(tier1Id)

            assertEquals(1, deletedRows)

            val deletedTierItems = dao.getAllTierItemsByTierId(tier1Id)
            val remainingTierItems = dao.getAllTierItemsByTierId(tier2Id)
            val remainingTiers = dao.getAllTiersByTierListId(generatedId)

            assertTrue(deletedTierItems.isEmpty())
            assertEquals(1, remainingTierItems.size)
            assertEquals(tierItem2.title, remainingTierItems.single().title)
            assertEquals(listOf(tier2Id), remainingTiers.map { it.id })
        }
    }
}
