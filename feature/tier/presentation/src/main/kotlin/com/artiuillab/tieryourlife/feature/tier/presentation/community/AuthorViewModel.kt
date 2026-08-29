package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : AuthorUiState

    data object Failed : AuthorUiState
}

@HiltViewModel
class AuthorViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Author>()

    private val _state = MutableStateFlow<AuthorUiState>(AuthorUiState.Loading)
    val state: StateFlow<AuthorUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = AuthorUiState.Loading
            _state.value = community.feed(author = route.authorUid).fold(
                onSuccess = { all ->
                    // A list hidden from the feed stays hidden here; the
                    // profile is another way to the same shelf.
                    val hidden = preferences.hiddenListIds()
                    val lists = all.filterNot { it.id in hidden }
                    AuthorUiState.Ready(
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
            community.report(publishedId, reason, note)
                .onFailure { Timber.w(it, "Could not file the report") }
        }
    }
}
