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
        val sTier = tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C")
        val aTier = tier(tierListId = filmsId, position = 2, label = "A", colorLight = "#C06A25", colorDark = "#E9A867")
        val bTier = tier(tierListId = gamesId, position = 1, label = "B", colorLight = "#A98B1F", colorDark = "#D8C05A")

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
                    colorLight = "#B03A32",
                    colorDark = "#F1948C",
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
                colorLight = "#B03A32",
                colorDark = "#F1948C",
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
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val aTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 2, label = "A", colorLight = "#C06A25", colorDark = "#E9A867"),
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
    fun create_tier_list_with_default_tier_inserts_list_and_six_tiers() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        val savedList = dao.getTierListById(listId)
        val tiers = dao.getAllTiersByTierListId(listId)

        assertTrue(listId > 0)
        assertEquals("Films", savedList?.title)
        assertEquals(6, tiers.size)
        assertTrue(tiers.all { it.tierListId == listId })
    }

    @Test
    fun create_tier_list_with_default_tier_orders_tiers_s_to_d_then_pool() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        val tiers = dao.getAllTiersByTierListId(listId)

        assertEquals(listOf("S", "A", "B", "C", "D", "Unranked"), tiers.map { it.label })
    }

    @Test
    fun create_tier_list_with_default_tier_marks_exactly_one_pool_tier() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        val tiers = dao.getAllTiersByTierListId(listId)
        val poolTiers = tiers.filter { it.isPool }

        assertEquals(1, poolTiers.size)
        assertEquals("Unranked", poolTiers.single().label)
    }

    @Test
    fun add_movie_to_pool_puts_first_item_at_position_zero() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolTierId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id

        dao.addMovieToPool(
            tierListId = listId,
            title = "Interstellar",
            imageUrl = "https://example.com/interstellar.jpg",
        )

        val poolItem = dao.getAllTierItemsByTierId(poolTierId).single()
        assertEquals("Interstellar", poolItem.title)
        assertEquals("https://example.com/interstellar.jpg", poolItem.imageUrl)
        assertEquals(0, poolItem.position)
    }

    @Test
    fun add_movie_to_pool_appends_items_with_increasing_positions() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolTierId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id

        dao.addMovieToPool(tierListId = listId, title = "Interstellar", imageUrl = null)
        dao.addMovieToPool(tierListId = listId, title = "Arrival", imageUrl = null)

        val poolItems = dao.getAllTierItemsByTierId(poolTierId)
        assertEquals(listOf("Interstellar", "Arrival"), poolItems.map { it.title })
        assertEquals(listOf(0, 1), poolItems.map { it.position })
    }

    @Test
    fun add_movie_to_pool_leaves_ranked_tiers_empty() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        dao.addMovieToPool(tierListId = listId, title = "Interstellar", imageUrl = null)

        val rankedTiers = dao.getAllTiersByTierListId(listId).filterNot { it.isPool }
        assertTrue(rankedTiers.all { dao.getAllTierItemsByTierId(it.id).isEmpty() })
    }

    @Test
    fun get_tier_list_with_tiers_and_items_returns_full_graph() = runBlocking {
        val films = tierList()
        val filmsId = dao.insertTierList(films)
        val sTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val aTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 2, label = "A", colorLight = "#C06A25", colorDark = "#E9A867"),
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

    @Test
    fun move_item_from_pool_to_ranked_tier_relocates_item() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolTierId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val itemId = dao.addMovieToPool(tierListId = listId, title = "Interstellar", imageUrl = null)

        dao.moveItem(itemId = itemId, toTierId = sTierId, toPosition = 0)

        assertTrue(dao.getAllTierItemsByTierId(poolTierId).isEmpty())
        val sItems = dao.getAllTierItemsByTierId(sTierId)
        assertEquals(listOf(itemId), sItems.map { it.id })
        assertEquals(listOf(0), sItems.map { it.position })
    }

    @Test
    fun move_item_from_ranked_tier_to_pool_relocates_item() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolTierId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val itemId = dao.insertTierItem(tierItem(tierId = sTierId, title = "Interstellar", position = 0))

        dao.moveItem(itemId = itemId, toTierId = poolTierId, toPosition = 0)

        assertTrue(dao.getAllTierItemsByTierId(sTierId).isEmpty())
        val poolItems = dao.getAllTierItemsByTierId(poolTierId)
        assertEquals(listOf(itemId), poolItems.map { it.id })
        assertEquals(listOf(0), poolItems.map { it.position })
    }

    @Test
    fun move_item_between_two_ranked_tiers_relocates_item() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val itemId = dao.insertTierItem(tierItem(tierId = sTierId, title = "Interstellar", position = 0))

        dao.moveItem(itemId = itemId, toTierId = aTierId, toPosition = 0)

        assertTrue(dao.getAllTierItemsByTierId(sTierId).isEmpty())
        assertEquals(listOf(itemId), dao.getAllTierItemsByTierId(aTierId).map { it.id })
    }

    @Test
    fun move_item_into_tier_at_start_shifts_existing_items() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val first = dao.insertTierItem(tierItem(tierId = aTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = aTierId, title = "Second", position = 1))
        val moved = dao.insertTierItem(tierItem(tierId = sTierId, title = "Moved", position = 0))

        dao.moveItem(itemId = moved, toTierId = aTierId, toPosition = 0)

        val items = dao.getAllTierItemsByTierId(aTierId)
        assertEquals(listOf(moved, first, second), items.map { it.id })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun move_item_into_tier_in_the_middle_shifts_later_items() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val first = dao.insertTierItem(tierItem(tierId = aTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = aTierId, title = "Second", position = 1))
        val moved = dao.insertTierItem(tierItem(tierId = sTierId, title = "Moved", position = 0))

        dao.moveItem(itemId = moved, toTierId = aTierId, toPosition = 1)

        val items = dao.getAllTierItemsByTierId(aTierId)
        assertEquals(listOf(first, moved, second), items.map { it.id })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun move_item_into_tier_at_end_appends_after_existing_items() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val first = dao.insertTierItem(tierItem(tierId = aTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = aTierId, title = "Second", position = 1))
        val moved = dao.insertTierItem(tierItem(tierId = sTierId, title = "Moved", position = 0))

        dao.moveItem(itemId = moved, toTierId = aTierId, toPosition = 2)

        val items = dao.getAllTierItemsByTierId(aTierId)
        assertEquals(listOf(first, second, moved), items.map { it.id })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun move_item_with_out_of_range_position_appends_to_end() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val first = dao.insertTierItem(tierItem(tierId = aTierId, title = "First", position = 0))
        val moved = dao.insertTierItem(tierItem(tierId = sTierId, title = "Moved", position = 0))

        dao.moveItem(itemId = moved, toTierId = aTierId, toPosition = 999)

        val items = dao.getAllTierItemsByTierId(aTierId)
        assertEquals(listOf(first, moved), items.map { it.id })
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun move_last_item_out_of_tier_leaves_source_tier_empty() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val moved = dao.insertTierItem(tierItem(tierId = sTierId, title = "Moved", position = 0))

        dao.moveItem(itemId = moved, toTierId = aTierId, toPosition = 0)

        assertTrue(dao.getAllTierItemsByTierId(sTierId).isEmpty())
        val items = dao.getAllTierItemsByTierId(aTierId)
        assertEquals(listOf(moved), items.map { it.id })
        assertEquals(listOf(0), items.map { it.position })
    }

    @Test
    fun move_item_within_same_tier_reorders_without_losing_items() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val first = dao.insertTierItem(tierItem(tierId = sTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = sTierId, title = "Second", position = 1))
        val third = dao.insertTierItem(tierItem(tierId = sTierId, title = "Third", position = 2))

        dao.moveItem(itemId = third, toTierId = sTierId, toPosition = 0)

        val items = dao.getAllTierItemsByTierId(sTierId)
        assertEquals(listOf(third, first, second), items.map { it.id })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun move_item_within_same_tier_to_start_reorders_and_stays_contiguous() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val first = dao.insertTierItem(tierItem(tierId = sTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = sTierId, title = "Second", position = 1))
        val third = dao.insertTierItem(tierItem(tierId = sTierId, title = "Third", position = 2))
        val fourth = dao.insertTierItem(tierItem(tierId = sTierId, title = "Fourth", position = 3))

        dao.moveItem(itemId = fourth, toTierId = sTierId, toPosition = 0)

        val items = dao.getAllTierItemsByTierId(sTierId)
        assertEquals(listOf(fourth, first, second, third), items.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), items.map { it.position })
    }

    @Test
    fun move_item_within_same_tier_to_middle_reorders_and_stays_contiguous() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val first = dao.insertTierItem(tierItem(tierId = sTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = sTierId, title = "Second", position = 1))
        val third = dao.insertTierItem(tierItem(tierId = sTierId, title = "Third", position = 2))
        val fourth = dao.insertTierItem(tierItem(tierId = sTierId, title = "Fourth", position = 3))

        dao.moveItem(itemId = fourth, toTierId = sTierId, toPosition = 1)

        val items = dao.getAllTierItemsByTierId(sTierId)
        assertEquals(listOf(first, fourth, second, third), items.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), items.map { it.position })
    }

    @Test
    fun move_item_within_same_tier_to_end_reorders_and_stays_contiguous() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val first = dao.insertTierItem(tierItem(tierId = sTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = sTierId, title = "Second", position = 1))
        val third = dao.insertTierItem(tierItem(tierId = sTierId, title = "Third", position = 2))
        val fourth = dao.insertTierItem(tierItem(tierId = sTierId, title = "Fourth", position = 3))

        dao.moveItem(itemId = first, toTierId = sTierId, toPosition = 3)

        val items = dao.getAllTierItemsByTierId(sTierId)
        assertEquals(listOf(second, third, fourth, first), items.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), items.map { it.position })
    }

    @Test
    fun move_item_within_same_tier_forward_past_later_items_lands_on_requested_index() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val first = dao.insertTierItem(tierItem(tierId = sTierId, title = "First", position = 0))
        val second = dao.insertTierItem(tierItem(tierId = sTierId, title = "Second", position = 1))
        val third = dao.insertTierItem(tierItem(tierId = sTierId, title = "Third", position = 2))
        val fourth = dao.insertTierItem(tierItem(tierId = sTierId, title = "Fourth", position = 3))

        // toPosition is an index into the sibling list with `first` already removed:
        // [second, third, fourth]. Index 2 lands `first` right before `fourth`.
        dao.moveItem(itemId = first, toTierId = sTierId, toPosition = 2)

        val items = dao.getAllTierItemsByTierId(sTierId)
        assertEquals(listOf(second, third, first, fourth), items.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), items.map { it.position })
    }

    @Test
    fun move_item_leaves_source_and_target_positions_contiguous_without_duplicates() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        dao.insertTierItem(tierItem(tierId = sTierId, title = "S1", position = 0))
        val moved = dao.insertTierItem(tierItem(tierId = sTierId, title = "S2", position = 1))
        dao.insertTierItem(tierItem(tierId = sTierId, title = "S3", position = 2))
        dao.insertTierItem(tierItem(tierId = aTierId, title = "A1", position = 0))
        dao.insertTierItem(tierItem(tierId = aTierId, title = "A2", position = 1))

        dao.moveItem(itemId = moved, toTierId = aTierId, toPosition = 1)

        val sourcePositions = dao.getAllTierItemsByTierId(sTierId).map { it.position }
        val targetPositions = dao.getAllTierItemsByTierId(aTierId).map { it.position }
        assertEquals(listOf(0, 1), sourcePositions)
        assertEquals(setOf(0, 1), sourcePositions.toSet())
        assertEquals(listOf(0, 1, 2), targetPositions)
        assertEquals(setOf(0, 1, 2), targetPositions.toSet())
    }

    private fun tierList(title: String = "Films"): TierListEntity = TierListEntity(
        title = title,
    )

    private fun tier(
        tierListId: Long,
        position: Int,
        label: String,
        colorLight: String,
        colorDark: String,
    ): TierEntity = TierEntity(
        tierListId = tierListId,
        position = position,
        label = label,
        colorLight = colorLight,
        colorDark = colorDark,
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
