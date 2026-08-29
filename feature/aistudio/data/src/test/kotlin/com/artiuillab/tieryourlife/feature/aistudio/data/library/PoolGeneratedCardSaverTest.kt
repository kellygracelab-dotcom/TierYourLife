package com.artiuillab.tieryourlife.feature.aistudio.data.library

import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PoolGeneratedCardSaverTest {

    @Test
    fun `save adds a generated item to the pool and attaches the image, returning its id`() = runBlocking {
        val repository = FakeTierRepository()
        val saver = PoolGeneratedCardSaver(repository)

        val itemId = saver.save(tierListId = 7L, title = "Neon street", imageUri = "file:///cache/aistudio/x.png")

        assertEquals(repository.addedItemId, itemId)
        val call = repository.addCalls.single()
        assertEquals(7L, call.tierListId)
        assertEquals("Neon street", call.title)
        assertEquals(null, call.imageUrl)
        assertEquals(TierItemSource.GENERATED, call.source)
        assertEquals(listOf(repository.addedItemId to "file:///cache/aistudio/x.png"), repository.attachCalls)
    }
}

private data class AddCall(val tierListId: Long, val title: String, val imageUrl: String?, val source: TierItemSource)

private class FakeTierRepository : TierRepository {

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
    ): Long = 0

    override suspend fun setPublishedId(id: Long, publishedId: String?) = Unit

    val addCalls = mutableListOf<AddCall>()
    val attachCalls = mutableListOf<Pair<Long, String>>()
    val addedItemId = 42L

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long {
        addCalls += AddCall(tierListId, title, imageUrl, source)
        return addedItemId
    }

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) {
        attachCalls += itemId to sourceUri
    }

    override suspend fun getTierListById(id: Long): TierList? = unsupported()
    override suspend fun getAllTierLists(): List<TierList> = unsupported()
    override suspend fun createTierList(title: String): Long = unsupported()
    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = unsupported()
    override suspend fun renameTierList(id: Long, title: String) = unsupported()
    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = unsupported()
    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) = unsupported()
    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long = unsupported()

    override suspend fun renameTier(id: Long, label: String, caption: String?) = unsupported()
    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) = unsupported()
    override suspend fun deleteTierToPool(id: Long) = unsupported()
    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) = unsupported()

    override suspend fun reorderTiers(orderedTierIds: List<Long>) = unsupported()
    override suspend fun deleteTierLists(ids: List<Long>) = unsupported()
    override suspend fun restoreTierLists(ids: List<Long>) = unsupported()
    override suspend fun deleteTierItem(id: Long) = unsupported()
    override suspend fun restoreTierItem(id: Long) = unsupported()
    override suspend fun deleteTierListPermanently(id: Long) = unsupported()
    override suspend fun deleteTierItemPermanently(id: Long) = unsupported()
    override suspend fun emptyTrash() = unsupported()
    override suspend fun getTrashEntries(): List<TrashEntry> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Not used by PoolGeneratedCardSaverTest")
}
