package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierListRow
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierCountRow
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

class RoomTierRepositoryDeleteTest {

    private val imagesDir: File = Files.createTempDirectory("tier_images_test").toFile()
    private val imageStore = TierImageStore(imagesDir, openSource = { uri -> ByteArrayInputStream("bytes of $uri".toByteArray()) })

    @Test
    fun `trashing lists marks them with the injected current time`() = runBlocking {
        val dao = RecordingTierDao()
        val fixedNow = 1_700_000_000_000L
        val repository = RoomTierRepository(dao, imageStore) { fixedNow }

        repository.deleteTierLists(listOf(1L, 2L))

        assertEquals(listOf(listOf(1L, 2L) to fixedNow), dao.markedListDeletes)
    }

    @Test
    fun `permanent delete removes single entries by id`() = runBlocking {
        val dao = RecordingTierDao()
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        repository.deleteTierListPermanently(3L)
        repository.deleteTierItemPermanently(9L)

        assertEquals(listOf(3L), dao.permanentlyDeletedListIds)
        assertEquals(listOf(9L), dao.permanentlyDeletedItemIds)
    }

    // Only lists go to the trash: a card leaves no row behind, deletedAt or not.
    @Test
    fun `deleting a card removes its row for good and closes the gap`() = runBlocking {
        val dao = boardWithThreeCards()
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        val removed = requireNotNull(repository.deleteTierItem(MOON))

        assertTrue(dao.items.none { it.id == MOON })
        assertTrue(dao.items.all { it.deletedAt == null })
        assertEquals(listOf(ARRIVAL to 0, ALIEN to 1), dao.items.filter { it.tierId == S_TIER }.map { it.id to it.position })
        assertEquals(LIST, removed.tierListId)
        assertEquals(S_TIER, removed.tierId)
        assertEquals(1, removed.position)
        assertEquals("uid-moon", removed.uid)
        assertEquals(TierItemSource.GENERATED, removed.source)
    }

    @Test
    fun `undo puts the card back in its tier at its place, as it was`() = runBlocking {
        val dao = boardWithThreeCards()
        val repository = RoomTierRepository(dao, imageStore) { 0L }
        val before = dao.items.single { it.id == MOON }

        repository.restoreTierItem(requireNotNull(repository.deleteTierItem(MOON)))

        assertEquals(listOf(ARRIVAL, MOON, ALIEN), dao.getAllTierItemsByTierId(S_TIER).map { it.id })
        assertEquals(before, dao.items.single { it.id == MOON })
    }

    @Test
    fun `undo puts the card at the end of the pool where its tier has gone`() = runBlocking {
        val dao = boardWithThreeCards()
        val repository = RoomTierRepository(dao, imageStore) { 0L }
        val removed = requireNotNull(repository.deleteTierItem(MOON))

        repository.deleteTierToPool(S_TIER)
        repository.restoreTierItem(removed)

        assertEquals(listOf(ARRIVAL, ALIEN, MOON), dao.getAllTierItemsByTierId(POOL).map { it.id })
        val back = dao.items.single { it.id == MOON }
        assertEquals("/pictures/moon.jpg", back.imageUrl)
        assertEquals("uid-moon", back.uid)
        assertEquals("GENERATED", back.source)
        assertEquals("© Jane Doe, CC BY", back.credit)
    }

    @Test
    fun `the picture outlives the card until Undo is let go, and goes then unless another card shows it`() = runBlocking {
        val own = File(imagesDir, "own.jpg").apply { writeText("moon") }
        val shared = File(imagesDir, "shared.jpg").apply { writeText("alien") }
        val dao = boardWithThreeCards(moonImage = own.absolutePath, alienImage = shared.absolutePath)
        dao.insertTierItem(TierItemEntity(tierId = POOL, position = 0, title = "Alien again", imageUrl = shared.absolutePath))
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        val moon = requireNotNull(repository.deleteTierItem(MOON))
        val alien = requireNotNull(repository.deleteTierItem(ALIEN))
        assertTrue(own.exists())

        repository.forgetRemovedTierItem(moon)
        repository.forgetRemovedTierItem(alien)

        assertFalse(own.exists())
        assertTrue(shared.exists())
    }

    @Test
    fun `a deleted card takes its picture off the cover, and Undo puts it back`() = runBlocking {
        val dao = boardWithThreeCards()
        dao.lists[LIST] = dao.lists.getValue(LIST).copy(coverImageUrl = MOON_PICTURE)
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        val removed = requireNotNull(repository.deleteTierItem(MOON))
        assertNull(dao.lists.getValue(LIST).coverImageUrl)

        repository.restoreTierItem(removed)
        assertEquals(MOON_PICTURE, dao.lists.getValue(LIST).coverImageUrl)
    }

    @Test
    fun `a deleted card's picture leaves a collage, and the rest of it stays`() = runBlocking {
        val dao = boardWithThreeCards()
        dao.lists[LIST] = dao.lists.getValue(LIST).copy(coverImageUrls = listOf(MOON_PICTURE, "https://a", "https://b").joinToString("\n"))
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        repository.deleteTierItem(MOON)

        assertEquals("https://a\nhttps://b", dao.lists.getValue(LIST).coverImageUrls)
        assertNull(dao.lists.getValue(LIST).coverImageUrl)
    }

    @Test
    fun `the cover keeps a picture another card still shows, or one found elsewhere`() = runBlocking {
        val dao = boardWithThreeCards(alienImage = MOON_PICTURE)
        dao.lists[LIST] = dao.lists.getValue(LIST).copy(coverImageUrl = MOON_PICTURE)
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        assertNull(requireNotNull(repository.deleteTierItem(MOON)).coverBefore)
        assertEquals(MOON_PICTURE, dao.lists.getValue(LIST).coverImageUrl)

        dao.lists[LIST] = dao.lists.getValue(LIST).copy(coverImageUrl = "https://found/elsewhere.jpg")
        repository.deleteTierItem(ALIEN)
        assertEquals("https://found/elsewhere.jpg", dao.lists.getValue(LIST).coverImageUrl)
    }

    @Test
    fun `Undo leaves a cover chosen since the card went`() = runBlocking {
        val dao = boardWithThreeCards()
        dao.lists[LIST] = dao.lists.getValue(LIST).copy(coverImageUrl = MOON_PICTURE)
        val repository = RoomTierRepository(dao, imageStore) { 0L }
        val removed = requireNotNull(repository.deleteTierItem(MOON))

        repository.setCover(LIST, listOf("https://new/cover.jpg"))
        repository.restoreTierItem(removed)

        assertEquals("https://new/cover.jpg", dao.lists.getValue(LIST).coverImageUrl)
    }

    @Test
    fun `cards removed together for good take their pictures off the cover too`() = runBlocking {
        val dao = boardWithThreeCards()
        dao.lists[LIST] = dao.lists.getValue(LIST).copy(coverImageUrl = MOON_PICTURE)
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        listOf(ARRIVAL, MOON).forEach { repository.deleteTierItemPermanently(it) }

        assertNull(dao.lists.getValue(LIST).coverImageUrl)
    }

    // An older phone or the web's older data may still carry a trashed card.
    @Test
    fun `the trash lists only lists, never a card trashed elsewhere`() = runBlocking {
        val dao = boardWithThreeCards()
        dao.insertTierItem(TierItemEntity(tierId = S_TIER, position = 3, title = "Old", imageUrl = null, deletedAt = 5L))
        dao.lists[TRASHED_LIST] = TierListEntity(id = TRASHED_LIST, title = "Games", deletedAt = 7L)
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        val entries = repository.getTrashEntries()

        assertEquals(listOf(TRASHED_LIST), entries.map { (it as TrashEntry.DeletedList).id })
        assertEquals(listOf(ARRIVAL, MOON, ALIEN), dao.getAllTierItemsByTierId(S_TIER).map { it.id })
    }

    @Test
    fun `emptying the trash purges cards trashed elsewhere and keeps the rest`() = runBlocking {
        val dao = boardWithThreeCards()
        val old = dao.insertTierItem(TierItemEntity(tierId = S_TIER, position = 3, title = "Old", imageUrl = null, deletedAt = 5L))
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        repository.emptyTrash()

        assertTrue(dao.items.none { it.id == old })
        assertEquals(listOf(ARRIVAL, MOON, ALIEN), dao.getAllTierItemsByTierId(S_TIER).map { it.id })
    }

    @Test
    fun `emptying the trash around a list still public purges cards trashed elsewhere too`() = runBlocking {
        val dao = boardWithThreeCards()
        val old = dao.insertTierItem(TierItemEntity(tierId = S_TIER, position = 3, title = "Old", imageUrl = null, deletedAt = 5L))
        dao.lists[TRASHED_LIST] = TierListEntity(id = TRASHED_LIST, title = "Games", deletedAt = 7L)
        dao.lists[STILL_PUBLIC] = TierListEntity(id = STILL_PUBLIC, title = "Films", deletedAt = 8L, publishedId = "pub-1")
        val repository = RoomTierRepository(dao, imageStore) { 0L }

        repository.emptyTrash(keepingListIds = setOf(STILL_PUBLIC))

        assertTrue(dao.items.none { it.id == old })
        assertEquals(listOf(TRASHED_LIST), dao.permanentlyDeletedListIds)
        assertNotNull(dao.lists[STILL_PUBLIC])
        assertEquals(setOf(LIST, STILL_PUBLIC), dao.lists.keys)
    }

    @Test
    fun `emptying the trash leaves another account's trash where it is`() = runBlocking {
        val dao = boardWithThreeCards()
        dao.lists[TRASHED_LIST] = TierListEntity(id = TRASHED_LIST, title = "Games", deletedAt = 7L, ownerUid = "ann")
        dao.lists[STILL_PUBLIC] = TierListEntity(id = STILL_PUBLIC, title = "Films", deletedAt = 8L, ownerUid = "bob")
        val repository = RoomTierRepository(dao, imageStore, owner = { "bob" }) { 0L }

        val entries = repository.getTrashEntries()
        repository.emptyTrash()

        assertEquals(listOf(STILL_PUBLIC), entries.map { (it as TrashEntry.DeletedList).id })
        assertNotNull(dao.lists[TRASHED_LIST])
        assertEquals(setOf(LIST, TRASHED_LIST), dao.lists.keys)
    }

    private suspend fun boardWithThreeCards(moonImage: String? = "/pictures/moon.jpg", alienImage: String? = null) = RecordingTierDao().apply {
        lists[LIST] = TierListEntity(id = LIST, title = "Sci-fi films")
        tiers += TierEntity(id = S_TIER, tierListId = LIST, position = 0, label = "S", colorLight = "#000000", colorDark = "#000000")
        tiers += TierEntity(id = POOL, tierListId = LIST, position = 1, label = "Pool", colorLight = "#000000", colorDark = "#000000", isPool = true)
        insertTierItem(TierItemEntity(id = ARRIVAL, tierId = S_TIER, position = 0, title = "Arrival", imageUrl = null))
        insertTierItem(
            TierItemEntity(
                id = MOON,
                tierId = S_TIER,
                position = 1,
                title = "Moon",
                imageUrl = moonImage,
                source = "GENERATED",
                uid = "uid-moon",
                credit = "© Jane Doe, CC BY",
            ),
        )
        insertTierItem(TierItemEntity(id = ALIEN, tierId = S_TIER, position = 2, title = "Alien", imageUrl = alienImage))
    }

    private companion object {
        const val LIST = 1L
        const val TRASHED_LIST = 2L
        const val STILL_PUBLIC = 3L
        const val S_TIER = 10L
        const val POOL = 30L
        const val ARRIVAL = 100L
        const val MOON = 101L
        const val ALIEN = 102L
        const val MOON_PICTURE = "/pictures/moon.jpg"
    }
}

/** Keeps rows the way the database would, for the dao's own transactions to run against. */
private class RecordingTierDao : TierDao {
    val lists = mutableMapOf<Long, TierListEntity>()
    val tiers = mutableListOf<TierEntity>()
    val items = mutableListOf<TierItemEntity>()
    private var nextItemId = 1_000L

    override suspend fun setFavouritedAt(id: Long, at: Long?) = Unit

    override suspend fun setCategory(id: Long, category: String?) = Unit

    override suspend fun setAnonymous(id: Long, anonymous: Boolean) = Unit
    override suspend fun setPlay(id: Long, play: String?) = Unit

    override suspend fun setCover(id: Long, coverImageUrl: String?, coverImageUrls: String?) {
        lists.computeIfPresent(id) { _, list -> list.copy(coverImageUrl = coverImageUrl, coverImageUrls = coverImageUrls) }
    }

    override suspend fun countPublishedLists(owner: String?): Int = 0

    override suspend fun countBoards(owner: String?): Int = 0

    override suspend fun cardImageUrls(owner: String?, limit: Int): List<String> = emptyList()

    override suspend fun getTierCounts(owner: String?): List<TierCountRow> = emptyList()

    override suspend fun getPreviewImages(tierListId: Long, limit: Int): List<String> = emptyList()

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) = Unit

    override suspend fun setRankingOf(id: Long, listId: String?) = Unit

    override suspend fun setTopic(id: Long, topicId: String?, topicName: String?, newTopicName: String?) = Unit

    val markedListDeletes = mutableListOf<Pair<List<Long>, Long>>()
    val permanentlyDeletedListIds = mutableListOf<Long>()
    val permanentlyDeletedItemIds = mutableListOf<Long>()

    override suspend fun markTierListsDeleted(ids: List<Long>, deletedAt: Long) {
        markedListDeletes += ids to deletedAt
    }

    override suspend fun deleteTierListById(id: Long): Int {
        permanentlyDeletedListIds += id
        dropList(id)
        return 1
    }

    override suspend fun forgetTrashedBoardsInOtherAccounts(owner: String?) = Unit

    override suspend fun forgetBoardInOtherAccounts(id: Long, account: String?) = Unit

    override suspend fun deleteTierItemById(id: Long): Int {
        permanentlyDeletedItemIds += id
        return if (items.removeAll { it.id == id }) 1 else 0
    }

    override suspend fun insertTierList(tierList: TierListEntity): Long = 0L
    override suspend fun getTierListById(id: Long): TierListEntity? = lists[id]?.takeIf { it.deletedAt == null }
    override suspend fun getAllTierLists(owner: String?): List<TierListEntity> = emptyList()
    override suspend fun getAllTierListsWithTiers(owner: String?): List<TierListWithTiers> = emptyList()
    override suspend fun getTierListWithTiers(id: Long, owner: String?): TierListWithTiers? = null
    override suspend fun getPublishedTierListsWithTiers(owner: String?): List<TierListWithTiers> = emptyList()
    override suspend fun getTierListWithTiersPublishedAs(publishedId: String, owner: String?): TierListWithTiers? = null
    override suspend fun restoreTierLists(ids: List<Long>) = Unit
    override suspend fun updateTierListDisplayMode(id: Long, displayMode: String) = Unit
    override suspend fun updateTierListTitle(id: Long, title: String) = Unit
    override suspend fun getDeletedTierLists(owner: String?): List<DeletedTierListRow> =
        lists.values.filter { it.deletedAt != null && it.ownerUid == owner }.sortedByDescending { it.deletedAt }.map { list ->
            DeletedTierListRow(list.id, list.title, itemCount = 0, deletedAt = list.deletedAt!!, list.publishedId, list.rankingOf)
        }

    override suspend fun deleteAllTrashedTierLists(owner: String?) {
        lists.values.filter { it.deletedAt != null && it.ownerUid == owner }.forEach { dropList(it.id) }
    }

    override suspend fun deleteAllTrashedTierItems(owner: String?) {
        items.removeAll { it.deletedAt != null }
    }

    override suspend fun insertTier(tier: TierEntity): Long = 0L
    override suspend fun renameTier(id: Long, label: String, caption: String?) = Unit
    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) = Unit
    override suspend fun updateTierPosition(id: Long, position: Int) = Unit
    override suspend fun getTierById(id: Long): TierEntity? = tiers.firstOrNull { it.id == id }
    override suspend fun getAllTiersByTierListId(tierListId: Long): List<TierEntity> =
        tiers.filter { it.tierListId == tierListId }.sortedBy { it.position }

    override suspend fun deleteTierById(id: Long): Int {
        items.removeAll { it.tierId == id }
        return if (tiers.removeAll { it.id == id }) 1 else 0
    }

    override suspend fun shiftTiersFrom(tierListId: Long, fromPosition: Int) = Unit

    override suspend fun insertTierItem(tierItem: TierItemEntity): Long {
        val id = tierItem.id.takeIf { it != 0L } ?: nextItemId++
        check(items.none { it.id == id || it.uid == tierItem.uid }) { "UNIQUE constraint failed" }
        items += tierItem.copy(id = id)
        return id
    }

    override suspend fun getAllTierItemsByTierId(tierId: Long): List<TierItemEntity> =
        items.filter { it.tierId == tierId && it.deletedAt == null }.sortedBy { it.position }

    override suspend fun getTrashedItemsByTierId(tierId: Long): List<TierItemEntity> =
        items.filter { it.tierId == tierId && it.deletedAt != null }

    override suspend fun reassignTierItem(id: Long, toTierId: Long, position: Int) =
        update(id) { it.copy(tierId = toTierId, position = position) }

    override suspend fun getTierItemById(id: Long): TierItemEntity? = items.firstOrNull { it.id == id && it.deletedAt == null }
    override suspend fun updateTierItemPosition(id: Long, position: Int) = update(id) { it.copy(position = position) }
    override suspend fun updateTierItemTierAndPosition(id: Long, tierId: Long, position: Int) =
        update(id) { it.copy(tierId = tierId, position = position) }

    override suspend fun compactTierPositionsAfter(tierId: Long, afterPosition: Int) {
        items.replaceAll { if (it.tierId == tierId && it.position > afterPosition) it.copy(position = it.position - 1) else it }
    }

    override suspend fun shiftTierPositionsFrom(tierId: Long, fromPosition: Int) {
        items.replaceAll { if (it.tierId == tierId && it.position >= fromPosition) it.copy(position = it.position + 1) else it }
    }

    override suspend fun updateTierItemImage(id: Long, imageUrl: String?) = update(id) { it.copy(imageUrl = imageUrl) }
    override suspend fun getTierItemImageById(id: Long): String? = items.firstOrNull { it.id == id }?.imageUrl
    override suspend fun getAllImageRefs(): List<String> = items.mapNotNull { it.imageUrl }

    private fun update(id: Long, change: (TierItemEntity) -> TierItemEntity) {
        items.replaceAll { if (it.id == id) change(it) else it }
    }

    private fun dropList(id: Long) {
        lists.remove(id)
        val tierIds = tiers.filter { it.tierListId == id }.map { it.id }.toSet()
        tiers.removeAll { it.id in tierIds }
        items.removeAll { it.tierId in tierIds }
    }
}
