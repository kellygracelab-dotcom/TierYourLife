package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.community.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.community.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** The same stand-in id the community reader uses for its unranked strip. */
private const val POOL_TIER_ID = -1L

sealed interface ModerationUiState {
    data object Loading : ModerationUiState

    data class Ready(
        val reports: List<ModerationReport>,
        val settling: String? = null,
        /** Which row the second pane is showing, on a window wide enough. */
        val looking: String? = null,
        val pendingTakeDown: PendingTakeDown? = null,
    ) : ModerationUiState

    data object Failed : ModerationUiState
}

/** Out of the queue and not yet sent: the server deletes for good, so not sending is the only undo. */
data class PendingTakeDown(
    val report: ModerationReport,
    val position: Int,
    val ban: BanLength?,
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ModerationUiState>(ModerationUiState.Loading)
    val state: StateFlow<ModerationUiState> = _state.asStateFlow()

    /** For the pane beside the queue: a complaint takes the list out of the feed, so the queue is the only place it can be looked at. */
    private val _looking = MutableStateFlow<CommunityListUiState>(CommunityListUiState.Loading)
    val looking: StateFlow<CommunityListUiState> = _looking.asStateFlow()

    fun look(listId: String) {
        val shown = _state.value as? ModerationUiState.Ready ?: return
        if (shown.looking == listId) return
        _state.value = shown.copy(looking = listId)
        _looking.value = CommunityListUiState.Loading
        viewModelScope.launch {
            _looking.value = community.open(listId).fold(
                onSuccess = { published ->
                    // Exactly as a reader sees it: the author's tiers and cards.
                    CommunityListUiState.Success(
                        list = TierList(
                            id = 0,
                            title = published.summary.title,
                            tiers = published.tiers.map { it.copy(items = emptyList()) } +
                                Tier(
                                    id = POOL_TIER_ID,
                                    label = "Unranked",
                                    colorLight = "#DAD7E0",
                                    colorDark = "#46464F",
                                    items = published.items,
                                    isPool = true,
                                ),
                            authorName = published.summary.authorName,
                        ),
                        authorName = published.summary.authorName,
                        authorUid = published.summary.authorUid,
                        authorPhotoUrl = published.summary.authorPhotoUrl,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Opening a reported list failed")
                    CommunityListUiState.Error
                },
            )
        }
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = community.reports().fold(
                onSuccess = { ModerationUiState.Ready(it) },
                onFailure = { error ->
                    Timber.w(error, "Reading the report queue failed")
                    ModerationUiState.Failed
                },
            )
        }
    }

    /** Takes the row out and sends nothing yet; the screen calls [confirmTakeDown] or [undoTakeDown] once the moment has passed. */
    fun takeDown(listId: String, ban: BanLength? = null) {
        val shown = readyToAct() ?: return
        val position = shown.reports.indexOfFirst { it.listId == listId }
        if (position < 0) return
        _state.value = shown.copy(
            reports = shown.reports.filterIndexed { index, _ -> index != position },
            pendingTakeDown = PendingTakeDown(shown.reports[position], position, ban),
        )
    }

    fun undoTakeDown() {
        val shown = _state.value as? ModerationUiState.Ready ?: return
        val pending = shown.pendingTakeDown ?: return
        _state.value = shown.copy(reports = shown.reports.putBack(pending), pendingTakeDown = null)
    }

    /** One call: no moment in which the list is gone and nobody has answered for it. */
    fun confirmTakeDown() {
        val shown = _state.value as? ModerationUiState.Ready ?: return
        val pending = shown.pendingTakeDown ?: return
        val listId = pending.report.listId
        // Stays out of sight while it is sent; a failure puts it back where it was.
        settle(
            listId = listId,
            shown = shown.copy(reports = shown.reports.putBack(pending), pendingTakeDown = null),
            whileSettling = shown.copy(pendingTakeDown = null),
        ) { community.takeDown(listId, pending.ban) }
    }

    fun dismiss(listId: String) {
        val shown = readyToAct() ?: return
        settle(listId, shown) { community.dismissReports(listId) }
    }

    private fun readyToAct(): ModerationUiState.Ready? =
        (_state.value as? ModerationUiState.Ready)?.takeIf { it.settling == null && it.pendingTakeDown == null }

    /** Both endings close every complaint about the list. Failing leaves them all: a queue that quietly loses entries is worse than one that will not budge. */
    private fun settle(
        listId: String,
        shown: ModerationUiState.Ready,
        whileSettling: ModerationUiState.Ready = shown,
        act: suspend () -> Result<Unit>,
    ) {
        _state.value = whileSettling.copy(settling = listId)
        viewModelScope.launch {
            act().fold(
                onSuccess = {
                    val left = shown.reports.filterNot { it.listId == listId }
                    _state.value = ModerationUiState.Ready(left)
                    // The pane was showing the one just settled; move it on.
                    left.firstOrNull()?.let { look(it.listId) }
                },
                onFailure = { error ->
                    Timber.w(error, "Settling reports about $listId failed")
                    _state.value = shown.copy(settling = null)
                },
            )
        }
    }
}

private fun List<ModerationReport>.putBack(pending: PendingTakeDown): List<ModerationReport> =
    toMutableList().apply { add(pending.position.coerceAtMost(size), pending.report) }
