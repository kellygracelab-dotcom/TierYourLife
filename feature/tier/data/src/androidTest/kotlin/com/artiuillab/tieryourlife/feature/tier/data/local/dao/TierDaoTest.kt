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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDaoTest {

    private lateinit var database: TierDatabase
    private lateinit var dao: TierDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TierDatabase::class.java,
        ).build()
        dao = database.tierDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_tier_list_and_read_it_by_generated_id_returns_saved_data() = runBlocking {
        val films = tierList()
        val generatedId = dao.insertTierList(films)

        val actual = dao.getTierListById(generatedId)

        assertTrue(generatedId > 0)
        assertEquals(films.copy(id = generatedId), actual)
    }

    @Test
    fun get_tiers_by_list_id_returns_tiers_ordered_by_position() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val gamesId = dao.insertTierList(tierList(title = "Games"))
        val sTier = tier(tierListId = filmsId, position = 1, label = "S", color = "GOLD")
        val aTier = tier(tierListId = filmsId, position = 2, label = "A", color = "GREEN")
        val bTier = tier(tierListId = gamesId, position = 1, label = "B", color = "RED")

        dao.insertTier(aTier)
        dao.insertTier(sTier)
        dao.insertTier(bTier)

        val filmTiers = dao.getAllTiersByTierListId(filmsId)
        val gameTiers = dao.getAllTiersByTierListId(gamesId)

        assertEquals(listOf("S", "A"), filmTiers.map { it.label })
        assertTrue(filmTiers.all { it.tierListId == filmsId })
        assertEquals(listOf("B"), gameTiers.map { it.label })
        assertTrue(gameTiers.all { it.tierListId == gamesId })
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_tier_with_nonexistent_tier_list_id_throws_constraint_exception() {
        runBlocking {
            dao.insertTier(
                tier(
                    tierListId = Long.MAX_VALUE,
                    position = 1,
                    label = "S",
                    color = "GOLD",
                ),
            )
        }
    }

    @Test
    fun delete_tier_list_cascade_deletes_its_tiers() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        dao.insertTier(
            tier(
                tierListId = filmsId,
                position = 1,
                label = "S",
                color = "GOLD",
            ),
        )

        val deletedRows = dao.deleteTierListById(filmsId)

        assertEquals(1, deletedRows)
        assertEquals(null, dao.getTierListById(filmsId))
        assertTrue(dao.getAllTiersByTierListId(filmsId).isEmpty())
    }

    @Test
    fun delete_tier_cascade_deletes_only_its_items() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val sTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", color = "GOLD"),
        )
        val aTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 2, label = "A", color = "GREEN"),
        )
        val sItem = tierItem(tierId = sTierId, title = "The Shawshank Redemption")
        val aItem = tierItem(tierId = aTierId, title = "The Matrix")
        dao.insertTierItem(sItem)
        dao.insertTierItem(aItem)

        val deletedRows = dao.deleteTierById(sTierId)

        val deletedTierItems = dao.getAllTierItemsByTierId(sTierId)
        val remainingTierItems = dao.getAllTierItemsByTierId(aTierId)
        val remainingTiers = dao.getAllTiersByTierListId(filmsId)
        assertEquals(1, deletedRows)
        assertTrue(deletedTierItems.isEmpty())
        assertEquals(listOf(aItem.title), remainingTierItems.map { it.title })
        assertEquals(listOf(aTierId), remainingTiers.map { it.id })
    }

    @Test
    fun get_tier_list_with_tiers_and_items_returns_full_graph() = runBlocking {
        val films = tierList()
        val filmsId = dao.insertTierList(films)
        val sTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", color = "GOLD"),
        )
        val aTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 2, label = "A", color = "GREEN"),
        )
        val sItem = tierItem(tierId = sTierId, title = "The Shawshank Redemption")
        val aItem = tierItem(tierId = aTierId, title = "The Matrix")
        dao.insertTierItem(sItem)
        dao.insertTierItem(aItem)

        val actual = requireNotNull(dao.getTierListWithTiers(filmsId))

        assertEquals(filmsId, actual.tierList.id)
        assertEquals(films.title, actual.tierList.title)
        assertEquals(setOf("S", "A"), actual.tiers.map { it.tier.label }.toSet())

        val actualSTier = actual.tiers.single { it.tier.label == "S" }
        val actualATier = actual.tiers.single { it.tier.label == "A" }
        assertEquals(sItem.title, actualSTier.items.single().title)
        assertEquals(aItem.title, actualATier.items.single().title)
    }

    private fun tierList(title: String = "Films"): TierListEntity = TierListEntity(
        title = title,
    )

    private fun tier(
        tierListId: Long,
        position: Int,
        label: String,
        color: String,
    ): TierEntity = TierEntity(
        tierListId = tierListId,
        position = position,
        label = label,
        color = color,
    )

    private fun tierItem(
        tierId: Long,
        title: String,
        position: Int = 1,
    ): TierItemEntity = TierItemEntity(
        tierId = tierId,
        position = position,
        title = title,
        imageUrl = "https://example.com/poster.jpg",
    )
}
