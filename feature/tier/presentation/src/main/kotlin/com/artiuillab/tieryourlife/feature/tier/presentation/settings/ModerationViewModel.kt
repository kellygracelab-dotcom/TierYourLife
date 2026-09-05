package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityListUiState
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
    ) : ModerationUiState

    data object Failed : ModerationUiState
}

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

    /** One call: no moment in which the list is gone and nobody has answered for it. */
    fun takeDown(listId: String, ban: BanLength? = null) = settle(listId) { community.takeDown(listId, ban) }

    fun dismiss(listId: String) = settle(listId) { community.dismissReports(listId) }

    /** Both endings close every complaint about the list. Failing leaves them all: a queue that quietly loses entries is worse than one that will not budge. */
    private fun settle(listId: String, act: suspend () -> Result<Unit>) {
        val shown = _state.value as? ModerationUiState.Ready ?: return
        if (shown.settling != null) return

        _state.value = shown.copy(settling = listId)
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
