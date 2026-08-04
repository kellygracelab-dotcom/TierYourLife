package com.artiuillab.tieryourlife.feature.tier.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTierRepositoryTest {

    private lateinit var database: TierDatabase
    private lateinit var dao: TierDao
    private lateinit var imagesDir: File
    private lateinit var repository: RoomTierRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TierDatabase::class.java,
        ).build()
        dao = database.tierDao()
        imagesDir = File(context.cacheDir, "test_tier_images_${System.nanoTime()}")
        val imageStore = TierImageStore(imagesDir) { uri -> ByteArrayInputStream("bytes of $uri".toByteArray()) }
        repository = RoomTierRepository(dao, imageStore)
    }

    @After
    fun tearDown() {
        database.close()
        imagesDir.deleteRecursively()
    }

    @Test
    fun get_tier_list_by_missing_id_returns_null() = runBlocking {
        val actual = repository.getTierListById(Long.MAX_VALUE)

        assertNull(actual)
    }

    @Test
    fun get_tier_list_by_existing_id_returns_mapped_domain_graph() = runBlocking {
        val tierList = TierListEntity(title = "Films")
        val tierListId = dao.insertTierList(tierList)
        val tier = TierEntity(
            tierListId = tierListId,
            position = 1,
            label = "S",
            colorLight = "#B03A32",
            colorDark = "#F1948C",
        )
        val tierId = dao.insertTier(tier)
        val item = TierItemEntity(
            tierId = tierId,
            position = 1,
            title = "The Shawshank Redemption",
            imageUrl = "https://example.com/shawshank.jpg",
        )
        dao.insertTierItem(item)

        val actual = requireNotNull(repository.getTierListById(tierListId))

        assertEquals(tierListId, actual.id)
        assertEquals(tierList.title, actual.title)
        assertEquals(tier.label, actual.tiers.single().label)
        assertEquals(item.title, actual.tiers.single().items.single().title)
    }

    @Test
    fun create_tier_list_defaults_to_wrap_display_mode() = runBlocking {
        val id = repository.createTierList("Films")

        val actual = requireNotNull(repository.getTierListById(id))

        assertEquals(TierListDisplayMode.WRAP, actual.displayMode)
    }

    @Test
    fun set_tier_list_display_mode_persists_and_is_read_back_by_id_and_by_overview() = runBlocking {
        val id = dao.insertTierList(TierListEntity(title = "Films"))

        repository.setTierListDisplayMode(id, TierListDisplayMode.HORIZONTAL_SCROLL)

        val byId = requireNotNull(repository.getTierListById(id))
        val overview = repository.getAllTierLists().single { it.id == id }
        assertEquals(TierListDisplayMode.HORIZONTAL_SCROLL, byId.displayMode)
        assertEquals(TierListDisplayMode.HORIZONTAL_SCROLL, overview.displayMode)
    }

    @Test
    fun get_all_tier_lists_carries_each_lists_own_display_mode() = runBlocking {
        val filmsId = dao.insertTierList(TierListEntity(title = "Films"))
        val gamesId = dao.insertTierList(TierListEntity(title = "Games"))
        repository.setTierListDisplayMode(filmsId, TierListDisplayMode.FLAT_RANKED)

        val lists = repository.getAllTierLists()

        assertEquals(TierListDisplayMode.FLAT_RANKED, lists.single { it.id == filmsId }.displayMode)
        assertEquals(TierListDisplayMode.WRAP, lists.single { it.id == gamesId }.displayMode)
    }

    @Test
    fun unrecognized_stored_display_mode_reads_back_as_wrap_by_id_and_in_overview() = runBlocking {
        val id = dao.insertTierList(TierListEntity(title = "Films", displayMode = "SOME_FUTURE_MODE"))

        val byId = requireNotNull(repository.getTierListById(id))
        val overview = repository.getAllTierLists().single { it.id == id }

        assertEquals(TierListDisplayMode.WRAP, byId.displayMode)
        assertEquals(TierListDisplayMode.WRAP, overview.displayMode)
    }

    @Test
    fun move_item_relocates_item_to_target_tier_and_position() = runBlocking {
        val tierListId = dao.insertTierList(TierListEntity(title = "Films"))
        val sourceTierId = dao.insertTier(
            TierEntity(
                tierListId = tierListId,
                position = 1,
                label = "S",
                colorLight = "#B03A32",
                colorDark = "#F1948C"
            ),
        )
        val targetTierId = dao.insertTier(
            TierEntity(
                tierListId = tierListId,
                position = 2,
                label = "A",
                colorLight = "#C06A25",
                colorDark = "#E9A867"
            ),
        )
        val itemId = dao.insertTierItem(
            TierItemEntity(
                tierId = sourceTierId,
                position = 0,
                title = "Interstellar",
                imageUrl = null
            ),
        )

        repository.moveItem(itemId = itemId, toTierId = targetTierId, toPosition = 0)

        val movedList = requireNotNull(repository.getTierListById(tierListId))
        val sourceTier = movedList.tiers.single { it.id == sourceTierId }
        val targetTier = movedList.tiers.single { it.id == targetTierId }
        assertEquals(emptyList<String>(), sourceTier.items.map { it.title })
        assertEquals(listOf("Interstellar"), targetTier.items.map { it.title })
    }

    @Test
    fun delete_tier_removes_it_and_cascades_its_items() = runBlocking {
        val listId = repository.createTierList("Films")
        val sTierId = requireNotNull(repository.getTierListById(listId)).tiers.single { it.label == "S" }.id
        dao.insertTierItem(TierItemEntity(tierId = sTierId, position = 0, title = "Doomed", imageUrl = null))

        repository.deleteTier(sTierId)

        val list = requireNotNull(repository.getTierListById(listId))
        assertNull(list.tiers.find { it.id == sTierId })
    }

    @Test
    fun delete_tier_on_the_pool_is_a_no_op_and_exactly_one_pool_remains() = runBlocking {
        val listId = repository.createTierList("Films")
        val poolTierId = requireNotNull(repository.getTierListById(listId)).tiers.single { it.isPool }.id

        repository.deleteTier(poolTierId)

        val list = requireNotNull(repository.getTierListById(listId))
        assertEquals(1, list.tiers.count { it.isPool })
        assertEquals(poolTierId, list.tiers.single { it.isPool }.id)
    }

    @Test
    fun rename_recolor_and_reorder_tiers_through_the_repository() = runBlocking {
        val listId = repository.createTierList("Films")
        val ranked = requireNotNull(repository.getTierListById(listId)).tiers.filterNot { it.isPool }
        val sTierId = ranked.first { it.label == "S" }.id

        repository.renameTier(sTierId, label = "S+", caption = "Legendary")
        repository.updateTierColors(sTierId, colorLight = "#111111", colorDark = "#222222")
        repository.reorderTiers(ranked.map { it.id }.reversed())

        val updated = requireNotNull(repository.getTierListById(listId))
        val sTier = updated.tiers.single { it.id == sTierId }
        assertEquals("S+", sTier.label)
        assertEquals("Legendary", sTier.caption)
        assertEquals("#111111", sTier.colorLight)
        assertEquals("#222222", sTier.colorDark)
        assertEquals(ranked.map { it.id }.reversed(), updated.tiers.filterNot { it.isPool }.map { it.id })
    }

    @Test
    fun bulk_add_movies_through_the_repository_appends_all_of_them_to_the_pool() = runBlocking {
        val listId = repository.createTierList("Films")

        repository.addMoviesToPool(
            listId,
            listOf(
                PoolMovieDraft(title = "Interstellar", imageUrl = "https://example.com/1.jpg"),
                PoolMovieDraft(title = "Arrival", imageUrl = null),
            ),
        )

        val pool = requireNotNull(repository.getTierListById(listId)).tiers.single { it.isPool }
        assertEquals(listOf("Interstellar", "Arrival"), pool.items.map { it.title })
    }
}
