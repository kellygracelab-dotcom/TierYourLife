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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun create_tier_list_with_default_tier_sets_the_five_ranked_captions() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        val tiers = dao.getAllTiersByTierListId(listId)

        assertEquals(
            listOf("Masterpiece", "Great", "Good", "Watchable", "No"),
            tiers.filterNot { it.isPool }.map { it.caption },
        )
        assertEquals(null, tiers.single { it.isPool }.caption)
    }

    @Test
    fun insert_tier_with_caption_persists_and_reads_it_back() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C")
                .copy(caption = "Masterpiece"),
        )

        val saved = dao.getAllTiersByTierListId(filmsId).single { it.id == tierId }

        assertEquals("Masterpiece", saved.caption)
    }

    @Test
    fun insert_tier_with_empty_caption_persists_empty_string_not_null() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C")
                .copy(caption = ""),
        )

        val saved = dao.getAllTiersByTierListId(filmsId).single { it.id == tierId }

        assertEquals("", saved.caption)
    }

    @Test
    fun insert_tier_without_caption_persists_null() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )

        val saved = dao.getAllTiersByTierListId(filmsId).single { it.id == tierId }

        assertEquals(null, saved.caption)
    }

    @Test
    fun add_tier_appends_after_existing_ranked_tiers_and_keeps_positions_contiguous() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        val newTierId = dao.addTier(
            tierListId = listId,
            label = "E",
            caption = "Trash",
            colorLight = "#111111",
            colorDark = "#222222",
        )

        val tiers = dao.getAllTiersByTierListId(listId)
        assertEquals(listOf("S", "A", "B", "C", "D", "E", "Unranked"), tiers.map { it.label })
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), tiers.map { it.position })
        val newTier = tiers.single { it.id == newTierId }
        assertEquals("E", newTier.label)
        assertEquals("Trash", newTier.caption)
        assertEquals(false, newTier.isPool)
    }

    @Test
    fun add_tier_does_not_create_a_second_pool() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        dao.addTier(tierListId = listId, label = "E", caption = null, colorLight = "#111111", colorDark = "#222222")

        val poolTiers = dao.getAllTiersByTierListId(listId).filter { it.isPool }
        assertEquals(1, poolTiers.size)
        assertEquals("Unranked", poolTiers.single().label)
    }

    @Test
    fun add_item_to_pool_puts_first_item_at_position_zero() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolTierId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id

        dao.addItemToPool(
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
    fun add_item_to_pool_appends_items_with_increasing_positions() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolTierId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id

        dao.addItemToPool(tierListId = listId, title = "Interstellar", imageUrl = null)
        dao.addItemToPool(tierListId = listId, title = "Arrival", imageUrl = null)

        val poolItems = dao.getAllTierItemsByTierId(poolTierId)
        assertEquals(listOf("Interstellar", "Arrival"), poolItems.map { it.title })
        assertEquals(listOf(0, 1), poolItems.map { it.position })
    }

    @Test
    fun add_item_to_pool_leaves_ranked_tiers_empty() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")

        dao.addItemToPool(tierListId = listId, title = "Interstellar", imageUrl = null)

        val rankedTiers = dao.getAllTiersByTierListId(listId).filterNot { it.isPool }
        assertTrue(rankedTiers.all { dao.getAllTierItemsByTierId(it.id).isEmpty() })
    }

    @Test
    fun rename_tier_updates_label_and_caption_together() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTier = dao.getAllTiersByTierListId(listId).first()

        dao.renameTier(sTier.id, label = "S+", caption = "Beyond words")

        val renamed = requireNotNull(dao.getTierById(sTier.id))
        assertEquals("S+", renamed.label)
        assertEquals("Beyond words", renamed.caption)
        assertEquals(sTier.position, renamed.position)
        assertEquals(sTier.colorLight, renamed.colorLight)
        assertEquals(sTier.colorDark, renamed.colorDark)
    }

    @Test
    fun rename_tier_with_empty_caption_persists_empty_string_not_null() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTier = dao.getAllTiersByTierListId(listId).first()

        dao.renameTier(sTier.id, label = "S+", caption = "")

        assertEquals("", requireNotNull(dao.getTierById(sTier.id)).caption)
    }

    @Test
    fun rename_tier_with_null_caption_clears_a_previously_set_one() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTier = dao.getAllTiersByTierListId(listId).first()

        dao.renameTier(sTier.id, label = "S+", caption = null)

        assertNull(requireNotNull(dao.getTierById(sTier.id)).caption)
    }

    @Test
    fun update_tier_colors_changes_both_theme_variants_without_touching_the_other_fields() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTier = dao.getAllTiersByTierListId(listId).first()

        dao.updateTierColors(sTier.id, colorLight = "#ABCDEF", colorDark = "#FEDCBA")

        val updated = requireNotNull(dao.getTierById(sTier.id))
        assertEquals("#ABCDEF", updated.colorLight)
        assertEquals("#FEDCBA", updated.colorDark)
        assertEquals(sTier.label, updated.label)
        assertEquals(sTier.caption, updated.caption)
    }

    @Test
    fun reorder_tiers_gives_contiguous_positions_and_leaves_the_pool_where_it_was() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val allTiers = dao.getAllTiersByTierListId(listId)
        val poolPosition = allTiers.single { it.isPool }.position
        val rankedIds = allTiers.filterNot { it.isPool }.map { it.id }

        dao.reorderTiers(rankedIds.reversed())

        val tiers = dao.getAllTiersByTierListId(listId)
        val rankedTiers = tiers.filterNot { it.isPool }
        assertEquals(rankedIds.reversed(), rankedTiers.map { it.id })
        assertEquals((0 until rankedTiers.size).toList(), rankedTiers.map { it.position })
        assertEquals(rankedTiers.size, rankedTiers.map { it.position }.toSet().size)
        assertEquals(poolPosition, tiers.single { it.isPool }.position)
        assertEquals("Unranked", tiers.last().label)
    }

    @Test
    fun reorder_tiers_ignores_a_stale_id_for_an_already_deleted_tier() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val rankedTiers = dao.getAllTiersByTierListId(listId).filterNot { it.isPool }
        val doomed = rankedTiers.first()
        val survivorIds = rankedTiers.drop(1).map { it.id }
        dao.deleteTierById(doomed.id)

        dao.reorderTiers(listOf(doomed.id) + survivorIds.reversed())

        val remaining = dao.getAllTiersByTierListId(listId).filterNot { it.isPool }
        assertEquals(survivorIds.reversed(), remaining.map { it.id })
        assertEquals((0 until remaining.size).toList(), remaining.map { it.position })
    }

    @Test
    fun bulk_add_items_to_pool_appends_all_with_contiguous_positions_in_one_call() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id
        dao.insertTierItem(tierItem(tierId = poolId, title = "Existing", position = 0))

        dao.addItemsToPool(
            listId,
            listOf(
                NewPoolItem(title = "Interstellar", imageUrl = "https://example.com/1.jpg"),
                NewPoolItem(title = "Arrival", imageUrl = null),
                NewPoolItem(title = "Moon", imageUrl = null),
            ),
        )

        val poolItems = dao.getAllTierItemsByTierId(poolId)
        assertEquals(listOf("Existing", "Interstellar", "Arrival", "Moon"), poolItems.map { it.title })
        assertEquals(listOf(0, 1, 2, 3), poolItems.map { it.position })
        assertEquals("https://example.com/1.jpg", poolItems[1].imageUrl)
    }

    @Test
    fun bulk_add_items_does_not_reuse_or_collide_with_a_trashed_items_old_position() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id
        dao.insertTierItem(tierItem(tierId = poolId, title = "Active", position = 0))
        val trashedId = dao.insertTierItem(tierItem(tierId = poolId, title = "Trashed", position = 1))
        dao.markTierItemDeleted(trashedId, deletedAt = 1_000L)

        dao.addItemsToPool(listId, listOf(NewPoolItem(title = "New", imageUrl = null)))

        val poolItems = dao.getAllTierItemsByTierId(poolId)
        assertEquals(listOf("Active", "New"), poolItems.map { it.title })
        assertEquals(listOf(0, 1), poolItems.map { it.position })
    }

    @Test
    fun bulk_add_items_to_the_pool_of_a_trashed_list_is_a_no_op() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val poolId = dao.getAllTiersByTierListId(listId).single { it.isPool }.id
        dao.markTierListsDeleted(listOf(listId), deletedAt = 1_000L)

        dao.addItemsToPool(listId, listOf(NewPoolItem(title = "New", imageUrl = null)))

        assertTrue(dao.getAllTierItemsByTierId(poolId).isEmpty())
    }

    @Test
    fun update_tier_item_image_persists_and_is_read_back() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val itemId = dao.insertTierItem(
            tierItem(tierId = sTierId, title = "Interstellar", position = 0).copy(imageUrl = null),
        )

        dao.updateTierItemImage(itemId, "/data/user/0/app/files/tier_images/abc")

        assertEquals("/data/user/0/app/files/tier_images/abc", dao.getTierItemImageById(itemId))
    }

    @Test
    fun get_tier_item_image_by_id_finds_it_even_when_the_item_is_trashed() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val itemId = dao.insertTierItem(
            tierItem(tierId = sTierId, title = "Interstellar", position = 0).copy(imageUrl = "/path/1"),
        )
        dao.markTierItemDeleted(itemId, deletedAt = 1_000L)

        assertEquals("/path/1", dao.getTierItemImageById(itemId))
    }

    @Test
    fun get_all_image_refs_includes_trashed_items_and_excludes_items_without_an_image() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        dao.insertTierItem(tierItem(tierId = sTierId, title = "A", position = 0).copy(imageUrl = "/path/1"))
        dao.insertTierItem(tierItem(tierId = sTierId, title = "B", position = 1).copy(imageUrl = null))
        val trashedWithImage = dao.insertTierItem(
            tierItem(tierId = sTierId, title = "C", position = 2).copy(imageUrl = "/path/2"),
        )
        dao.markTierItemDeleted(trashedWithImage, deletedAt = 1_000L)

        val refs = dao.getAllImageRefs()

        assertEquals(setOf("/path/1", "/path/2"), refs.toSet())
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
        val itemId = dao.addItemToPool(tierListId = listId, title = "Interstellar", imageUrl = null)

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

    @Test
    fun insert_tier_list_defaults_to_wrap_display_mode() = runBlocking {
        val id = dao.insertTierList(tierList())

        val saved = requireNotNull(dao.getTierListById(id))

        assertEquals("WRAP", saved.displayMode)
    }

    @Test
    fun create_tier_list_with_default_tier_defaults_to_wrap_display_mode() = runBlocking {
        val id = dao.createTierListWithDefaultTier(title = "Films")

        val saved = requireNotNull(dao.getTierListById(id))

        assertEquals("WRAP", saved.displayMode)
    }

    @Test
    fun update_tier_list_display_mode_persists_and_is_read_back() = runBlocking {
        val id = dao.insertTierList(tierList())

        dao.updateTierListDisplayMode(id, "HORIZONTAL_SCROLL")

        assertEquals("HORIZONTAL_SCROLL", requireNotNull(dao.getTierListById(id)).displayMode)
    }

    @Test
    fun update_tier_list_title_persists_and_is_read_back() = runBlocking {
        val id = dao.insertTierList(tierList(title = "Films"))

        dao.updateTierListTitle(id, "Sci-fi films")

        assertEquals("Sci-fi films", requireNotNull(dao.getTierListById(id)).title)
    }

    @Test
    fun tier_list_with_tiers_carries_the_lists_own_display_mode() = runBlocking {
        val filmsId = dao.insertTierList(tierList(title = "Films"))
        dao.updateTierListDisplayMode(filmsId, "FLAT_RANKED")
        val gamesId = dao.insertTierList(tierList(title = "Games"))

        val filmsGraph = requireNotNull(dao.getTierListWithTiers(filmsId))
        val gamesGraph = requireNotNull(dao.getTierListWithTiers(gamesId))

        assertEquals("FLAT_RANKED", filmsGraph.tierList.displayMode)
        assertEquals("WRAP", gamesGraph.tierList.displayMode)
    }

    @Test
    fun marked_list_is_absent_from_overview_and_from_full_graph() = runBlocking {
        val filmsId = dao.insertTierList(tierList(title = "Films"))
        val gamesId = dao.insertTierList(tierList(title = "Games"))

        dao.markTierListsDeleted(listOf(filmsId), deletedAt = 1_000L)

        assertEquals(listOf(gamesId), dao.getAllTierLists().map { it.id })
        assertNull(dao.getTierListById(filmsId))
        assertNull(dao.getTierListWithTiers(filmsId))
    }

    @Test
    fun marked_item_is_absent_from_its_tier_in_the_graph() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val keptId = dao.insertTierItem(tierItem(tierId = tierId, title = "Kept", position = 0))
        val removedId = dao.insertTierItem(tierItem(tierId = tierId, title = "Removed", position = 1))

        dao.markTierItemDeleted(removedId, deletedAt = 1_000L)

        val graphItems = requireNotNull(dao.getTierListWithTiers(filmsId)).tiers.single().items
        assertEquals(listOf(keptId), graphItems.map { it.id })
        assertEquals(listOf(keptId), dao.getAllTierItemsByTierId(tierId).map { it.id })
    }

    @Test
    fun restore_returns_list_and_item_with_original_ids_and_positions() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val firstId = dao.insertTierItem(tierItem(tierId = tierId, title = "First", position = 0))
        val secondId = dao.insertTierItem(tierItem(tierId = tierId, title = "Second", position = 1))

        dao.markTierListsDeleted(listOf(filmsId), deletedAt = 1_000L)
        dao.markTierItemDeleted(secondId, deletedAt = 2_000L)
        dao.restoreTierLists(listOf(filmsId))
        dao.restoreTierItem(secondId)

        val graph = requireNotNull(dao.getTierListWithTiers(filmsId))
        assertEquals(filmsId, graph.tierList.id)
        val items = graph.tiers.single().items
        assertEquals(listOf(firstId, secondId), items.map { it.id })
        assertEquals(listOf(0, 1), items.sortedBy { it.position }.map { it.position })
    }

    @Test
    fun permanently_deleting_trashed_list_cascades_to_its_tiers_and_items() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val keptListId = dao.insertTierList(tierList(title = "Kept"))
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        dao.insertTierItem(tierItem(tierId = tierId, title = "Inside", position = 0))

        dao.markTierListsDeleted(listOf(filmsId, keptListId), deletedAt = 1_000L)
        dao.deleteTierListById(filmsId)

        assertEquals(listOf(keptListId), dao.getDeletedTierLists().map { it.id })
        assertTrue(dao.getDeletedTierItems().isEmpty())
        assertNull(dao.getTierListById(filmsId))
        assertTrue(dao.getAllTiersByTierListId(filmsId).isEmpty())
        assertTrue(dao.getAllTierItemsByTierId(tierId).isEmpty())
    }

    @Test
    fun empty_trash_removes_every_trashed_row_and_keeps_alive_data() = runBlocking {
        val trashedListId = dao.insertTierList(tierList(title = "Trashed list"))
        val aliveListId = dao.insertTierList(tierList(title = "Alive list"))
        val aliveTierId = dao.insertTier(
            tier(tierListId = aliveListId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val trashedItemId = dao.insertTierItem(tierItem(tierId = aliveTierId, title = "Trashed item", position = 0))
        val aliveItemId = dao.insertTierItem(tierItem(tierId = aliveTierId, title = "Alive item", position = 1))

        dao.markTierListsDeleted(listOf(trashedListId), deletedAt = 1_000L)
        dao.markTierItemDeleted(trashedItemId, deletedAt = 2_000L)

        dao.emptyTrash()

        assertTrue(dao.getDeletedTierLists().isEmpty())
        assertTrue(dao.getDeletedTierItems().isEmpty())
        assertEquals(listOf(aliveListId), dao.getAllTierLists().map { it.id })
        assertEquals(listOf(aliveItemId), dao.getAllTierItemsByTierId(aliveTierId).map { it.id })
    }

    @Test
    fun permanently_deleting_trashed_item_removes_only_that_item() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val tierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val goneItemId = dao.insertTierItem(tierItem(tierId = tierId, title = "Gone", position = 0))
        val keptItemId = dao.insertTierItem(tierItem(tierId = tierId, title = "Kept", position = 1))

        dao.markTierItemDeleted(goneItemId, deletedAt = 1_000L)
        dao.markTierItemDeleted(keptItemId, deletedAt = 2_000L)
        dao.deleteTierItemById(goneItemId)

        assertEquals(listOf(keptItemId), dao.getDeletedTierItems().map { it.id })
        dao.restoreTierItem(keptItemId)
        assertEquals(listOf(keptItemId), dao.getAllTierItemsByTierId(tierId).map { it.id })
    }

    @Test
    fun trash_rows_carry_item_count_parent_title_and_pool_flag() = runBlocking {
        val filmsId = dao.insertTierList(tierList())
        val rankedTierId = dao.insertTier(
            tier(tierListId = filmsId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        val poolTierId = dao.insertTier(
            tier(
                tierListId = filmsId, position = 2, label = "Unranked",
                colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true,
            ),
        )
        dao.insertTierItem(tierItem(tierId = rankedTierId, title = "Ranked one", position = 0))
        val poolItemId = dao.insertTierItem(tierItem(tierId = poolTierId, title = "Pool one", position = 0))

        val gamesId = dao.insertTierList(tierList(title = "Games"))
        val gamesTierId = dao.insertTier(
            tier(tierListId = gamesId, position = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C"),
        )
        dao.insertTierItem(tierItem(tierId = gamesTierId, title = "Game item", position = 0))

        dao.markTierItemDeleted(poolItemId, deletedAt = 1_000L)
        dao.markTierListsDeleted(listOf(gamesId), deletedAt = 2_000L)

        val deletedLists = dao.getDeletedTierLists()
        val deletedItems = dao.getDeletedTierItems()
        assertEquals(listOf("Games"), deletedLists.map { it.title })
        assertEquals(listOf(1), deletedLists.map { it.itemCount })
        assertEquals(listOf("Pool one"), deletedItems.map { it.title })
        assertEquals(listOf("Films"), deletedItems.map { it.listTitle })
        assertTrue(deletedItems.single().wasInPool)
        assertFalse(deletedItems.any { it.title == "Game item" })
    }

    @Test
    fun move_item_does_not_move_a_deleted_item() = runBlocking {
        val listId = dao.createTierListWithDefaultTier(title = "Films")
        val sTierId = dao.getAllTiersByTierListId(listId).single { it.label == "S" }.id
        val aTierId = dao.getAllTiersByTierListId(listId).single { it.label == "A" }.id
        val itemId = dao.insertTierItem(tierItem(tierId = sTierId, title = "Interstellar", position = 0))
        dao.markTierItemDeleted(itemId, deletedAt = 1_000L)

        dao.moveItem(itemId = itemId, toTierId = aTierId, toPosition = 0)
        dao.restoreTierItem(itemId)

        assertEquals(listOf(itemId), dao.getAllTierItemsByTierId(sTierId).map { it.id })
        assertTrue(dao.getAllTierItemsByTierId(aTierId).isEmpty())
    }

    @Test
    fun move_item_does_not_move_into_a_tier_whose_list_is_trashed() = runBlocking {
        val sourceListId = dao.createTierListWithDefaultTier(title = "Films")
        val sourceTierId = dao.getAllTiersByTierListId(sourceListId).single { it.label == "S" }.id
        val itemId = dao.insertTierItem(tierItem(tierId = sourceTierId, title = "Interstellar", position = 0))

        val trashedListId = dao.createTierListWithDefaultTier(title = "Trashed")
        val trashedTierId = dao.getAllTiersByTierListId(trashedListId).single { it.label == "A" }.id
        dao.markTierListsDeleted(listOf(trashedListId), deletedAt = 1_000L)

        dao.moveItem(itemId = itemId, toTierId = trashedTierId, toPosition = 0)

        assertEquals(listOf(itemId), dao.getAllTierItemsByTierId(sourceTierId).map { it.id })
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
        isPool: Boolean = false,
    ): TierEntity = TierEntity(
        tierListId = tierListId,
        position = position,
        label = label,
        colorLight = colorLight,
        colorDark = colorDark,
        isPool = isPool,
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
