package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowRefusal
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

    /** Their own list with its board on this phone: the board is where it changes, so the page gives way to it. */
    data class OpenBoard(val boardId: Long) : ListNotice

    /** A guest tapped Follow: the sign-in opens, and the follow waits for it. */
    data object SignInToFollow : ListNotice

    data class FollowRefused(val refusal: FollowRefusal) : ListNotice
}

/**
 * Their own list's page: the board behind it, found, restored from the trash
 * or rebuilt from the list when this phone has lost it, and the list's taking
 * down. A board that is here opens in the page's place, unless the page was
 * asked for from that board. One of these at a time.
 */
internal class OwnList(
    private val publishedId: String,
    private val community: CommunityRepository,
    private val tiers: TierRepository,
    private val state: MutableStateFlow<CommunityListUiState>,
    private val scope: CoroutineScope,
    private val messages: UserMessages,
    private val tell: suspend (ListNotice) -> Unit,
    /** False when the page was asked for from the board itself. */
    private val opensBoard: Boolean = true,
    /** Drops the copy kept on this phone once the list is down. */
    private val forget: suspend () -> Unit = {},
) {
    private var opened: PublishedListSummary? = null
    private var cover: List<String> = emptyList()
    private var handedOver = false

    fun opened(answer: PublishedList) {
        opened = answer.summary
        cover = chosenCoverOf(answer)
    }

    /** Before the server is asked, for a list the card already called theirs. True when the board opens instead. */
    suspend fun openBoardIfHere(): Boolean {
        if (!opensBoard) return false
        val board = runSuspendCatching { tiers.boardPublishedAs(publishedId) }
            .onFailure { Timber.i(it, "Not opening the board behind $publishedId") }
            .getOrNull() ?: return false
        handOver(board.id)
        return true
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
        board?.let { handOver(it.id) }
    }

    fun restore() = busy("Restoring the board behind a list") { current ->
        val id = current.trashedBoardId ?: return@busy
        tiers.restoreTierLists(listOf(id))
        change { it.copy(ownBoardId = id, trashedBoardId = null) }
        handOver(id)
    }

    /**
     * The list as it is out there, made a board again: owned rather than
     * taken, still known as this list so Update sends to the same link, and
     * as named or nameless, and under the same cover, as it went out.
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
        if (cover.isNotEmpty()) tiers.setCover(id, cover)
        opened?.topicId?.let { topicId -> tiers.setTopic(id, topicId, opened?.topicName, null) }
        change { it.copy(ownBoardId = id) }
        handOver(id)
    }

    /** As from the list of their public lists: down for everyone, and forgotten by the board that sent it. */
    fun takeDown() = busy("Taking down a list from its page") { current ->
        community.unpublish(publishedId).getOrThrow()
        forget()
        listOfNotNull(current.ownBoardId, current.trashedBoardId).forEach { id ->
            runSuspendCatching { tiers.setPublished(id, null, null) }
                .onFailure { Timber.w(it, "Forgetting $publishedId on this phone failed") }
        }
        tell(ListNotice.TakenDown)
    }

    private suspend fun handOver(boardId: Long) {
        if (!opensBoard || handedOver) return
        handedOver = true
        tell(ListNotice.OpenBoard(boardId))
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

/**
 * The cover its author chose, as the list went out: one picture, or a
 * collage. With none chosen the server shows the first cards' art, so a
 * collage that is exactly that was nobody's choice, and sending nothing
 * again draws the same.
 */
internal fun chosenCoverOf(list: PublishedList): List<String> {
    list.summary.coverImageUrl?.let { return listOf(it) }
    val automatic = list.items.mapNotNull { it.imageUrl?.takeIf { url -> url.startsWith("https://") } }.take(AUTOMATIC_COVER_PICTURES)
    return list.summary.previewImages.takeIf { it != automatic }.orEmpty()
}

/** As many cards' pictures as the server draws a cover from when nobody chose one. */
private const val AUTOMATIC_COVER_PICTURES = 6
