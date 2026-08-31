package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.ordering.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val POOL_TIER_ID = -1L

@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val tiers: TierRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val publishedId = savedStateHandle.toRoute<Route.CommunityList>().publishedId

    private val _state = MutableStateFlow<CommunityListUiState>(CommunityListUiState.Loading)
    val state: StateFlow<CommunityListUiState> = _state.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        load()
    }

    private fun loadFollowState(authorUid: String) {
        if (authorUid.isEmpty()) return
        viewModelScope.launch {
            val state = community.followState(authorUid)
                .onFailure { Timber.i(it, "Not showing whether this author is followed") }
                .getOrNull() ?: return@launch
            _state.update { current ->
                if (current is CommunityListUiState.Success) current.copy(follow = state) else current
            }
        }
    }

    /**
     * Follows or stops, showing the answer before the server gives it and
     * putting it back if the server refuses. Nothing else on the screen
     * depends on it, so there is nothing else to undo.
     */
    fun toggleFollow() {
        val current = _state.value as? CommunityListUiState.Success ?: return
        val was = current.follow ?: return
        val now = was.copy(
            following = !was.following,
            followers = (was.followers + if (was.following) -1 else 1).coerceAtLeast(0),
        )
        _state.update { (it as CommunityListUiState.Success).copy(follow = now) }

        viewModelScope.launch {
            val result = if (now.following) {
                community.follow(current.authorUid)
            } else {
                community.unfollow(current.authorUid)
            }
            result.onFailure { error ->
                Timber.w(error, "Following an author failed")
                _state.update { state ->
                    if (state is CommunityListUiState.Success) state.copy(follow = was) else state
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = CommunityListUiState.Loading
            _state.value = community.open(publishedId).fold(
                onSuccess = { published ->
                    CommunityListUiState.Success(
                        list = asTheAuthorLeftIt(published),
                        mine = emptied(published),
                        // What somebody came for: the list as its author
                        // ranked it. Handing over a pile of cards instead
                        // answers a question they did not ask.
                        showing = if (published.arrangement.isEmpty()) Showing.Mine else Showing.Theirs,
                        knowsTheirs = published.arrangement.isNotEmpty(),
                        authorName = published.summary.authorName,
                        authorUid = published.summary.authorUid,
                        authorPhotoUrl = published.summary.authorPhotoUrl,
                    )
                },
                onFailure = { CommunityListUiState.Error },
            )
            (_state.value as? CommunityListUiState.Success)?.let { loadFollowState(it.authorUid) }
        }
    }

    /**
     * Switches between the author's arrangement and your own.
     *
     * Two boards kept side by side rather than one rebuilt: somebody who has
     * spent ten minutes ranking a list and glances at the author's should find
     * their own work where they left it.
     */
    fun show(which: Showing) {
        _state.update { current ->
            if (current !is CommunityListUiState.Success) current else current.copy(showing = which)
        }
    }

    /**
     * Ranking happens entirely in memory. Nothing reaches the database until
     * the reader asks for a copy, so backing out costs them nothing they were
     * promised.
     */
    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        _state.update { current ->
            if (current !is CommunityListUiState.Success) return@update current
            current.copy(
                list = current.list.withItemMoved(itemId, toTierId, toPosition),
                arranged = true,
            )
        }
    }

    fun saveToMyLists(onSaved: (Long) -> Unit) {
        val current = _state.value as? CommunityListUiState.Success ?: return
        if (current.saving) return
        _state.update { (it as CommunityListUiState.Success).copy(saving = true) }

        viewModelScope.launch {
            var newId: Long? = null
            val saved = messages.guard("Saving a community list") {
                newId = tiers.createFromTemplate(
                    title = current.list.title,
                    authorName = current.authorName,
                    tiers = current.list.tiers.filterNot { it.isPool },
                    items = current.list.tiers.flatMap { it.items },
                )
            }
            _state.update { (it as CommunityListUiState.Success).copy(saving = false) }
            if (saved) {
                // What the popular ordering counts. Told after the board
                // exists, and its failure is not this person's problem: they
                // have their copy either way, and a number that missed one
                // take is not worth an error on a screen.
                community.noteTaken(publishedId)
                    .onFailure { Timber.i(it, "Not counting this list as taken") }
                newId?.let(onSaved)
            }
        }
    }

    fun hide() {
        val current = _state.value as? CommunityListUiState.Success ?: return
        preferences.hideList(publishedId, current.list.title)
    }

    fun hideAuthor(authorUid: String, name: String) {
        preferences.hideAuthor(authorUid, name)
    }

    /**
     * Same bargain as the feed: it goes off this reader's screen at once,
     * and whether it comes down for everyone is a person's decision.
     */
    fun report(reason: ReportReason, note: String?) {
        hide()
        viewModelScope.launch {
            community.report(publishedId, reason, note)
                .onFailure { Timber.w(it, "Could not file the report") }
        }
    }

    /**
     * The board as its author left it.
     *
     * A card whose tier the snapshot does not know goes to the pool, which is
     * also where the author's own unranked cards sit -- the two are the same
     * thing to look at, and inventing a tier for them would be worse.
     */
    private fun asTheAuthorLeftIt(published: PublishedList): TierList {
        val where = published.arrangement
        val ranked = published.tiers.mapIndexed { index, tier ->
            tier.copy(items = published.items.filterIndexed { at, _ -> where.getOrNull(at) == index })
        }
        val unranked = published.items.filterIndexed { at, _ -> where.getOrNull(at) == null }
        return TierList(
            id = 0,
            title = published.summary.title,
            tiers = ranked + pool(unranked),
            authorName = published.summary.authorName,
        )
    }

    /** The same board with every card back in the pool, for ranking it yourself. */
    private fun emptied(published: PublishedList) = TierList(
        id = 0,
        title = published.summary.title,
        tiers = published.tiers.map { it.copy(items = emptyList()) } + pool(published.items),
        authorName = published.summary.authorName,
    )

    private fun pool(items: List<com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem>) = Tier(
        id = POOL_TIER_ID,
        label = "Unranked",
        colorLight = "#DAD7E0",
        colorDark = "#46464F",
        items = items,
        isPool = true,
    )
}
