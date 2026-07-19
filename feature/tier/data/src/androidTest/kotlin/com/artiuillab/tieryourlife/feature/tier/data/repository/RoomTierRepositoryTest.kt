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
    private lateinit var repository: RoomTierRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TierDatabase::class.java,
        ).build()
        dao = database.tierDao()
        repository = RoomTierRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
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
            color = "GOLD",
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
}
