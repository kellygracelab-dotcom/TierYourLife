package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.RemovedTierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.model.summary
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeTierRepository(
    initial: TierList,
    publishedId: String? = null,
) : TierRepository {

    private var list = initial.copy(publishedId = publishedId)

    /** What a sync run would have written: another device's edit, landed. */
    fun arrive(change: (TierList) -> TierList) {
        list = change(list)
    }

    val publishedIds = mutableListOf<Pair<Long, String?>>()

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) {
        publishedIds += id to publishedId
        list = list.copy(publishedId = publishedId)
        if (publishedId != null && holdReadsAfterPublish) readsAreHeld = true
    }

    val rankingsSet = mutableListOf<Pair<Long, String?>>()

    override suspend fun setRankingOf(id: Long, listId: String?) {
        rankingsSet += id to listId
        list = list.copy(rankingOf = listId)
    }

    override suspend fun setRankingChoice(id: Long, choice: RankingChoice) {
        list = list.copy(rankingChoice = choice)
    }

    override suspend fun setRankingOrigin(id: Long, sourceVersion: Long) {
        list = list.copy(originVersion = sourceVersion)
    }

    override suspend fun publishedStanding(): PublishedStanding = PublishedStanding()

    override suspend fun boardPublishedAs(publishedId: String): TierList? = null

    override suspend fun setFavouritedAt(id: Long, at: Long?) = Unit

    override suspend fun setCategory(id: Long, category: String?) {
        list = list.copy(category = category)
    }

    override suspend fun setAnonymous(id: Long, anonymous: Boolean) {
        list = list.copy(anonymous = anonymous)
    }

    override suspend fun setPlay(id: Long, play: String?) {
        list = list.copy(play = play)
    }

    override suspend fun setCover(id: Long, pictures: List<String>) {
        list = list.copy(coverImageUrl = pictures.singleOrNull(), coverImageUrls = pictures.takeIf { it.size > 1 }.orEmpty())
    }

    val topicsSet = mutableListOf<List<Any?>>()

    override suspend fun setTopic(id: Long, topicId: String?, topicName: String?, newTopicName: String?) {
        topicsSet += listOf(id, topicId, topicName, newTopicName)
        list = list.copy(topicId = topicId, topicName = topicName, newTopicName = newTopicName)
    }

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
        takenFrom: String?,
    ): Long = 0

    private var nextItemId = 1L
    var lastAddedImageUrl: String? = "not yet called"
        private set
    val attachedSources = mutableListOf<String>()
    val permanentlyDeletedIds = mutableListOf<Long>()
    var lastReorderedIds: List<Long>? = null
        private set
    val reorderStarted = CompletableDeferred<Unit>()
    private val reorderGate = CompletableDeferred<Unit>()

    fun releaseReorder() {
        reorderGate.complete(Unit)
    }

    var holdReadsAfterPublish = false
    var readsAreHeld = false
    private val heldRead = CompletableDeferred<Unit>()

    val boardReads = MutableStateFlow(0)

    override suspend fun getTierListById(id: Long): TierList? {
        boardReads.update { it + 1 }
        if (readsAreHeld) heldRead.await()
        return list
    }

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long {
        lastAddedImageUrl = imageUrl
        val id = nextItemId++
        val pool = list.tiers.single { it.isPool }
        val newItem = TierItem(id = id, title = title, imageUrl = imageUrl, source = source)
        val updatedPool = pool.copy(items = pool.items + newItem)
        list = list.copy(tiers = list.tiers.map { if (it.id == pool.id) updatedPool else it })
        return id
    }

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) {
        attachedSources += sourceUri
    }

    var summaryReads = 0
        private set

    override suspend fun getTierListSummaries(): List<TierListSummary> {
        summaryReads++
        return listOf(list.summary())
    }

    override suspend fun getAllTierLists(): List<TierList> = unsupported()
    override suspend fun createTierList(title: String, captions: List<String>): Long = unsupported()
    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = unsupported()
    override suspend fun renameTierList(id: Long, title: String) = unsupported()
    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = unsupported()
    private val movesGate = CompletableDeferred<Unit>()

    fun releaseMoves() {
        movesGate.complete(Unit)
    }

    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        movesGate.await()
        list = list.withItemMoved(itemId, toTierId, toPosition)
    }
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
    override suspend fun reorderTiers(orderedTierIds: List<Long>) {
        lastReorderedIds = orderedTierIds
        reorderStarted.complete(Unit)
        reorderGate.await()
        val orderedSet = orderedTierIds.toSet()
        val byId = list.tiers.associateBy { it.id }
        val queue = ArrayDeque(orderedTierIds.mapNotNull { byId[it] })
        list = list.copy(tiers = list.tiers.map { tier -> if (tier.id in orderedSet) queue.removeFirst() else tier })
    }
    override suspend fun deleteTierLists(ids: List<Long>) = unsupported()
    override suspend fun restoreTierLists(ids: List<Long>) = unsupported()
    val restoredItems = mutableListOf<RemovedTierItem>()
    val forgotten = CompletableDeferred<RemovedTierItem>()

    override suspend fun deleteTierItem(id: Long): RemovedTierItem? {
        val tier = list.tiers.firstOrNull { tier -> tier.items.any { it.id == id } } ?: return null
        val position = tier.items.indexOfFirst { it.id == id }
        val item = tier.items[position]
        list = list.copy(tiers = list.tiers.map { if (it.id == tier.id) it.copy(items = it.items - item) else it })
        return RemovedTierItem(
            id = item.id,
            uid = item.uid,
            tierListId = list.id,
            tierId = tier.id,
            position = position,
            title = item.title,
            imageUrl = item.imageUrl,
            source = item.source,
        )
    }

    override suspend fun restoreTierItem(removed: RemovedTierItem) {
        restoredItems += removed
        val item = TierItem(removed.id, removed.title, removed.imageUrl, removed.source, removed.uid)
        list = list.copy(
            tiers = list.tiers.map { tier ->
                if (tier.id != removed.tierId) {
                    tier
                } else {
                    tier.copy(items = tier.items.toMutableList().apply { add(removed.position.coerceAtMost(size), item) })
                }
            },
        )
    }

    override suspend fun forgetRemovedTierItem(removed: RemovedTierItem) {
        forgotten.complete(removed)
    }
    override suspend fun deleteTierListPermanently(id: Long) {
        permanentlyDeletedIds += id
    }
    override suspend fun deleteTierItemPermanently(id: Long) {
        permanentlyDeletedIds += id
        list = list.copy(
            tiers = list.tiers.map { tier -> tier.copy(items = tier.items.filterNot { it.id == id }) },
        )
    }
    override suspend fun emptyTrash(keepingListIds: Set<Long>) = unsupported()
    override suspend fun getTrashEntries(): List<TrashEntry> = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("Not used by TierDetailViewModel manual-entry tests")
}
