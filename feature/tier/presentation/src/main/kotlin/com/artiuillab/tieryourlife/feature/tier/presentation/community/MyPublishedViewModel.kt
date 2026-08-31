package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
        /**
         * The ones whose board has been edited since. Only these offer to be
         * brought up to date -- for the rest there is nothing to send.
         */
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

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val behind = runCatching { tiers.publishedCopiesLeftBehind() }
                .onFailure { Timber.w(it, "Comparing published copies failed") }
                .getOrDefault(emptySet())
            _state.value = community.myPublished().fold(
                onSuccess = { MyPublishedUiState.Ready(lists = it, behind = behind) },
                onFailure = { error ->
                    Timber.w(error, "Reading your published lists failed")
                    MyPublishedUiState.Failed
                },
            )
        }
    }

    /**
     * Taking one down also clears the id from the local copy if this phone
     * still holds one. Leaving it behind would let the settings screen offer
     * to unpublish something that is no longer there.
     */
    fun takeDown(publishedId: String) {
        val shown = _state.value as? MyPublishedUiState.Ready ?: return
        if (shown.removing != null) return

        _state.value = shown.copy(removing = publishedId)
        viewModelScope.launch {
            community.unpublish(publishedId).fold(
                onSuccess = {
                    forgetLocally(publishedId)
                    _state.value = MyPublishedUiState.Ready(shown.lists.filterNot { it.id == publishedId })
                },
                onFailure = { error ->
                    Timber.w(error, "Taking down $publishedId failed")
                    _state.value = shown.copy(removing = null)
                },
            )
        }
    }

    /**
     * Sends the board again, over the copy already in the feed.
     *
     * The same id, so the same link: anybody who has it keeps it, and the
     * people who saw the list yesterday find the same list today. Taking it
     * down and publishing again would work and would break every link given
     * out, which is the one thing publishing is for.
     */
    fun update(publishedId: String) {
        val shown = _state.value as? MyPublishedUiState.Ready ?: return
        if (shown.updating != null || shown.removing != null) return

        _state.value = shown.copy(updating = publishedId)
        viewModelScope.launch {
            val board = runCatching { tiers.boardPublishedAs(publishedId) }.getOrNull()
            if (board == null) {
                // Published from a phone this one no longer is. There is
                // nothing here to send, and saying so beats a button that
                // fails silently.
                _state.value = shown.copy(updating = null)
                return@launch
            }
            community.publish(board).fold(
                onSuccess = { published ->
                    runCatching { tiers.setPublished(board.id, published.id, published.fingerprint) }
                    _state.value = shown.copy(updating = null, behind = shown.behind - publishedId)
                },
                onFailure = { error ->
                    Timber.w(error, "Updating $publishedId failed")
                    _state.value = shown.copy(updating = null)
                },
            )
        }
    }

    private suspend fun forgetLocally(publishedId: String) {
        val mine = runCatching { tiers.getAllTierLists() }.getOrNull().orEmpty()
        mine.firstOrNull { it.publishedId == publishedId }
            ?.let { runCatching { tiers.setPublished(it.id, null, null) } }
    }
}
