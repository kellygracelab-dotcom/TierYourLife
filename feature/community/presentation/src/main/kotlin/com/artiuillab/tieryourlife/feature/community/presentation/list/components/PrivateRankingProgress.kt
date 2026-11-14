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
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
        val copy = if (continuing == null && !fresh.own) savedFor(record?.savedBoardUid, answer) else null
        val kept = copy?.draftOn(answer, fresh.mine)?.on(answer, fresh.mine) ?: record?.on(answer, fresh.mine)
            ?: copy?.placedOn(answer)?.on(answer, fresh.mine)
        if (account != currentOwner()) return previous ?: fresh
        owner = account
        if (continuing == null) {
            savedCopy = copy
            choice = copy?.rankingChoice ?: record?.choice
        }
        val shown = kept?.let { fresh.copy(mine = it, showing = Showing.Mine, arranged = true) } ?: fresh
        return continuing ?: shown.copy(savedId = copy?.id)
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
        val copy = original?.let { savedFor(record?.savedBoardUid, it) }
        val kept = original?.let { copy?.draftOn(it, empty(it)) } ?: record ?: original?.let { copy?.placedOn(it) }
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

    /**
     * A copy that came after the page did, from the account's boards (SPEC
     * list-flow Н2): Mine takes its places while nothing is arranged here.
     */
    suspend fun copyMayHaveArrived() {
        val answer = source ?: return
        val account = owner
        if (!waitingForMine(account) || drafts.read(listId, account) != null) return
        val copy = savedFor(null, answer) ?: return
        val mine = (copy.draftOn(answer, empty(answer)) ?: copy.placedOn(answer))?.on(answer, empty(answer)) ?: return
        if (account != currentOwner()) return
        savedCopy = copy
        choice = choice ?: copy.rankingChoice
        state.update { now ->
            val still = now as? CommunityListUiState.Success
            if (still != null && still.unarranged() && still.version == answer.summary.updatedAtMillis) still.copy(mine = mine, arranged = true, savedId = copy.id) else now
        }
    }

    private fun waitingForMine(account: String?): Boolean =
        ready && account == currentOwner() && (state.value as? CommunityListUiState.Success)?.let { !it.own && it.unarranged() } == true

    private fun CommunityListUiState.Success.unarranged() = !arranged && !arriving

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

    /**
     * One list, one copy: what the reader sees becomes theirs, [which] half of
     * it, and saving again changes that copy rather than adding another. A
     * copy of an older version of the list is left as it is. It takes the
     * list's cover from the community; a list without one leaves the cards to
     * make the collage. Never published by saving or dismissing the offer.
     */
    suspend fun saveCopy(which: Showing, languageTag: String, value: RankingChoice? = choice): Long = writes.withLock {
        val account = owner
        val current = state.value as CommunityListUiState.Success
        val original = requireNotNull(source)
        check(ready && isCurrentOwner && !current.arriving)
        val mine = if (which == Showing.Theirs) current.theirs else current.mine
        val known = savedCopy ?: savedFor(null, original)
        if (known != null && !known.isCopyOf(original, empty(original))) {
            savedCopy = known
            return@withLock known.id
        }
        val id = if (known != null) {
            val now = tiers.getTierListById(known.id) ?: known
            tiers.arrangeBoard(now.id, now.itemLayout(), now.layoutOf(original, mine))
            known.id
        } else {
            tiers.createRankingCopy(keeping(mine, original, current, languageTag), listId, original.summary.updatedAtMillis, value)
        }
        check(account == currentOwner())
        if (value != null) tiers.setRankingChoice(id, value)
        savedCopy = tiers.getTierListById(id)
        choice = value
        state.value = (state.value as CommunityListUiState.Success).copy(mine = mine, showing = Showing.Mine, arranged = true)
        drafts.keep(listId, draftOf(original, mine).copy(choice = choice, savedBoardUid = savedCopy?.uid), account)
        id
    }

    private fun keeping(board: TierList, original: PublishedList, current: CommunityListUiState.Success, languageTag: String): TierList {
        val indices = original.items.withIndex().associate { it.value.id to it.index }
        val translated = board.readIn(current.translations.wordsIn(languageTag))
        return translated.copy(
            category = current.section,
            coverImageUrl = original.summary.coverImageUrl,
            coverImageUrls = emptyList(),
            tiers = translated.tiers.map { tier -> tier.copy(items = tier.items.map { it.copy(sourceIndex = indices[it.id]) }) },
        )
    }

    private suspend fun updateCopy(original: PublishedList, mine: TierList, account: String?, remembered: RankingChoice?) {
        val known = savedCopy ?: return
        if (account != currentOwner()) return
        val actual = tiers.getTierListByUid(known.uid)
        if (actual == null) {
            savedCopy = null
            val current = state.value as? CommunityListUiState.Success
            if (current?.savedId == known.id) state.value = current.copy(savedId = null)
            return
        }
        if (!actual.isCopyOf(original, empty(original))) return
        val target = actual.layoutOf(original, mine)
        if (remembered != null) tiers.setRankingChoice(actual.id, remembered)
        if (tiers.arrangeBoard(actual.id, known.itemLayout(), target)) {
            savedCopy = tiers.getTierListById(actual.id)
        } else {
            savedCopy = null
        }
    }

    /**
     * This reader's copy of [source], whichever version of it and however it
     * was saved, here or on another device: the one the draft names, else the
     * last edited. A copy saved without a choice went unrecognised before, and
     * the next Save made a second one (Danylo's Eurovision twice in My lists).
     */
    private suspend fun savedFor(uid: String?, source: PublishedList): TierList? {
        val copies = tiers.getAllTierLists().filter { it.takenFrom == source.summary.id }
        return copies.firstOrNull { it.uid == uid } ?: copies.maxByOrNull { it.editedAt ?: 0 }
    }

    /**
     * Where a copy that is no longer exactly the list puts the list's cards
     * (SPEC list-flow Н2): renamed tiers, added cards or an older version of
     * the list. A card counts where it is still the list's card at that
     * place; the rest of the list waits in the pool. Null when the tiers no
     * longer match or none of its cards is placed.
     */
    private fun TierList.placedOn(source: PublishedList): RankingDraft? {
        val ranked = tiers.filterNot { it.isPool }
        val sameVersion = originVersion == source.summary.updatedAtMillis
        val seen = mutableSetOf<Int>()
        fun TierItem.at(): Int? = sourceIndex?.takeIf { it in source.items.indices && (sameVersion || source.items[it].title == title) && seen.add(it) }
        val rows = ranked.map { tier -> tier.items.mapNotNull { it.at() } }
        if (ranked.size != source.tiers.size || seen.isEmpty()) return null
        return RankingDraft(
            version = source.summary.updatedAtMillis,
            rows = rows,
            pool = source.items.indices.filterNot { it in seen },
            choice = rankingChoice,
            savedBoardUid = uid,
        )
    }

    /** Where the copy's cards stand, as a draft of this version; null for a copy of another version, which is not mapped over. */
    private fun TierList.draftOn(source: PublishedList, empty: TierList): RankingDraft? = takeIf { it.isCopyOf(source, empty) }?.sourceDraft(source)

    private fun currentOwner() = (accounts.current() as? Account.SignedIn)?.recordKey
}
