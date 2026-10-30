package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Said once, by the snackbar or by leaving. */
sealed interface ListNotice {
    /** Their own list came down: the page has nothing left to show. */
    data object TakenDown : ListNotice

    /** A move on Theirs started the reader's own version from [name]'s; [replaced] when Mine had been arranged before. */
    data class StartedFromTheirs(val name: String?, val replaced: Boolean) : ListNotice
}

/**
 * Their own list's page: the board behind it, found, restored from the trash
 * or rebuilt from the list when this phone has lost it, and what becomes of
 * the list itself, its name and its taking down. One of these at a time.
 */
internal class OwnList(
    private val publishedId: String,
    private val community: CommunityRepository,
    private val tiers: TierRepository,
    private val state: MutableStateFlow<CommunityListUiState>,
    private val scope: CoroutineScope,
    private val messages: UserMessages,
    private val tell: suspend (ListNotice) -> Unit,
    /** Reads the list again without the placeholder, once the server holds a new copy. */
    private val reread: suspend () -> Unit,
) {
    private var opened: PublishedListSummary? = null

    fun opened(summary: PublishedListSummary) {
        opened = summary
    }

    /** Left unanswered when this phone cannot be asked: offering to rebuild a board that is here would make a second one. */
    suspend fun find() {
        val board = runSuspendCatching { tiers.boardPublishedAs(publishedId) }
            .onFailure { Timber.i(it, "Not offering the board behind $publishedId") }
            .getOrElse { return }
        val trashed = if (board != null) {
            null
        } else {
            runSuspendCatching { tiers.getTrashEntries() }
                .onFailure { Timber.i(it, "Not looking for the board behind $publishedId in the trash") }
                .getOrElse { return }
                .filterIsInstance<TrashEntry.DeletedList>()
                .firstOrNull { it.publishedId == publishedId }
        }
        change { it.copy(ownBoardId = board?.id, trashedBoardId = trashed?.id, ownBoardLookedUp = true) }
    }

    fun restore() = busy("Restoring the board behind a list") { current ->
        val id = current.trashedBoardId ?: return@busy
        tiers.restoreTierLists(listOf(id))
        change { it.copy(ownBoardId = id, trashedBoardId = null) }
    }

    /**
     * The list as it is out there, made a board again: owned rather than
     * taken, still known as this list so Update sends to the same link, and
     * as named or nameless as it went out.
     */
    fun getBack() = busy("Getting a published list back as a board") { current ->
        val list = current.list
        val id = tiers.createFromTemplate(
            title = list.title,
            authorName = "",
            tiers = list.tiers.filterNot { it.isPool },
            items = list.tiers.filter { it.isPool }.flatMap { it.items },
        )
        tiers.setPublished(id, publishedId, null)
        tiers.setAnonymous(id, current.anonymous)
        current.section?.let { tiers.setCategory(id, it) }
        opened?.topicId?.let { topicId -> tiers.setTopic(id, topicId, opened?.topicName, null) }
        change { it.copy(ownBoardId = id) }
    }

    /** Sent again from the board with the name on or off, over the same id, so every link given out still works. */
    fun publishAnonymously(anonymous: Boolean) {
        val id = (state.value as? CommunityListUiState.Success)?.ownBoardId ?: return
        busy("Publishing a list with its name changed") { current ->
            tiers.setAnonymous(id, anonymous)
            val board = tiers.getTierListById(id) ?: error("The board behind $publishedId is gone")
            community.publish(board).fold(
                onSuccess = { published -> tiers.setPublished(id, published.id, published.fingerprint) },
                onFailure = { error ->
                    tiers.setAnonymous(id, current.anonymous)
                    throw error
                },
            )
            reread()
        }
    }

    /** As from the list of their public lists: down for everyone, and forgotten by the board that sent it. */
    fun takeDown() = busy("Taking down a list from its page") { current ->
        community.unpublish(publishedId).getOrThrow()
        listOfNotNull(current.ownBoardId, current.trashedBoardId).forEach { id ->
            runSuspendCatching { tiers.setPublished(id, null, null) }
                .onFailure { Timber.w(it, "Forgetting $publishedId on this phone failed") }
        }
        tell(ListNotice.TakenDown)
    }

    private fun busy(operation: String, block: suspend (CommunityListUiState.Success) -> Unit) {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (!current.own || current.ownBusy) return
        change { it.copy(ownBusy = true) }
        scope.launch {
            messages.guard(operation) { block(current) }
            change { it.copy(ownBusy = false) }
        }
    }

    private fun change(block: (CommunityListUiState.Success) -> CommunityListUiState.Success) {
        state.update { current -> if (current is CommunityListUiState.Success) block(current) else current }
    }
}
