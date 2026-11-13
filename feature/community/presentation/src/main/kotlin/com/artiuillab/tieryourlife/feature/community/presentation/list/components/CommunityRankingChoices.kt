package com.artiuillab.tieryourlife.feature.community.presentation.list.components

import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.community.domain.model.rankingOffer
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListUiState
import com.artiuillab.tieryourlife.feature.community.presentation.list.ListNotice
import com.artiuillab.tieryourlife.feature.community.presentation.list.Showing
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Records the offer separately from public visibility. Closing it never publishes anything. */
internal class CommunityRankingChoices(
    private val state: MutableStateFlow<CommunityListUiState>,
    private val progress: PrivateRankingProgress,
    private val scope: CoroutineScope,
    private val messages: UserMessages,
    private val tell: suspend (ListNotice) -> Unit,
    private val showRanking: () -> Unit,
) {
    fun offerIfNeeded() {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.own || current.arriving) return
        if (!current.arranged || current.showing != Showing.Mine) return
        val offer = rankingOffer(current.mine, progress.choice) ?: return
        progress.rememberChoice(RankingChoice.Offered)
        state.value = current.copy(rankingOffer = offer)
    }

    fun dismiss() {
        state.update { if (it is CommunityListUiState.Success) it.copy(rankingOffer = null) else it }
    }

    fun recordOffer(languageTag: String) {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.rankingOffer == null || current.saving || current.savedId != null) return
        state.value = current.copy(saving = true)
        val owner = progress.ownerKey
        scope.launch {
            var id: Long? = null
            messages.guard("Keeping the ranking offer across devices") {
                id = progress.saveCopy(Showing.Mine, languageTag, RankingChoice.Offered)
            }
            if (owner == progress.ownerKey) state.update { now ->
                if (now is CommunityListUiState.Success) now.copy(saving = false, savedId = id ?: now.savedId) else now
            }
        }
    }

    fun choose(public: Boolean, languageTag: String) {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.saving || current.arriving) return
        state.value = current.copy(saving = true)
        val owner = progress.ownerKey
        scope.launch {
            var id: Long? = null
            val saved = messages.guard("Saving the private ranking choice") {
                id = progress.saveCopy(Showing.Mine, languageTag, if (public && current.signedIn) RankingChoice.Ranking else RankingChoice.Private)
            }
            if (owner != progress.ownerKey) return@launch
            state.update { now ->
                if (now is CommunityListUiState.Success) {
                    now.copy(saving = false, savedId = id ?: now.savedId, rankingOffer = if (saved) null else now.rankingOffer)
                } else {
                    now
                }
            }
            if (saved && public) {
                if (current.signedIn) {
                    showRanking()
                } else {
                    // Existing sign-in route, with no deferred automatic public action.
                    tell(ListNotice.SignInToFollow)
                }
            }
        }
    }

    /** Explicit menu action remembers intent too; Withdraw does not erase that memory. */
    fun showMine(languageTag: String) = choose(public = true, languageTag)
}
