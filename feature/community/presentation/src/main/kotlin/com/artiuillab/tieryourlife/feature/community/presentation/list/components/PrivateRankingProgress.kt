package com.artiuillab.tieryourlife.feature.community.presentation.list.components

import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.readIn
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListUiState
import com.artiuillab.tieryourlife.feature.community.presentation.list.Showing
import com.artiuillab.tieryourlife.feature.community.presentation.list.draftOf
import com.artiuillab.tieryourlife.feature.community.presentation.list.on
import com.artiuillab.tieryourlife.feature.tier.domain.lists.itemLayout
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Private progress and its account boundary, separate from the public Rankings controller. */
internal class PrivateRankingProgress(
    private val listId: String,
    private val state: MutableStateFlow<CommunityListUiState>,
    private val accounts: AccountRepository,
    private val drafts: RankingDrafts,
    private val scope: CoroutineScope,
    private val messages: UserMessages,
    private val empty: (PublishedList) -> TierList,
    private val invalidateUndo: () -> Unit,
    private val tiers: TierRepository,
) {
    var source: PublishedList? = null
        private set
    private var owner = currentOwner()
    private var ready = true
    private var savedCopy: TierList? = null
    private val writes = Mutex()
    var choice: RankingChoice? = null
        private set

    val isCurrentOwner: Boolean get() = owner == currentOwner()
    val ownerKey: String? get() = owner

    suspend fun opened(
        answer: PublishedList,
        fresh: CommunityListUiState.Success,
        previous: CommunityListUiState.Success?,
        continuing: CommunityListUiState.Success?,
    ): CommunityListUiState.Success {
        val account = currentOwner()
        source = answer
        val record = if (continuing == null && !fresh.own) drafts.read(listId, account) else null
        val copy = if (continuing == null && !fresh.own) savedFor(record?.savedBoardUid, answer, fresh.mine) else null
        val kept = copy?.sourceDraft(answer)?.on(answer, fresh.mine) ?: record?.on(answer, fresh.mine)
        if (account != currentOwner()) return previous ?: fresh
        owner = account
        if (continuing == null) {
            savedCopy = copy
            choice = copy?.rankingChoice ?: record?.choice
        }
        return continuing ?: kept?.let { fresh.copy(mine = it, showing = Showing.Mine, arranged = true, savedId = copy?.id) } ?: fresh
    }

    suspend fun ownerChanged(account: String?) {
        if (account == owner && ready) return
        keep()
        ready = false
        invalidateUndo()
        val original = source
        val current = state.value as? CommunityListUiState.Success
        val signingIn = owner == null && account != null
        val guestProgress = current?.mine?.takeIf { signingIn && current.arranged && !current.own }
        if (current != null) state.value = current.copy(arriving = true)
        val record = drafts.read(listId, account)
        val copy = original?.let { savedFor(record?.savedBoardUid, it, empty(it)) }
        val kept = copy?.sourceDraft(requireNotNull(original)) ?: record
        if (currentOwner() != account) return
        owner = account
        ready = true
        savedCopy = copy
        choice = kept?.choice
        restoreOwner(original, kept, guestProgress, copy)
    }

    private fun restoreOwner(original: PublishedList?, kept: RankingDraft?, guestProgress: TierList?, copy: TierList?) {
        val pair = original?.let { answer ->
            (state.value as? CommunityListUiState.Success)?.takeIf { it.version == answer.summary.updatedAtMillis }?.let { answer to it }
        }
        if (pair != null) {
            val (answer, now) = pair
            val emptyBoard = empty(answer)
            val mine = kept?.on(answer, emptyBoard) ?: guestProgress.takeIf { kept == null }
            state.value = now.copy(
                mine = mine ?: emptyBoard,
                showing = Showing.Mine,
                arranged = mine != null,
                arriving = false,
                chosenRanking = null,
                rankings = emptyList(),
                rankingsLoading = false,
                moreRankings = false,
                savedId = copy?.id,
                rankingOffer = null,
            )
            if (mine === guestProgress && guestProgress != null) keep()
        }
    }

    fun keep() {
        if (!ready) return
        val current = state.value as? CommunityListUiState.Success ?: return
        val original = source ?: return
        if (current.own || current.arriving || current.version != original.summary.updatedAtMillis) return
        val account = owner
        val draft = draftOf(original, current.mine).copy(choice = choice, savedBoardUid = savedCopy?.uid)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            messages.guard("Keeping private ranking progress") {
                withContext(NonCancellable) {
                    // Reserve the shared file queue immediately, before a reopened screen can read it.
                    drafts.keep(listId, draft, account)
                    writes.withLock {
                        updateCopy(original, current.mine, account, draft.choice)
                    }
                }
            }
        }
    }

    fun rememberChoice(value: RankingChoice) {
        if (value != RankingChoice.Offered || choice == null) choice = value
        keep()
    }

    suspend fun publicVisibility(shown: Boolean) {
        rememberChoice(RankingChoice.Ranking)
        savedCopy?.takeIf { isCurrentOwner }?.let { tiers.setRankingOf(it.id, listId.takeIf { shown }) }
    }

    /** A private library copy is never published by saving or dismissing the offer. */
    suspend fun saveCopy(which: Showing, languageTag: String, value: RankingChoice? = choice): Long = writes.withLock {
        val account = owner
        val current = state.value as CommunityListUiState.Success
        val original = requireNotNull(source)
        check(ready && isCurrentOwner && !current.arriving)
        val mine = if (which == Showing.Theirs) current.theirs else current.mine
        val indices = original.items.withIndex().associate { it.value.id to it.index }
        val translated = mine.readIn(current.translations.wordsIn(languageTag))
        val keeping = translated.copy(
            category = current.section,
            tiers = translated.tiers.map { tier -> tier.copy(items = tier.items.map { it.copy(sourceIndex = indices[it.id]) }) },
        )
        val known = savedCopy?.takeIf { which == Showing.Mine && it.isCopyOf(original, empty(original)) }
        val copyChoice = value.takeIf { which == Showing.Mine }
        val id = known?.id ?: tiers.createRankingCopy(keeping, listId, original.summary.updatedAtMillis, copyChoice)
        check(account == currentOwner())
        if (which == Showing.Mine) {
            if (value != null) tiers.setRankingChoice(id, value)
            savedCopy = tiers.getTierListById(id)
            choice = value
            drafts.keep(listId, draftOf(original, current.mine).copy(choice = choice, savedBoardUid = savedCopy?.uid), account)
        }
        id
    }

    private suspend fun updateCopy(original: PublishedList, mine: TierList, account: String?, remembered: RankingChoice?) {
        val known = savedCopy ?: return
        if (account != currentOwner()) return
        val actual = tiers.getTierListByUid(known.uid)
        if (actual == null || !actual.isCopyOf(original, empty(original))) {
            savedCopy = null
            val current = state.value as? CommunityListUiState.Success
            if (current?.savedId == known.id) state.value = current.copy(savedId = null)
            return
        }
        val target = actual.layoutOf(original, mine)
        if (remembered != null) tiers.setRankingChoice(actual.id, remembered)
        if (tiers.arrangeBoard(actual.id, known.itemLayout(), target)) {
            savedCopy = tiers.getTierListById(actual.id)
        } else {
            savedCopy = null
        }
    }

    private suspend fun savedFor(uid: String?, source: PublishedList, empty: TierList): TierList? {
        val exact = uid?.let { tiers.getTierListByUid(it) }?.takeIf { it.isCopyOf(source, empty) }
        return exact ?: tiers.getAllTierLists().filter { it.rankingChoice != null && it.isCopyOf(source, empty) }.maxByOrNull { it.editedAt ?: 0 }
    }

    private fun currentOwner() = (accounts.current() as? Account.SignedIn)?.recordKey
}
