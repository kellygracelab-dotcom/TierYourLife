package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.NewPoolItem
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.NewTemplateTier
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.mapper.TOPIC_TAKEN_OFF
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toEntity
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toRemoved
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toStoredCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toSummaries
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.RemovedTierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PublishFingerprint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Every read is of one owner's boards and every new board is theirs: [owner]
 * is the signed-in account, or null for the boards made without one.
 */
class RoomTierRepository internal constructor(
    private val dao: TierDao,
    private val imageStore: TierImageStore,
    private val owner: suspend () -> String? = { null },
    private val nowMillis: () -> Long,
) : TierRepository {

    @Inject constructor(dao: TierDao, imageStore: TierImageStore, ownership: BoardOwnership) :
        this(dao, imageStore, ownership::current, System::currentTimeMillis)

    // Mapped off the main thread: a library is every card of every board, and the callers are view models.
    override suspend fun getTierListById(id: Long): TierList? {
        val row = dao.getTierListWithTiers(id, owner()) ?: return null
        return withContext(Dispatchers.Default) { row.toDomain() }
    }

    override suspend fun getAllTierLists(): List<TierList> {
        val rows = dao.getAllTierListsWithTiers(owner())
        return withContext(Dispatchers.Default) { rows.map { it.toDomain() } }
    }

    override suspend fun getTierListSummaries(): List<TierListSummary> {
        val rows = dao.getSummaryRows(owner(), TierListSummary.PREVIEW_IMAGES)
        return withContext(Dispatchers.Default) { rows.toSummaries() }
    }

    override suspend fun createTierList(title: String, captions: List<String>): Long {
        return dao.createTierListWithDefaultTier(title = title, captions = captions, owner = owner())
    }

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
        takenFrom: String?,
    ): Long = dao.createTierListFromTemplate(
        title = title,
        authorName = authorName,
        takenFrom = takenFrom,
        owner = owner(),
        tiers = tiers.map { tier ->
            NewTemplateTier(
                label = tier.label,
                caption = tier.caption,
                colorLight = tier.colorLight,
                colorDark = tier.colorDark,
                items = tier.items.map { NewPoolItem(it.title, it.imageUrl, it.credit) },
            )
        },
        items = items.map { NewPoolItem(it.title, it.imageUrl, it.credit) },
    )

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) {
        dao.setPublished(id, publishedId, fingerprint)
    }

    override suspend fun setRankingOf(id: Long, listId: String?) {
        dao.setRankingOf(id, listId)
    }

    override suspend fun publishedStanding(): PublishedStanding {
        val rows = dao.getPublishedTierListsWithTiers(owner())
        return withContext(Dispatchers.Default) { standingOf(rows.map { it.toDomain() }) }
    }

    private fun standingOf(published: List<TierList>): PublishedStanding {
        return PublishedStanding(
            canUpdate = published.mapNotNull { it.publishedId }.toSet(),
            knownBehind = published.filter(::isBehind).mapNotNull { board -> board.publishedId }.toSet(),
        )
    }

    private fun isBehind(board: TierList): Boolean =
        board.publishedFingerprint != null &&
            PublishFingerprint.of(board, imageStore::pictureIdOf) != board.publishedFingerprint

    override suspend fun isKnownBehind(board: TierList): Boolean =
        board.publishedId != null && withContext(Dispatchers.Default) { isBehind(board) }

    override suspend fun boardPublishedAs(publishedId: String): TierList? {
        val row = dao.getTierListWithTiersPublishedAs(publishedId, owner()) ?: return null
        return withContext(Dispatchers.Default) { row.toDomain() }
    }

    override suspend fun setFavouritedAt(id: Long, at: Long?) {
        dao.setFavouritedAt(id, at)
    }

    override suspend fun setCategory(id: Long, category: String?) {
        dao.setCategory(id, category)
    }

    override suspend fun setAnonymous(id: Long, anonymous: Boolean) {
        dao.setAnonymous(id, anonymous)
    }

    override suspend fun setPlay(id: Long, play: String?) {
        dao.setPlay(id, play)
    }

    override suspend fun setCover(id: Long, pictures: List<String>) {
        dao.setCover(id, coverImageUrl = pictures.singleOrNull(), coverImageUrls = pictures.toStoredCoverImageUrls())
    }

    override suspend fun setTopic(id: Long, topicId: String?, topicName: String?, newTopicName: String?) {
        dao.setTopic(
            id,
            topicId = topicId?.takeUnless { it == TOPIC_TAKEN_OFF },
            topicName = topicName,
            newTopicName = newTopicName?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    override suspend fun takeTopicOff(id: Long) {
        dao.setTopic(id, topicId = TOPIC_TAKEN_OFF, topicName = null, newTopicName = null)
    }

    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) {
        dao.updateTierListDisplayMode(id, displayMode.name)
    }

    override suspend fun renameTierList(id: Long, title: String) {
        dao.updateTierListTitle(id, title)
    }

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long {
        return dao.addItemToPool(tierListId, title, imageUrl, source.name)
    }

    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) {
        val kept = items.map { draft ->
            val url = draft.imageUrl
            // A copy that could not be made leaves the address: a picture that may go is better than none.
            val imageUrl = if (draft.keepCopy && url != null) imageStore.downloadToInternalStorage(url) ?: url else url
            NewPoolItem(title = draft.title, imageUrl = imageUrl, credit = draft.credit)
        }
        dao.addItemsToPool(tierListId, kept)
    }

    override suspend fun keepPicture(sourceUri: String): String = withContext(Dispatchers.IO) {
        imageStore.copyToInternalStorage(sourceUri)
    }

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) {
        val copyPath = imageStore.copyToInternalStorage(sourceUri)
        val previousImage = dao.getTierItemImageById(itemId)
        dao.updateTierItemImage(itemId, copyPath)
        if (previousImage != null) {
            imageStore.deleteCopy(previousImage)
        }
    }

    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        dao.moveItem(itemId, toTierId, toPosition)
    }

    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long {
        return dao.addTier(tierListId, label, caption, colorLight, colorDark)
    }

    override suspend fun renameTier(id: Long, label: String, caption: String?) {
        dao.renameTier(id, label, caption)
    }

    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) {
        dao.updateTierColors(id, colorLight, colorDark)
    }

    override suspend fun deleteTierToPool(id: Long) {
        dao.deleteTierToPool(id)
    }

    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) {
        dao.restoreTier(tierListId, label, caption, colorLight, colorDark, position, itemIds)
    }

    override suspend fun reorderTiers(orderedTierIds: List<Long>) {
        dao.reorderTiers(orderedTierIds)
    }

    override suspend fun deleteTierLists(ids: List<Long>) {
        dao.markTierListsDeleted(ids, nowMillis())
    }

    override suspend fun restoreTierLists(ids: List<Long>) {
        dao.restoreTierLists(ids)
    }

    override suspend fun deleteTierItem(id: Long): RemovedTierItem? {
        val item = dao.getTierItemById(id) ?: return null
        val tier = dao.getTierById(item.tierId) ?: return null
        dao.removeTierItem(item)
        val coverBefore = takePictureOffCover(tier.tierListId, item.imageUrl)
        return item.toRemoved(tier.tierListId).copy(coverBefore = coverBefore)
    }

    override suspend fun restoreTierItem(removed: RemovedTierItem) {
        dao.putBackTierItem(removed.toEntity(), removed.tierListId)
        val coverBefore = removed.coverBefore ?: return
        val board = dao.getTierListById(removed.tierListId) ?: return
        if (board.coverPictures() == coverBefore - removed.imageUrl) setCover(removed.tierListId, coverBefore)
    }

    override suspend fun forgetRemovedTierItem(removed: RemovedTierItem) {
        removed.imageUrl?.let { letGoOfPicture(it) }
    }

    override suspend fun deleteTierListPermanently(id: Long) {
        dao.deleteTierListPermanently(id, owner())
        imageStore.deleteOrphans(picturesInUse())
    }

    override suspend fun deleteTierItemPermanently(id: Long) {
        val tierListId = dao.getTierItemById(id)?.let { dao.getTierById(it.tierId) }?.tierListId
        val imagePath = dao.getTierItemImageById(id)
        dao.deleteTierItemById(id)
        if (tierListId != null) takePictureOffCover(tierListId, imagePath)
        if (imagePath != null) {
            letGoOfPicture(imagePath)
        }
    }

    override suspend fun emptyTrash(keepingListIds: Set<Long>) {
        dao.emptyTrash(owner(), keepingListIds)
        imageStore.deleteOrphans(picturesInUse())
    }

    // A card with a deletedAt from an older phone or the web stays hidden and unlisted until the trash is emptied.
    override suspend fun getTrashEntries(): List<TrashEntry> =
        dao.getDeletedTierLists(owner()).map { it.toDomain() }

    /**
     * A card's picture leaves the cover with the last card on the board showing it, so it is not
     * published after the card is gone. The cover as it was comes back, for Undo; null where it stayed.
     */
    private suspend fun takePictureOffCover(tierListId: Long, picture: String?): List<String>? {
        picture ?: return null
        val board = dao.getTierListById(tierListId) ?: return null
        val cover = board.coverPictures()
        if (picture !in cover) return null
        val stillShown = dao.getAllTiersByTierListId(tierListId).any { tier ->
            dao.getAllTierItemsByTierId(tier.id).any { it.imageUrl == picture }
        }
        if (stillShown) return null
        setCover(tierListId, cover - picture)
        return cover
    }

    private fun TierListEntity.coverPictures(): List<String> =
        coverImageUrls.toCoverImageUrls().ifEmpty { listOfNotNull(coverImageUrl) }

    /** A board copied from another shares its pictures: the file goes only with the last card showing it. */
    private suspend fun letGoOfPicture(path: String) {
        if (path !in picturesInUse()) imageStore.deleteCopy(path)
    }

    private suspend fun picturesInUse(): List<String> = dao.getAllImageRefs() + dao.getAllCoverRefs()
}
