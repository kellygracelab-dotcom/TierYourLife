package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.MIN_PLAY_CARDS
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.Resumption
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import com.artiuillab.tieryourlife.feature.play.domain.nextPair
import com.artiuillab.tieryourlife.feature.play.domain.progress
import com.artiuillab.tieryourlife.feature.play.domain.resumeOn
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.lists.itemLayout
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * Play over one screen's board: the picker, the game and its result, and the
 * game put down between sittings. Scoped to the screen beneath, so it lives
 * exactly as long as the board it plays.
 */
@HiltViewModel
class PlayViewModel @Inject constructor(
    private val games: SavedGames,
    private val boards: TierRepository,
    private val boardSync: BoardSync = BoardSync.None,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state.asStateFlow()

    private val _events = Channel<PlayEvent>(Channel.BUFFERED)
    val events: Flow<PlayEvent> = _events.receiveAsFlow()

    /** Read once per game, so a test can stop the clock and fix the shuffle. */
    internal var clock: () -> Long = System::currentTimeMillis
    internal var seeds: () -> Int = { Random.nextInt() }

    private var request: PlayRequest? = null

    /** The game put down for [request]'s board, as last read. */
    private var stored: SavedGame? = null
    private var lastKind: GameKind? = null
    private var session: Session? = null

    /** What the last notice's action would act on. */
    private var quitted: Session? = null
    private var discarded: Session? = null
    private var arranged: Arrangement? = null

    /** A game being let go from the picker: "Undo" waits for it, and nothing re-reads it meanwhile. */
    private var letting: Job? = null

    private val shelf = GameShelf(games, boards) { clock() }

    private data class Arrangement(val before: TierList, val own: Boolean)

    val picker = PickerControls()
    val selection = SelectionControls()
    val duel = DuelControls()
    val result = ResultControls()
    val notices = NoticeControls()

    private var stage: PlayStage
        get() = _state.value.stage
        set(value) = _state.update { it.copy(stage = value) }

    /**
     * The board on screen now; the screen hands it in each time it changes, and
     * what the picker opens with is read then, so it rises as it will stay.
     */
    fun offer(request: PlayRequest) {
        val before = this.request
        this.request = request
        val finished = stage as? PlayStage.Finished
        val current = session
        if (finished != null && current != null && current.key == request.key) {
            val participants = current.game.ids.toSet()
            val count = unplayedIn(request.applyTo ?: request.board, current.game, participants)
            stage = PlayStage.Finished(finished.result.copy(unplayedCount = count))
        }
        val news = before?.key != request.key || before.board.play != request.board.play
        if (news && session == null) {
            viewModelScope.launch {
                if (lastKind == null) {
                    val remembered = games.lastKind()
                    if (lastKind == null) lastKind = remembered
                }
                reread(request)
            }
        }
    }

    /** Reads whether a game of [key] is put down, for the dot on the button. */
    fun watch(key: String) {
        viewModelScope.launch {
            val current = request
            if (current?.key == key) taken(current) else markSaved(key, shelf.peek(key) != null)
        }
    }

    fun open() {
        val request = request ?: return
        if (stage != PlayStage.Closed) return
        val groups = groupsOf(request.board)
        val shownKind = lastKind ?: GameKind.Swiss
        stage = PlayStage.Picking(
            Picker(
                title = request.board.title,
                groups = groups,
                chosen = groups.map { it.tierId }.toSet(),
                cards = cardsOf(request.board),
                kind = shownKind,
                resume = stored?.let { offerFor(it, request.board) },
                intoMine = request.intoMine,
            ),
        )
        viewModelScope.launch {
            offerResume(request, shownKind)
            // A game saved on the website since the board was opened reaches the picker as it stands open.
            if (!request.intoMine && runSuspendCatching { boardSync.sync() }.isSuccess) reread(request)
        }
    }

    inner class PickerControls {
        fun choose(kind: GameKind) = updatePicker { it.copy(kind = kind) }

        fun showCards() = updatePicker { it.copy(page = PickerPage.Cards) }

        fun showGames() = updatePicker { it.copy(page = PickerPage.Games, cardsFirst = false) }

        fun showIndividual() = updatePicker { it.copy(page = PickerPage.Individual) }

        fun toggle(tierId: Long) = updatePicker {
            it.copy(chosen = if (tierId in it.chosen) it.chosen - tierId else it.chosen + tierId, selectedIds = null)
        }

        fun toggleEverything() = updatePicker {
            it.copy(chosen = if (it.everything) emptySet() else it.groups.map { group -> group.tierId }.toSet(), selectedIds = null)
        }

        fun toggleRanked() = updatePicker { it.copy(chosen = if (it.ranked) emptySet() else it.rankedIds, selectedIds = null) }

        /** A game discarded here and not brought back is offered once more on the board, beyond the sheet. */
        fun dismiss() {
            val picker = (stage as? PlayStage.Picking)?.picker ?: return
            close()
            if (picker.discarded && discarded != null) viewModelScope.launch { say(PlayNotice.SavedGameDiscarded) }
        }

        fun start() {
            val picker = (stage as? PlayStage.Picking)?.picker ?: return
            val request = request ?: return
            if (!picker.canPlay) return
            val allGroups = picker.groups.all { it.tierId in picker.chosen }
            val scope = if (allGroups && !picker.keepsTier) null else picker.groups.map { it.tierId }.filter { it in picker.chosen }
            begin(request, picker.kind, scope, picker.keepsTier, picker.selectedCards.map { it.item.id })
        }

        fun resume() {
            val request = request ?: return
            val saved = stored ?: return
            val ready = saved.resumeOn(request.board.cardsIn(null).map { it.id }, request.board.tierRowIds()) as? Resumption.Ready ?: return
            val next = Session(request.key, request.board, ready.game, own = !request.intoMine)
            // Nothing answered is left: what is put down stays as it is until something is.
            if (ready.game == saved || ready.game.history.isEmpty()) show(next) else record(next)
        }

        /** A hill or bracket whose cards changed: the same game over the cards there are now. */
        fun startAgain() {
            val request = request ?: return
            val saved = stored ?: return
            if (!offerFor(saved, request.board).canStartAgain) return
            begin(request, saved.spec.kind, saved.scope, saved.keepTier, saved.ids)
        }
    }

    inner class SelectionControls {
        fun sample(count: Int) = updatePicker { picker ->
            val ids = picker.candidates.map { it.item.id }
            if (count !in quickCounts(ids.size)) picker else picker.copy(selectedIds = ids.shuffled().take(count))
        }

        fun toggleCard(id: Long) = updatePicker { picker ->
            val ids = picker.selectedCards.map { it.item.id }
            if (picker.candidates.none { it.item.id == id }) picker
            else picker.copy(selectedIds = if (id in ids) ids - id else ids + id)
        }

        fun selectCandidates(all: Boolean) = updatePicker { it.copy(selectedIds = if (all) null else emptyList()) }
    }

    inner class DuelControls {
        fun pick(first: Boolean) = answer { a, b -> if (first) a else b }

        fun skip() = answer { _, _ -> null }

        fun undo() {
            val current = session ?: return
            if (current.game.history.isEmpty()) return
            record(current.withHistory(current.game.history.dropLast(1)))
        }

        /** Close, Back or the system's back: no question asked, the game is kept and the board says so. */
        fun quit() {
            val current = session ?: return
            session = null
            close()
            viewModelScope.launch {
                if (outdated(current)) return@launch
                when (persist(current)) {
                    Shelved.Kept -> {
                        stored = current.game
                        quitted = current
                        val played = progress(current.game.spec, current.game.history).played
                        say(PlayNotice.GameSaved(played, totalOf(current.game)))
                    }
                    Shelved.Lost -> say(PlayNotice.NotSaved)
                    Shelved.NothingToKeep -> Unit
                }
            }
        }

        private fun answer(winner: (Int, Int) -> Int?) {
            val current = session ?: return
            val (first, second) = nextPair(current.game.spec, current.game.history) ?: return
            record(current.withHistory(current.game.history + Outcome(first, second, winner(first, second))))
        }
    }

    inner class ResultControls {
        fun unplayed(policy: UnplayedPolicy) {
            val shown = (stage as? PlayStage.Finished)?.result ?: return
            stage = PlayStage.Finished(shown.copy(unplayedPolicy = policy))
        }

        fun apply() {
            val current = session ?: return
            val request = request ?: return
            val shown = (stage as? PlayStage.Finished)?.result?.takeIf { it.canApply } ?: return
            val placement = shown.tiers.associate { tier -> tier.tierId to tier.cards.map { it.item.id } }
            session = null
            close()
            viewModelScope.launch {
                if (outdated(current)) return@launch
                val into = request.applyTo
                val area = current.game.scope
                val after = if (into != null) intoMine(into, placement, shown.unplayedPolicy, area) else ontoBoard(request.board, placement, shown.unplayedPolicy, area)
                if (after == null) {
                    // Still put down, finished: Resume brings the result back to apply again.
                    stored = current.game
                    return@launch
                }
                forget(current)
                _events.send(PlayEvent.Arranged(after))
                say(PlayNotice.BoardArranged(shown.duels))
            }
        }

        /** Close calls on the same screen, once per game. */
        fun refine() {
            val current = session ?: return
            if (current.game.spec.refine) return
            record(current.copy(game = current.game.copy(spec = current.game.spec.copy(refine = true))))
        }

        /** Back to the picker over the same cards; the finished game stays put down until another starts. */
        fun playAgain() {
            val current = session ?: return
            val request = request ?: return
            session = null
            if (current.key != request.key) {
                stored = null
                stage = PlayStage.Closed
                open()
                viewModelScope.launch { outdated(current) }
                return
            }
            stored = current.game.takeIf { it.history.isNotEmpty() }
            val groups = groupsOf(request.board)
            val scope = current.game.scope
            stage = PlayStage.Picking(
                Picker(
                    title = request.board.title,
                    groups = groups,
                    chosen = scope?.toSet() ?: groups.map { it.tierId }.toSet(),
                    kind = current.game.spec.kind,
                    resume = stored?.let { offerFor(it, request.board) },
                    intoMine = request.intoMine,
                    cards = cardsOf(request.board),
                    selectedIds = current.game.ids,
                ),
            )
        }

        fun cancel() {
            val current = session ?: return
            session = null
            close()
            viewModelScope.launch {
                forget(current)
                discarded = current
                say(PlayNotice.ResultDiscarded)
            }
        }
    }

    inner class NoticeControls {
        /** From "Game saved": the game is not wanted after all. */
        fun discardSaved() {
            val gone = quitted ?: return
            quitted = null
            viewModelScope.launch { forget(gone) }
        }

        /**
         * A game nothing can pick up again, let go here as on the website. The
         * sheet stands over the board's notices, so its row says so and offers
         * "Undo" itself.
         */
        fun discardStored() {
            val request = request ?: return
            val saved = stored ?: return
            stored = null
            updatePicker { it.copy(resume = null, discarded = true) }
            val gone = Session(request.key, request.board, saved, own = !request.intoMine)
            discarded = gone
            letting = viewModelScope.launch { forget(gone) }
        }

        /** The game that was discarded comes back; one started in its place gives way to it. */
        fun undoDiscard() {
            val back = discarded ?: return
            discarded = null
            val current = session
            val request = request
            if (current != null && current.key == back.key && request != null) {
                val ready = back.game.resumeOn(request.board.cardsIn(null).map { it.id }, request.board.tierRowIds()) as? Resumption.Ready
                if (ready != null) {
                    record(back.copy(board = request.board, game = ready.game))
                    return
                }
            }
            viewModelScope.launch {
                letting?.join()
                persist(back)
                if (request?.key == back.key) {
                    stored = back.game
                    updatePicker { it.copy(resume = offerFor(back.game, request.board), discarded = false) }
                }
            }
        }

        fun undoArrange() {
            val undo = arranged ?: return
            arranged = null
            viewModelScope.launch {
                if (!undo.own) {
                    _events.send(PlayEvent.Arranged(undo.before, undone = true))
                    return@launch
                }
                val done = restore(undo.before)
                if (done == null) say(PlayNotice.Failed)
                _events.send(PlayEvent.Arranged(done ?: undo.before, undone = true))
            }
        }
    }

    private fun begin(request: PlayRequest, kind: GameKind, scope: List<Long>?, keepTier: Boolean, ids: List<Long>) {
        val available = request.board.cardsIn(null).associateBy { it.id }
        val cards = ids.distinct().mapNotNull { available[it] }
        if (cards.size < MIN_PLAY_CARDS) return
        val spec = GameSpec(kind, cards.size, seeds())
        val game = SavedGame(
            spec = spec,
            ids = cards.map { it.id },
            history = emptyList(),
            round = progress(spec, emptyList()).round,
            startedAt = clock(),
            scope = scope,
            keepTier = keepTier,
        )
        val replaced = stored
        stored = null
        lastKind = kind
        show(Session(request.key, request.board, game, own = !request.intoMine))
        viewModelScope.launch {
            games.rememberKind(kind)
            if (replaced != null) {
                val gone = Session(request.key, request.board, replaced, own = !request.intoMine)
                forget(gone)
                discarded = gone
                say(PlayNotice.SavedGameDiscarded)
            }
        }
    }

    private fun intoMine(mine: TierList, placement: Map<Long, List<Long>>, unplayed: UnplayedPolicy, area: List<Long>?): TierList {
        arranged = Arrangement(mine, own = false)
        return mine.arrangedBy(placement, unplayed, area)
    }

    /**
     * One compare-and-apply transaction on the current board. A concurrent edit
     * or a failed commit leaves that board intact and the result available to retry.
     */
    private suspend fun ontoBoard(board: TierList, placement: Map<Long, List<Long>>, unplayed: UnplayedPolicy, area: List<Long>?): TierList? {
        val before = runSuspendCatching { boards.getTierListById(board.id) }.getOrNull()
        val done = before?.let {
            runSuspendCatching {
                val after = before.arrangedBy(placement, unplayed, area)
                if (!boards.arrangeBoard(before.id, before.itemLayout(), after.itemLayout())) return@runSuspendCatching null
                after
            }.onFailure { Timber.w(it, "Arranging a board from a game failed") }.getOrNull()
        }
        if (before != null && done != null) {
            arranged = Arrangement(before, own = true)
            return done
        }
        val back = runSuspendCatching { boards.getTierListById(board.id) }.getOrNull() ?: board
        say(PlayNotice.Failed)
        _events.send(PlayEvent.Arranged(back))
        return null
    }

    /** Every card back where [before] had it, on the board as it is now; null when that failed. */
    private suspend fun restore(before: TierList): TierList? = runSuspendCatching {
        val now = boards.getTierListById(before.id) ?: return null
        val target = now.restoredTo(before)
        if (boards.arrangeBoard(now.id, now.itemLayout(), target.itemLayout())) target else null
    }.onFailure { Timber.w(it, "Putting the board back failed") }.getOrNull()

    private fun show(next: Session) {
        session = next
        stage = stageOf(next, request?.intoMine == true)
    }

    private fun record(next: Session) {
        show(next)
        viewModelScope.launch { persist(next) }
    }

    private suspend fun persist(current: Session): Shelved =
        shelf.put(current).also { shelved -> markSaved(current.key, shelved == Shelved.Kept) }

    /**
     * Someone else's list published again while [ended] was played: its
     * answers are about the old version's cards, which the new one numbers
     * afresh, so it is neither kept nor applied. Returns whether that was so.
     */
    private suspend fun outdated(ended: Session): Boolean {
        val request = request ?: return false
        if (ended.key == request.key) return false
        forget(ended)
        if (ended.game.history.isNotEmpty()) say(PlayNotice.ListUpdated)
        stored = taken(request)
        return true
    }

    private suspend fun forget(gone: Session) {
        shelf.clear(gone)
        markSaved(gone.key, false)
        if (request?.key == gone.key) stored = null
    }

    /** The device's last game takes the picker's choice, unless somebody has already chosen while it was read. */
    private suspend fun offerResume(request: PlayRequest, shownKind: GameKind) {
        val remembered = games.lastKind()?.also { lastKind = it }
        val saved = taken(request)
        stored = saved
        updatePicker { picker ->
            picker.copy(
                resume = saved?.let { offerFor(it, request.board) },
                kind = if (remembered != null && picker.kind == shownKind) remembered else picker.kind,
            )
        }
    }

    /** The game put down for [request]'s board read afresh, and offered so by a picker that stands open. */
    private suspend fun reread(request: PlayRequest) {
        if (letting?.isActive == true) return
        val saved = taken(request)
        val current = this.request
        if (current?.key != request.key || session != null || letting?.isActive == true) return
        stored = saved
        updatePicker { picker ->
            if (picker.discarded && saved == null) picker else picker.copy(resume = saved?.let { offerFor(it, current.board) }, discarded = false)
        }
    }

    private suspend fun taken(request: PlayRequest): SavedGame? =
        shelf.take(request).also { game -> markSaved(request.key, game != null) }

    private fun markSaved(key: String, saved: Boolean) =
        _state.update { it.copy(saved = if (saved) it.saved + key else it.saved - key) }

    private fun updatePicker(change: (Picker) -> Picker) = _state.update { state ->
        val picking = state.stage as? PlayStage.Picking ?: return@update state
        state.copy(stage = PlayStage.Picking(change(picking.picker)))
    }

    private fun close() {
        stage = PlayStage.Closed
        viewModelScope.launch { _events.send(PlayEvent.Closed) }
    }

    private suspend fun say(notice: PlayNotice) = _events.send(PlayEvent.Said(notice))
}

private fun Session.withHistory(history: List<Outcome>) = copy(game = game.copy(history = history))
