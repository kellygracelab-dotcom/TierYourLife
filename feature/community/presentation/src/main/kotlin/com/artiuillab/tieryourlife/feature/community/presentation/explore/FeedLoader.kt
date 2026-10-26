package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedFreshness
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Whether there is anything to show: skeletons, lists, or the block that says why not. */
enum class FeedPhase { Loading, Ready, Failed }

/** One feed as the screen draws it (docs/design-spec-community-explore.md §13). */
data class FeedPane(
    val lists: List<PublishedListSummary> = emptyList(),
    val phase: FeedPhase = FeedPhase.Loading,
    val canLoadMore: Boolean = false,
    val loadingMore: Boolean = false,
    /** The pull-to-refresh indicator: the only spinner, and only when the person pulled. */
    val refreshing: Boolean = false,
    /** Retry was pressed and is being answered; the button waits, without a spinner. */
    val retrying: Boolean = false,
    /** Newer lists came while the person was scrolled down: held, with the New lists pill. */
    val newListsWaiting: Boolean = false,
    /** When what is on screen was fetched, while the offline strip should say so; null when it should not show. */
    val offlineSinceMillis: Long? = null,
    /** A following feed from somebody who follows nothing at all. */
    val followingNobody: Boolean = false,
    /** Put away in this sitting, id to whether it was reported: a quiet note where the card was. */
    val justHidden: Map<String, Boolean> = emptyMap(),
)

sealed interface FeedNotice {
    /** A pull that failed: what is on screen stays, and says how old it is. */
    data class RefreshFailed(val showingFromMillis: Long) : FeedNotice
}

/**
 * A feed opened from what the phone kept and refreshed by the rules of §13:
 * the kept page shows at once, the server is asked when it is missing or older
 * than five minutes, an answer that lands while the person is scrolled down is
 * held behind the New lists pill, and a failure leaves what is on screen with
 * at most a strip saying how old it is.
 */
class FeedLoader(
    private val key: String,
    private val scope: CoroutineScope,
    private val cache: CommunityFeedCache,
    private val clock: CommunityClock,
    private val visible: (PublishedListSummary) -> Boolean = { true },
    private val fetch: suspend (after: String?) -> Result<CommunityPage>,
) {
    private val _pane = MutableStateFlow(FeedPane())
    val pane: StateFlow<FeedPane> = _pane.asStateFlow()

    private val _notices = Channel<FeedNotice>(Channel.BUFFERED)
    val notices: Flow<FeedNotice> = _notices.receiveAsFlow()

    private var shown: List<PublishedListSummary> = emptyList()
    private var cursor: String? = null
    private var fetchedAt: Long? = null
    private var held: Pair<CommunityPage, Long>? = null
    private var scrolledDown = false
    private var lastAttemptFailed = false
    private var lastAutomaticRetry: Long? = null
    private var readKept = false
    private var refreshJob: Job? = null
    private var moreJob: Job? = null

    /** On every arrival: the kept page first, then the server when that is missing, old, or last failed. */
    fun open() {
        scope.launch {
            if (!readKept) {
                readKept = true
                cache.read(key)?.let { kept -> show(kept.page, kept.fetchedAtMillis) }
            }
            val at = fetchedAt
            if (at == null || lastAttemptFailed || FeedFreshness.isStale(at, clock.nowMillis())) refresh(pulled = false)
        }
    }

    fun pullToRefresh() = refresh(pulled = true)

    /** Asked again from nothing: what it is made of changed (a first follow), so the old page is wrong, and skeletons stand in. */
    fun reload() {
        refreshJob?.cancel()
        shown = emptyList()
        fetchedAt = null
        held = null
        _pane.value = FeedPane()
        refresh(pulled = false)
    }

    /** The strip's or the error block's button. */
    fun retry() {
        if (_pane.value.retrying) return
        _pane.update { it.copy(retrying = true) }
        refresh(pulled = false)
    }

    /** The network came back: a feed that failed tries again on its own, at most once every ten seconds. */
    fun onNetworkBack() {
        if (!lastAttemptFailed && _pane.value.phase != FeedPhase.Failed) return
        val now = clock.nowMillis()
        if (!FeedFreshness.mayRetry(lastAutomaticRetry, now)) return
        lastAutomaticRetry = now
        refresh(pulled = false)
    }

    /** Back at the top, a held answer applies by itself; away from it, the next answer waits. */
    fun onScrolledDown(down: Boolean) {
        scrolledDown = down
        if (!down) applyHeld()
    }

    /** The pill: the held answer, with the feed back at the top. */
    fun applyHeld() {
        val (page, at) = held ?: return
        show(page, at)
    }

    fun loadMore() {
        val pane = _pane.value
        val after = cursor ?: return
        if (pane.phase != FeedPhase.Ready || pane.loadingMore || moreJob?.isActive == true) return
        _pane.update { it.copy(loadingMore = true) }
        moreJob = scope.launch {
            fetch(after).fold(
                onSuccess = { page ->
                    cursor = page.nextCursor
                    val known = shown.mapTo(mutableSetOf()) { it.id }
                    shown = shown + page.lists.filterNot { it.id in known }
                    _pane.update {
                        it.copy(lists = shown.filter(visible), canLoadMore = cursor != null, loadingMore = false)
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Loading another page of %s failed", key)
                    _pane.update { it.copy(loadingMore = false) }
                },
            )
        }
    }

    /** What is hidden changed: the lists on screen are filtered again, with the note where a card went. */
    fun refilter(justHidden: Map<String, Boolean> = _pane.value.justHidden) {
        _pane.update { it.copy(lists = shown.filter { summary -> visible(summary) || summary.id in justHidden }, justHidden = justHidden) }
    }

    private fun refresh(pulled: Boolean) {
        if (refreshJob?.isActive == true) {
            if (pulled) _pane.update { it.copy(refreshing = true) }
            return
        }
        if (pulled) _pane.update { it.copy(refreshing = true) }
        refreshJob = scope.launch {
            val result = fetch(null)
            val now = clock.nowMillis()
            result.fold(
                onSuccess = { page -> arrived(page, now, pulled) },
                onFailure = { error -> failed(error, now, pulled) },
            )
        }
    }

    private suspend fun arrived(page: CommunityPage, now: Long, pulled: Boolean) {
        lastAttemptFailed = false
        cache.write(key, page, now)
        val different = page.lists.map { it.id } != shown.map { it.id }
        if (holds(pulled, different)) {
            held = page to now
            fetchedAt = now
            _pane.update { it.copy(newListsWaiting = true, refreshing = false, retrying = false, offlineSinceMillis = null) }
        } else {
            show(page, now)
        }
    }

    /** An answer waits behind the pill only when somebody is reading further down and it would change what they read. */
    private fun holds(pulled: Boolean, different: Boolean): Boolean = scrolledDown && !pulled && different && shown.isNotEmpty()

    private suspend fun failed(error: Throwable, now: Long, pulled: Boolean) {
        // A refused App Check token is a failure like any other here: the person never sees the term.
        Timber.w(error, "Loading %s failed", key)
        lastAttemptFailed = true
        val keptAt = fetchedAt
        if (keptAt == null) {
            _pane.update { it.copy(phase = FeedPhase.Failed, refreshing = false, retrying = false) }
            return
        }
        _pane.update {
            it.copy(
                refreshing = false,
                retrying = false,
                offlineSinceMillis = if (pulled) it.offlineSinceMillis else keptAt.takeIf { at -> FeedFreshness.showsOfflineStrip(true, at, now) },
            )
        }
        if (pulled) _notices.send(FeedNotice.RefreshFailed(keptAt))
    }

    private fun show(page: CommunityPage, at: Long) {
        shown = page.lists
        cursor = page.nextCursor
        fetchedAt = at
        held = null
        _pane.update {
            it.copy(
                lists = shown.filter(visible),
                phase = FeedPhase.Ready,
                canLoadMore = cursor != null,
                loadingMore = false,
                refreshing = false,
                retrying = false,
                newListsWaiting = false,
                offlineSinceMillis = null,
                followingNobody = page.followingNobody,
                justHidden = emptyMap(),
            )
        }
    }
}
