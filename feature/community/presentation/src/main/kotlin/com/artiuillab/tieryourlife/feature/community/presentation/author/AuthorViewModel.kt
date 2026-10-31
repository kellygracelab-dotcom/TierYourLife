package com.artiuillab.tieryourlife.feature.community.presentation.author

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.EditorialAuthors
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.OwnerMemory
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.authorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface AuthorUiState {
    data object Loading : AuthorUiState

    /** The name travels with the route, so the header is right even at zero lists. */
    data class Ready(
        val name: String,
        val photoUrl: String?,
        val lists: List<PublishedListSummary>,
        /**
         * Null until the server answers. Distinct from "not following": a
         * button that says Follow before we know would ask somebody to undo
         * something they never did.
         */
        val follow: FollowState? = null,
    ) : AuthorUiState

    data object Failed : AuthorUiState

    /**
     * A profile behind lists that stand without an author, or no uid at all.
     * Reached only by an old route; asking the server would get the whole
     * feed back as if it were one person's.
     */
    data object Unavailable : AuthorUiState
}

/** What the owner may do about this person's standing; null for everyone who is not the owner. */
enum class ModeratorAction { Make, Remove }

@HiltViewModel
class AuthorViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
    private val owner: OwnerMemory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val route = savedStateHandle.authorRoute()

    private val _state = MutableStateFlow<AuthorUiState>(AuthorUiState.Loading)
    val state: StateFlow<AuthorUiState> = _state.asStateFlow()

    private val unavailable = EditorialAuthors.isEditorial(route.authorUid)

    /** Offered only once the moderators list has answered the owner; before that nobody is known to be the owner. */
    val moderatorAction: StateFlow<ModeratorAction?> = owner.moderators
        .map { moderators ->
            when {
                moderators == null || unavailable -> null
                route.authorUid in moderators -> ModeratorAction.Remove
                else -> ModeratorAction.Make
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        if (unavailable) {
            _state.value = AuthorUiState.Unavailable
        } else {
            load()
            loadFollowState()
        }
    }

    private fun loadFollowState() {
        viewModelScope.launch {
            val state = community.followState(route.authorUid)
                .onFailure { Timber.i(it, "Not showing whether this author is followed") }
                .getOrNull() ?: return@launch
            _state.update { current ->
                if (current is AuthorUiState.Ready) current.copy(follow = state) else current
            }
        }
    }

    /** Answers before the server does, and goes back if the request fails. */
    fun toggleFollow() {
        val current = _state.value as? AuthorUiState.Ready ?: return
        val was = current.follow ?: return
        val now = FollowState(following = !was.following)
        _state.update { (it as AuthorUiState.Ready).copy(follow = now) }

        viewModelScope.launch {
            val result = if (now.following) {
                community.follow(route.authorUid)
            } else {
                community.unfollow(route.authorUid)
            }
            result.onFailure { error ->
                Timber.w(error, "Following an author failed")
                _state.update { current2 ->
                    if (current2 is AuthorUiState.Ready) current2.copy(follow = was) else current2
                }
            }
        }
    }

    fun load() {
        if (unavailable) return
        viewModelScope.launch {
            _state.value = AuthorUiState.Loading
            _state.value = community.feed(author = route.authorUid).fold(
                onSuccess = { page ->
                    // An author may publish twenty lists, which is one page.
                    val all = page.lists
                    // A list hidden from the feed stays hidden here; the
                    // profile is another way to the same shelf.
                    val hidden = preferences.hiddenListIds()
                    val lists = all.filterNot { it.id in hidden }
                    AuthorUiState.Ready(
                        follow = (_state.value as? AuthorUiState.Ready)?.follow,
                        // Their own lists carry a fresher name and face than the
                        // card the reader tapped, so prefer those when there are any.
                        name = all.firstOrNull()?.authorName ?: route.authorName,
                        photoUrl = all.firstOrNull()?.authorPhotoUrl ?: route.authorPhotoUrl,
                        lists = lists,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Loading an author's lists failed")
                    AuthorUiState.Failed
                },
            )
        }
    }

    fun toggleModerator() {
        val action = moderatorAction.value ?: return
        val shown = _state.value as? AuthorUiState.Ready
        viewModelScope.launch {
            val result = when (action) {
                ModeratorAction.Make -> community.appointModerator(
                    route.authorUid,
                    shown?.name ?: route.authorName,
                    shown?.photoUrl ?: route.authorPhotoUrl,
                ).onSuccess { owner.appointed(route.authorUid) }
                ModeratorAction.Remove -> community.removeModerator(route.authorUid)
                    .onSuccess { owner.removed(route.authorUid) }
            }
            result.onFailure { error ->
                Timber.w(error, "Changing who moderates failed")
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    fun hideList(publishedId: String, title: String) {
        preferences.hideList(publishedId, title)
        _state.update { current ->
            if (current !is AuthorUiState.Ready) return@update current
            current.copy(lists = current.lists.filterNot { it.id == publishedId })
        }
    }

    fun hideAuthor() {
        val shown = (_state.value as? AuthorUiState.Ready)?.name ?: route.authorName
        preferences.hideAuthor(route.authorUid, shown)
    }

    fun report(publishedId: String, title: String, reason: ReportReason, note: String?) {
        hideList(publishedId, title)
        viewModelScope.launch {
            community.report(publishedId, reason, note).onFailure {
                Timber.w(it, "Could not file the report")
                // Back on the page, so it can be reported again; hidden, it never could.
                preferences.unhideList(publishedId)
                load()
                messages.send(UserMessage.ActionFailed)
            }
        }
    }
}
