package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.MAX_PLAY_CARDS
import com.artiuillab.tieryourlife.feature.play.domain.MIN_PLAY_CARDS
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.Resumption
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import com.artiuillab.tieryourlife.feature.play.domain.nextPair
import com.artiuillab.tieryourlife.feature.play.domain.progress
import com.artiuillab.tieryourlife.feature.play.domain.resumeOn
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val shelf = GameShelf(games, boards) { clock() }

    private data class Arrangement(val before: TierList, val own: Boolean)

    val picker = PickerControls()
    val duel = DuelControls()
    val result = ResultControls()
    val notices = NoticeControls()

    private var stage: PlayStage
        get() = _state.value.stage
        set(value) = _state.update { it.copy(stage = value) }

    /** The board on screen now; the screen hands it in each time it changes, and a game that came with it is read. */
    fun offer(request: PlayRequest) {
        val before = this.request
        this.request = request
        val news = before?.key != request.key || before.board.play != request.board.play
        if (news && session == null) viewModelScope.launch { stored = taken(request) }
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
        val tooMany = groups.sumOf { it.count } > MAX_PLAY_CARDS
        val chosen = if (tooMany) {
            // Nothing is chosen for somebody unless the pool alone is a game.
            groups.filter { it.isPool && it.count in MIN_PLAY_CARDS..MAX_PLAY_CARDS }
        } else {
            groups
        }
        val shownKind = lastKind ?: GameKind.Swiss
        stage = PlayStage.Picking(
            Picker(
                title = request.board.title,
                groups = groups,
                chosen = chosen.map { it.tierId }.toSet(),
                kind = shownKind,
                page = if (tooMany) PickerPage.Cards else PickerPage.Games,
                cardsFirst = tooMany,
                intoMine = request.intoMine,
            ),
        )
        viewModelScope.launch { offerResume(request, shownKind) }
    }

    inner class PickerControls {
        fun choose(kind: GameKind) = updatePicker { it.copy(kind = kind) }

        fun showCards() = updatePicker { it.copy(page = PickerPage.Cards) }

        fun showGames() = updatePicker { it.copy(page = PickerPage.Games, cardsFirst = false) }

        fun toggle(tierId: Long) = updatePicker {
            it.copy(chosen = if (tierId in it.chosen) it.chosen - tierId else it.chosen + tierId)
        }

        fun toggleEverything() = updatePicker {
            it.copy(chosen = if (it.everything) emptySet() else it.groups.map { group -> group.tierId }.toSet())
        }

        fun keepInTier(keep: Boolean) = updatePicker { it.copy(keepTier = keep) }

        fun dismiss() {
            if (stage !is PlayStage.Picking) return
            close()
        }

        fun start() {
            val picker = (stage as? PlayStage.Picking)?.picker ?: return
            val request = request ?: return
            if (!picker.canPlay) return
            val scope = if (picker.everything) null else picker.groups.map { it.tierId }.filter { it in picker.chosen }
            begin(request, picker.kind, scope, picker.keepsTier)
        }

        fun resume() {
            val request = request ?: return
            val saved = stored ?: return
            val ready = saved.resumeOn(request.board.cardsIn(saved.scope).map { it.id }) as? Resumption.Ready ?: return
            val next = Session(request.key, request.board, ready.game, own = !request.intoMine)
            if (ready.game == saved) show(next) else record(next)
        }

        /** A hill or bracket whose cards changed: the same game over the cards there are now. */
        fun startAgain() {
            val request = request ?: return
            val saved = stored ?: return
            begin(request, saved.spec.kind, saved.scope, saved.keepTier)
        }
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
                if (persist(current)) {
                    stored = current.game
                    quitted = current
                    val played = progress(current.game.spec, current.game.history).played
                    say(PlayNotice.GameSaved(played, totalOf(current.game)))
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
        fun apply() {
            val current = session ?: return
            val request = request ?: return
            val shown = (stage as? PlayStage.Finished)?.result?.takeIf { it.canApply } ?: return
            val placement = shown.tiers.associate { tier -> tier.tierId to tier.cards.map { it.item.id } }
            session = null
            close()
            viewModelScope.launch {
                val into = request.applyTo
                val after = if (into != null) intoMine(into, placement) else ontoBoard(request.board, placement)
                forget(current)
                if (after != null) {
                    _events.send(PlayEvent.Arranged(after))
                    say(PlayNotice.BoardArranged(shown.duels))
                }
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
            stored = current.game.takeIf { it.history.isNotEmpty() }
            val groups = groupsOf(request.board)
            val scope = current.game.scope
            stage = PlayStage.Picking(
                Picker(
                    title = request.board.title,
                    groups = groups,
                    chosen = scope?.toSet() ?: groups.map { it.tierId }.toSet(),
                    keepTier = current.game.keepTier || scope?.size != 1,
                    kind = current.game.spec.kind,
                    resume = stored?.let { offerFor(it, request.board) },
                    intoMine = request.intoMine,
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

        /** The game that was discarded comes back; one started in its place gives way to it. */
        fun undoDiscard() {
            val back = discarded ?: return
            discarded = null
            val current = session
            val request = request
            if (current != null && current.key == back.key && request != null) {
                val ready = back.game.resumeOn(request.board.cardsIn(back.game.scope).map { it.id }) as? Resumption.Ready
                if (ready != null) {
                    record(back.copy(board = request.board, game = ready.game))
                    return
                }
            }
            viewModelScope.launch {
                persist(back)
                if (request?.key == back.key) stored = back.game
            }
        }

        fun undoArrange() {
            val undo = arranged ?: return
            arranged = null
            viewModelScope.launch {
                if (!undo.own) {
                    _events.send(PlayEvent.Arranged(undo.before))
                    return@launch
                }
                val done = runSuspendCatching {
                    val now = boards.getTierListById(undo.before.id) ?: return@launch
                    val target = now.restoredTo(undo.before)
                    movesBetween(now, target).forEach { boards.moveItem(it.itemId, it.tierId, it.position) }
                    target
                }.onFailure { Timber.w(it, "Undoing an arrangement failed") }
                if (done.isFailure) say(PlayNotice.Failed)
                _events.send(PlayEvent.Arranged(done.getOrDefault(undo.before)))
            }
        }
    }

    private fun begin(request: PlayRequest, kind: GameKind, scope: List<Long>?, keepTier: Boolean) {
        val cards = request.board.cardsIn(scope).take(MAX_PLAY_CARDS)
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

    private fun intoMine(mine: TierList, placement: Map<Long, List<Long>>): TierList {
        arranged = Arrangement(mine, own = false)
        return mine.arrangedBy(placement)
    }

    /** Through the board's own moves, one card at a time, on the board as it is now rather than as it was. */
    private suspend fun ontoBoard(board: TierList, placement: Map<Long, List<Long>>): TierList? {
        val done = runSuspendCatching {
            val before = boards.getTierListById(board.id) ?: return null
            val after = before.arrangedBy(placement)
            movesBetween(before, after).forEach { boards.moveItem(it.itemId, it.tierId, it.position) }
            arranged = Arrangement(before, own = true)
            after
        }.onFailure { Timber.w(it, "Arranging a board from a game failed") }
        if (done.isFailure) {
            say(PlayNotice.Failed)
            // Some moves may have landed: the board reads itself again either way.
            _events.send(PlayEvent.Arranged(board))
        }
        return done.getOrNull()
    }

    private fun show(next: Session) {
        session = next
        stage = stageOf(next, request?.intoMine == true)
    }

    private fun record(next: Session) {
        show(next)
        viewModelScope.launch { persist(next) }
    }

    /** Returns whether a game is kept: one with nothing answered is not. */
    private suspend fun persist(current: Session): Boolean =
        shelf.put(current).also { kept -> markSaved(current.key, kept) }

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
