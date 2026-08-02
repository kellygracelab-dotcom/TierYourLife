package com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TierListsPresentationLoaderTest {

    @Test
    fun `loader creates initial lists, loads full graphs, and builds fixed presentation data`() = runBlocking {
        val repository = FakeTierRepository()

        val lists = repository.loadTierListsForPresentation()

        assertEquals(2, repository.createCalls)
        assertEquals(setOf(1L, 2L), repository.requestedFullListIds.toSet())
        assertEquals(15, lists.size)
        assertTrue(lists.take(2).all { it.id > 0 })

        val temporaryLists = lists.drop(2)
        assertEquals(13, temporaryLists.size)
        assertTrue(temporaryLists.all { it.id < 0 })
        assertEquals(15, lists.map { it.id }.toSet().size)

        assertTierCounts(lists.first { it.title == "Sci-fi films" }, ranked = 7, pool = 6)
        assertTierCounts(lists.first { it.title == "Every A24 film" }, ranked = 12, pool = 4)
        assertTemporaryLists(temporaryLists)

        val repeatedLists = repository.loadTierListsForPresentation()
        assertEquals(15, repeatedLists.size)
        assertEquals(15, repeatedLists.map { it.id }.toSet().size)
        val repeatedTemporaryLists = repeatedLists.drop(2)
        assertEquals(13, repeatedTemporaryLists.size)
        assertTrue(repeatedTemporaryLists.all { it.id < 0 })
        assertEquals(2, repository.createCalls)
        assertEquals(2, repository.storedLists.size)
        assertEquals(
            repository.storedLists.flatMap { it.tiers }.size,
            repository.storedLists.flatMap { it.tiers }.map { it.id }.toSet().size,
        )
    }

    @Test
    fun `loader preserves existing tier items instead of replacing them with demo items`() = runBlocking {
        val actualItem = TierItem(id = 42L, title = "Real item", imageUrl = null)
        val repository = FakeTierRepository(
            initialLists = listOf(
                TierList(1L, "Sci-fi films", defaultTiers(1L, itemsInS = listOf(actualItem))),
                TierList(2L, "Every A24 film", defaultTiers(2L)),
            ),
        )

        val lists = repository.loadTierListsForPresentation()
        val sciFi = lists.first { it.title == "Sci-fi films" }
        val a24 = lists.first { it.title == "Every A24 film" }

        assertEquals(0, repository.createCalls)
        val preservedItem = sciFi.tiers.first { it.label == "S" }.items.single()
        assertEquals(actualItem.id, preservedItem.id)
        assertEquals(actualItem.title, preservedItem.title)
        assertEquals(1, sciFi.tiers.sumOf { it.items.size })
        assertTierCounts(a24, ranked = 12, pool = 4)
    }

    private fun assertTierCounts(list: TierList, ranked: Int, pool: Int) {
        assertFalse(list.tiers.isEmpty())
        assertEquals(ranked, list.tiers.filterNot { it.isPool }.sumOf { it.items.size })
        val poolTier = list.tiers.firstOrNull { it.isPool }
        assertNotNull(poolTier)
        assertEquals(pool, poolTier?.items?.size)
    }

    private fun assertTemporaryLists(lists: List<TierList>) {
        lists.forEach { list ->
            assertTrue(list.tiers.all { it.id < 0 })
            assertTrue(list.tiers.filterNot { it.isPool }.count { it.items.isNotEmpty() } > 1)
            val poolTier = list.tiers.firstOrNull { it.isPool }
            assertNotNull(poolTier)
            assertTrue((poolTier?.items?.isNotEmpty()) == true)
            assertTrue(list.tiers.flatMap { it.items }.all { it.id < 0 })
        }
    }
}

private class FakeTierRepository(
    initialLists: List<TierList> = emptyList(),
) : TierRepository {
    val storedLists = initialLists.toMutableList()
    val requestedFullListIds = mutableListOf<Long>()
    var createCalls = 0
        private set

    override suspend fun getTierListById(id: Long): TierList? {
        requestedFullListIds += id
        return storedLists.firstOrNull { it.id == id }
    }

    override suspend fun getAllTierLists(): List<TierList> = storedLists.map { it.copy(tiers = emptyList()) }

    override suspend fun createTierList(title: String): Long {
        createCalls += 1
        val id = storedLists.size + 1L
        storedLists += TierList(id = id, title = title, tiers = defaultTiers(id))
        return id
    }

    override suspend fun addMovieToPool(tierListId: Long, title: String, imageUrl: String?): Long = error("Not used")
}

private fun defaultTiers(tierListId: Long, itemsInS: List<TierItem> = emptyList()): List<Tier> =
    listOf("S", "A", "B", "C", "D", "Unranked").mapIndexed { index, label ->
        Tier(
            id = tierListId * 100 + index + 1L,
            label = label,
            colorLight = "#000000",
            colorDark = "#000000",
            items = if (label == "S") itemsInS else emptyList(),
            isPool = false,
        )
    }
