package com.artiuillab.tieryourlife.feature.community.presentation.published

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface MyPublishedUiState {
    data object Loading : MyPublishedUiState

    data class Ready(
        val lists: List<PublishedListSummary>,
        val removing: String? = null,
        /** Every copy this phone still has the board for. */
        val canUpdate: Set<String> = emptySet(),
        /** The ones we can prove have been left behind. */
        val behind: Set<String> = emptySet(),
        val updating: String? = null,
    ) : MyPublishedUiState

    data object Failed : MyPublishedUiState
}

@HiltViewModel
class MyPublishedViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val tiers: TierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MyPublishedUiState>(MyPublishedUiState.Loading)
    val state: StateFlow<MyPublishedUiState> = _state.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val standing = runCatching { tiers.publishedStanding() }
                .onFailure { Timber.w(it, "Comparing published copies failed") }
                .getOrDefault(PublishedStanding())
            _state.value = community.myPublished().fold(
                onSuccess = {
                    MyPublishedUiState.Ready(
                        lists = it,
                        canUpdate = standing.canUpdate,
                        behind = standing.knownBehind,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Reading your published lists failed")
                    MyPublishedUiState.Failed
                },
            )
        }
    }

    /** Also clears the id from the local copy, or the settings screen would offer to unpublish something no longer there. */
    fun takeDown(publishedId: String) {
        val shown = _state.value as? MyPublishedUiState.Ready ?: return
        if (shown.removing != null) return

        _state.value = shown.copy(removing = publishedId)
        viewModelScope.launch {
            community.unpublish(publishedId).fold(
                onSuccess = {
                    forgetLocally(publishedId)
                    // A copy, not a fresh Ready: the other rows keep their Update buttons.
                    _state.value = shown.copy(
                        lists = shown.lists.filterNot { it.id == publishedId },
                        removing = null,
                        canUpdate = shown.canUpdate - publishedId,
                        behind = shown.behind - publishedId,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Taking down $publishedId failed")
                    _state.value = shown.copy(removing = null)
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }

    /** Over the same id, so the same link: taking it down and publishing again would break every link given out. */
    fun update(publishedId: String) {
        val shown = _state.value as? MyPublishedUiState.Ready ?: return
        if (shown.updating != null || shown.removing != null) return

        _state.value = shown.copy(updating = publishedId)
        viewModelScope.launch {
            val board = runCatching { tiers.boardPublishedAs(publishedId) }
                .onFailure { Timber.w(it, "Finding the board behind $publishedId failed") }
                .getOrNull()
            if (board == null) {
                // Published from a phone this one no longer is: nothing here to send.
                _state.value = shown.copy(updating = null)
                return@launch
            }
            community.publish(board).fold(
                onSuccess = { published ->
                    runCatching { tiers.setPublished(board.id, published.id, published.fingerprint) }
                        .onFailure { Timber.w(it, "Recording the update of $publishedId failed") }
                    // Sent, so it is neither behind nor unknown any more.
                    _state.value = shown.copy(updating = null, behind = shown.behind - publishedId)
                },
                onFailure = { error ->
                    Timber.w(error, "Updating $publishedId failed")
                    _state.value = shown.copy(updating = null)
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }

    private suspend fun forgetLocally(publishedId: String) {
        val mine = runCatching { tiers.getAllTierLists() }
            .onFailure { Timber.w(it, "Reading boards to forget $publishedId failed") }
            .getOrNull().orEmpty()
        mine.firstOrNull { it.publishedId == publishedId }
            ?.let { board ->
                runCatching { tiers.setPublished(board.id, null, null) }
                    .onFailure { Timber.w(it, "Forgetting $publishedId on this phone failed") }
            }
    }
}
