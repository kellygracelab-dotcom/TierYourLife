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
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTierRepositoryImageTest {

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
        val store = TierImageStore(imagesDir) { uri ->
            ByteArrayInputStream("image bytes of $uri".toByteArray())
        }
        repository = RoomTierRepository(dao, store) { 1_000L }
    }

    @After
    fun tearDown() {
        database.close()
        imagesDir.deleteRecursively()
    }

    @Test
    fun attach_image_copies_source_into_internal_storage() = runBlocking {
        val itemId = insertItem()

        repository.attachImageToItem(itemId, sourceUri = "content://gallery/42")

        val storedPath = requireNotNull(dao.getTierItemImageById(itemId))
        val copy = File(storedPath)
        assertTrue(copy.exists())
        assertEquals(imagesDir.absolutePath, copy.parentFile?.absolutePath)
        assertEquals("image bytes of content://gallery/42", copy.readText())
    }

    @Test
    fun replacing_an_attached_image_deletes_the_previous_copy() = runBlocking {
        val itemId = insertItem()
        repository.attachImageToItem(itemId, sourceUri = "content://gallery/old")
        val firstPath = requireNotNull(dao.getTierItemImageById(itemId))

        repository.attachImageToItem(itemId, sourceUri = "content://gallery/new")

        val secondPath = requireNotNull(dao.getTierItemImageById(itemId))
        assertFalse(File(firstPath).exists())
        assertTrue(File(secondPath).exists())
    }

    @Test
    fun deleting_item_permanently_deletes_its_image_copy() = runBlocking {
        val itemId = insertItem()
        repository.attachImageToItem(itemId, sourceUri = "content://gallery/42")
        val storedPath = requireNotNull(dao.getTierItemImageById(itemId))

        repository.deleteTierItem(itemId)
        repository.deleteTierItemPermanently(itemId)

        assertFalse(File(storedPath).exists())
    }

    @Test
    fun emptying_trash_sweeps_image_copies_of_cascade_deleted_items() = runBlocking {
        val itemId = insertItem()
        repository.attachImageToItem(itemId, sourceUri = "content://gallery/42")
        val storedPath = requireNotNull(dao.getTierItemImageById(itemId))
        val listId = dao.getAllTierLists().single().id

        repository.deleteTierLists(listOf(listId))
        repository.emptyTrash()

        assertFalse(File(storedPath).exists())
        assertTrue(imagesDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun deleting_a_tier_sweeps_the_image_copies_of_its_cascade_deleted_items() = runBlocking {
        val itemId = insertItem()
        repository.attachImageToItem(itemId, sourceUri = "content://gallery/42")
        val storedPath = requireNotNull(dao.getTierItemImageById(itemId))
        val tierId = dao.getTierItemById(itemId)!!.tierId

        // A hard tier delete cascades the item row away without going through
        // deleteTierItemPermanently, so the orphan sweep is the only thing that
        // catches its now-dangling image copy.
        repository.deleteTier(tierId)

        assertFalse(File(storedPath).exists())
    }

    private suspend fun insertItem(): Long {
        val listId = dao.insertTierList(TierListEntity(title = "Films"))
        val tierId = dao.insertTier(
            TierEntity(
                tierListId = listId,
                position = 0,
                label = "S",
                colorLight = "#000000",
                colorDark = "#000000",
            ),
        )
        return dao.insertTierItem(
            TierItemEntity(
                tierId = tierId,
                position = 0,
                title = "Interstellar",
                imageUrl = null,
            ),
        )
    }
}
