package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import com.artiuillab.tieryourlife.feature.play.domain.estimatedDuels
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val NOW = 1_800_000_000_000L
private const val DAY = 24 * 60 * 60 * 1000L
private const val KEY = "board:1"

class PlayViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val unranked = board(1L to emptyList(), 2L to emptyList(), 3L to emptyList(), POOL to (1L..6L).toList())

    @Test
    fun allMeansExactlyEveryCardAcrossEveryMode_andNeverRoundsOrClips() = runBlocking {
        for (kind in GameKind.entries) {
            for (count in listOf(65, 91, 128, 129)) {
                val whole = board(1L to emptyList(), POOL to (1L..count.toLong()).toList())
                val games = FakeSavedGames()
                val vm = viewModel(games, MovingTierRepository(whole), PlayRequest.ownBoard(whole))
                vm.open()
                assertEquals(count, picker(vm.state.value)!!.count)
                vm.picker.choose(kind)
                vm.picker.start()
                vm.duel.pick(true)
                eventually { games.games[KEY]?.ids?.size == count }
                assertEquals((1L..count.toLong()).toList(), games.games.getValue(KEY).ids)
            }
        }
    }

    @Test
    fun aSampleFromSingleS_ordersThoseSixteen_atTheTopOfS_andTheRestStayBehind() = runBlocking {
        val whole = board(1L to (1L..91L).toList(), 2L to emptyList(), POOL to emptyList())
        val games = FakeSavedGames()
        val vm = viewModel(games, MovingTierRepository(whole), PlayRequest.ownBoard(whole))
        vm.open()
        assertEquals(listOf(16, 32, 64), quickCounts(91))
        vm.selection.sample(16)
        val chosen = picker(vm.state.value)!!.selectedCards.map { it.item.id }
        assertEquals(16, chosen.toSet().size)
        vm.picker.choose(GameKind.Hill)
        vm.picker.start()
        vm.playToTheEnd()
        val arranged = vm.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        val s = arranged.board.tiers.first { it.id == 1L }.items.map { it.id }
        assertEquals(91, s.size)
        assertEquals(chosen.toSet(), s.take(16).toSet())
        assertTrue(arranged.board.tiers.first { it.id == 2L }.items.isEmpty())
    }

    @Test
    fun manualSelectionAndSamplingUseOnlyTheChosenGroups() = runBlocking {
        val whole = board(1L to (1L..35L).toList(), POOL to (36L..91L).toList())
        val vm = viewModel(boards = MovingTierRepository(whole), request = PlayRequest.ownBoard(whole))
        vm.open()
        vm.picker.toggle(POOL)
        vm.selection.sample(32)
        assertTrue(picker(vm.state.value)!!.selectedCards.all { it.tierId == 1L })
        vm.selection.selectCandidates(false)
        listOf(3L, 17L, 29L).forEach(vm.selection::toggleCard)
        assertEquals(listOf(3L, 17L, 29L), picker(vm.state.value)!!.selectedIds)
        assertTrue(picker(vm.state.value)!!.canPlay)
        vm.selection.toggleCard(36L)
        assertEquals(3, picker(vm.state.value)!!.count)
        vm.picker.toggleEverything()
        assertEquals(91, picker(vm.state.value)!!.count)
    }

    @Test
    fun thePicker_offersTheLastGamePlayedOnThisDevice() = runBlocking {
        val viewModel = viewModel(games = FakeSavedGames(last = GameKind.Hill))

        viewModel.open()

        val picker = viewModel.state.firstWithin { picker(it)?.kind == GameKind.Hill }.let(::picker)
        assertEquals(PickerPage.Games, picker?.page)
    }

    // Read while the board is on screen, so the sheet rises with them rather than changing as it rises.
    @Test
    fun thePicker_opensWithTheSavedGameAndTheLastKind_alreadyInIt() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..6L).toList(), answered = 2)
        val viewModel = viewModel(games = SlowSavedGames(FakeSavedGames(mapOf(KEY to saved), last = GameKind.Hill)))
        viewModel.state.firstWithin { KEY in it.saved }

        viewModel.open()

        val picker = picker(viewModel.state.value)!!
        assertEquals(GameKind.Hill, picker.kind)
        assertEquals(2, picker.resume?.played)
    }

    @Test
    fun aLargeBoardOpensWithEveryCardSelected() = runBlocking {
        val big = board(1L to (100L..159L).toList(), POOL to (1L..10L).toList())
        val viewModel = viewModel(boards = MovingTierRepository(big), request = PlayRequest.ownBoard(big))

        viewModel.open()

        val picker = picker(viewModel.state.value)!!
        assertEquals(PickerPage.Games, picker.page)
        assertFalse(picker.cardsFirst)
        assertEquals(setOf(1L, POOL), picker.chosen)
        assertEquals(70, picker.count)
        assertTrue(picker.canPlay)
    }

    @Test
    fun aWholeGame_endsOnTheBoard_withEveryCardPlaced_andIsNotKept() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games = games)
        viewModel.open()
        viewModel.picker.start()

        viewModel.playToTheEnd()

        assertEquals(PlayStage.Closed, viewModel.state.value.stage)
        val arranged = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        val placed = arranged.board.tiers.filterNot { it.isPool }
        assertEquals((1L..6L).toSet(), placed.flatMap { tier -> tier.items.map { it.id } }.toSet())
        assertEquals(listOf(1L, 2L, 3L), placed.map { it.id })
        eventually { KEY !in games.games }
    }

    @Test
    fun quitting_keepsTheGame_andSaysHowFarItGot() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games = games)
        viewModel.open()
        viewModel.picker.start()
        viewModel.duel.pick(first = true)
        viewModel.duel.skip()

        viewModel.duel.quit()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.GameSaved(2, estimatedDuels(GameKind.Swiss, 6)), said.notice)
        assertEquals(2, games.games.getValue(KEY).history.size)
        assertEquals(null, games.games.getValue(KEY).history[1].winner)
        assertTrue(KEY in viewModel.state.value.saved)
        assertEquals(PlayStage.Closed, viewModel.state.value.stage)
    }

    @Test
    fun quittingBeforeAnyPick_keepsNothing() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games = games)
        viewModel.open()
        viewModel.picker.start()

        viewModel.duel.quit()

        viewModel.events.firstWithin { it == PlayEvent.Closed }
        eventually { KEY !in games.games }
        assertFalse(KEY in viewModel.state.value.saved)
    }

    @Test
    fun aSavedGame_resumesWhereItWasLeft() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..6L).toList(), answered = 2)
        val viewModel = viewModel(games = FakeSavedGames(mapOf(KEY to saved)))
        viewModel.open()
        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        assertEquals(2, offer.played)

        viewModel.picker.resume()

        assertEquals(3, (viewModel.state.value.stage as PlayStage.Dueling).duel.number)
    }

    @Test
    fun aSwissGameKeepsItsParticipantsWhenNewCardsArrive() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..5L).toList(), answered = 2)
        val grown = board(1L to emptyList(), POOL to (1L..7L).toList())
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games, boards = MovingTierRepository(grown), request = PlayRequest.ownBoard(grown))
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        viewModel.picker.resume()

        assertEquals(0, offer.joined)
        assertFalse(offer.changed)
        assertEquals((1L..5L).toList(), games.games.getValue(KEY).ids)
    }

    @Test
    fun aHillWhoseCardsChanged_startsAgain_andSaysTheOldOneWent() = runBlocking {
        val saved = savedGame(GameKind.Hill, (1L..7L).toList(), answered = 2)
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games)
        viewModel.open()
        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        assertTrue(offer.changed)

        viewModel.picker.startAgain()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.SavedGameDiscarded, said.notice)
        val duel = (viewModel.state.value.stage as PlayStage.Dueling).duel
        assertEquals(GameKind.Hill, duel.kind)
        assertEquals(5, duel.total)
    }

    // A24: Start again over fewer than three cards did nothing, and nothing said why.
    @Test
    fun aHillWithTooFewCardsLeft_offersNoStartAgain() = runBlocking {
        val saved = savedGame(GameKind.Hill, (1L..4L).toList(), answered = 1).copy(scope = listOf(1L))
        val thinned = board(1L to listOf(1L, 2L), POOL to (5L..6L).toList())
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games, boards = MovingTierRepository(thinned), request = PlayRequest.ownBoard(thinned))
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        viewModel.picker.startAgain()

        assertTrue(offer.changed)
        assertFalse(offer.canStartAgain)
        assertTrue(viewModel.state.value.stage is PlayStage.Picking)
        assertEquals(saved, games.games[KEY])
    }

    // QA 484 N1: a game whose tier was removed blamed its cards, and nothing on the phone let it go.
    @Test
    fun aGameWhoseTierIsGone_saysSo_andCanBeLetGoAndBroughtBack() = runBlocking {
        val saved = savedGame(GameKind.Hill, (1L..4L).toList(), answered = 1).copy(scope = listOf(9L), keepTier = true)
        val left = board(1L to listOf(1L, 2L), POOL to (3L..6L).toList())
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games, boards = MovingTierRepository(left), request = PlayRequest.ownBoard(left))
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        assertTrue(offer.tierGone)
        assertFalse(offer.canStartAgain)

        // Pressed on the main thread, as a phone presses it: from here it raced the picker's second
        // read of the saved game, which put the offer back over the discard (red main CI on 76d52d82).
        withContext(Dispatchers.Main) { viewModel.notices.discardStored() }
        // QA 490: the board's "Undo" was drawn under the sheet; the sheet offers it itself.
        assertTrue(picker(viewModel.state.value)!!.discarded)
        assertNull(picker(viewModel.state.value)?.resume)
        viewModel.state.firstWithin { KEY !in it.saved }
        assertNull(games.games[KEY])

        withContext(Dispatchers.Main) { viewModel.notices.undoDiscard() }
        val back = viewModel.state.firstWithin { picker(it)?.resume != null }.let(::picker)!!
        assertFalse(back.discarded)
        assertEquals(saved.ids, games.games.getValue(KEY).ids)
    }

    @Test
    fun aGameDiscardedInThePicker_isOfferedBackOnTheBoardOnceTheSheetCloses() = runBlocking {
        val saved = savedGame(GameKind.Hill, (1L..4L).toList(), answered = 1).copy(scope = listOf(9L), keepTier = true)
        val left = board(1L to listOf(1L, 2L), POOL to (3L..6L).toList())
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games, boards = MovingTierRepository(left), request = PlayRequest.ownBoard(left))
        viewModel.open()
        viewModel.state.firstWithin { picker(it)?.resume != null }
        viewModel.notices.discardStored()

        viewModel.picker.dismiss()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.SavedGameDiscarded, said.notice)
        viewModel.notices.undoDiscard()
        viewModel.state.firstWithin { KEY in it.saved }
        assertEquals(saved.ids, games.games.getValue(KEY).ids)
    }

    @Test
    fun aMonthOldGame_goesWithoutAWord() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..6L).toList(), answered = 2).copy(startedAt = NOW - 40 * DAY, touchedAt = NOW - 31 * DAY)
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games)

        viewModel.open()

        eventually { KEY !in games.games }
        assertNull(picker(viewModel.state.value)?.resume)
    }

    @Test
    fun aGameStartedLongAgo_butPlayedYesterday_isStillOffered() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..6L).toList(), answered = 2).copy(startedAt = NOW - 40 * DAY, touchedAt = NOW - DAY)
        val viewModel = viewModel(games = FakeSavedGames(mapOf(KEY to saved)))

        viewModel.open()

        assertEquals(2, viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!!.played })
    }

    @Test
    fun eachAnswer_isWhatTheMonthCountsFrom() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games = games)
        viewModel.open()
        viewModel.picker.start()
        viewModel.clock = { NOW + DAY }

        viewModel.duel.pick(first = true)

        eventually { games.games[KEY]?.touchedAt == NOW + DAY }
        assertEquals(NOW, games.games.getValue(KEY).startedAt)
    }

    @Test
    fun startingAnotherGame_discardsTheSavedOne_andUndoGoesBackToIt() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..6L).toList(), answered = 2)
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games)
        viewModel.open()
        viewModel.state.firstWithin { picker(it)?.resume != null }
        viewModel.picker.choose(GameKind.Bracket)

        viewModel.picker.start()
        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.SavedGameDiscarded, said.notice)
        assertEquals(GameKind.Bracket, games.last)

        viewModel.notices.undoDiscard()

        val duel = (viewModel.state.value.stage as PlayStage.Dueling).duel
        assertEquals(GameKind.Swiss, duel.kind)
        assertEquals(3, duel.number)
    }

    // SPEC 14.11 п.1: the result is one step, Undo and Redo go between the board before the game and after it.
    @Test
    fun theLastAnswer_putsTheResultOnTheBoard_andUndoAndRedoToggleIt() = runBlocking {
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(boards = repository)
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()

        val arranged = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        val said = (viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said).notice as PlayNotice.BoardArranged
        assertTrue(said.duels > 0)
        val after = arranged.board.ids()
        assertEquals(after, repository.board.ids())
        assertEquals(POOL to emptyList<Long>(), after.last())

        viewModel.notices.undoArrange()
        val undone = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        assertTrue(undone.undone)
        assertEquals(PlayNotice.GameUndone(said.duels), (viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said).notice)
        assertEquals(unranked.ids(), repository.board.ids())

        viewModel.notices.redoArrange()
        val redone = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        assertFalse(redone.undone)
        assertEquals(said, (viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said).notice)
        assertEquals(after, repository.board.ids())

        viewModel.notices.undoArrange()
        viewModel.events.firstWithin { it is PlayEvent.Arranged }
        assertEquals(unranked.ids(), repository.board.ids())
    }

    @Test
    fun everyDuelSkipped_movesNoCard_andSaysSo() = runBlocking {
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(boards = repository)
        viewModel.open()
        viewModel.picker.start()
        while (viewModel.state.value.stage is PlayStage.Dueling) viewModel.duel.skip()

        assertEquals(PlayNotice.NothingAnswered, (viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said).notice)
        assertEquals(PlayStage.Closed, viewModel.state.value.stage)
        assertTrue(repository.moves.isEmpty())
        assertEquals(unranked.ids(), repository.board.ids())
    }

    @Test
    fun oneTierKept_isOnlyReorderedInside_andNothingElseMoves() = runBlocking {
        val ranked = board(1L to listOf(1L, 2L, 3L, 4L), 2L to listOf(5L), POOL to listOf(6L))
        val repository = MovingTierRepository(ranked)
        val viewModel = viewModel(boards = repository, request = PlayRequest.ownBoard(ranked))
        viewModel.open()
        viewModel.picker.toggle(2L)
        viewModel.picker.toggle(POOL)
        assertTrue(picker(viewModel.state.value)!!.keepsTier)
        viewModel.picker.start()

        viewModel.playToTheEnd(first = false)

        viewModel.events.firstWithin { it is PlayEvent.Arranged }
        val order = repository.board.tiers.first { it.id == 1L }.items.map { it.id }
        assertEquals(listOf(1L to order, 2L to listOf(5L), POOL to listOf(6L)), repository.board.ids())
        assertEquals((1L..4L).toSet(), order.toSet())
    }

    @Test
    fun onSomeoneElsesList_theResultFillsMine_andNothingIsWritten() = runBlocking {
        val theirs = board(1L to listOf(1L, 2L, 3L), 2L to listOf(4L, 5L, 6L), POOL to emptyList(), id = 0)
        val mine = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList(), id = 0)
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(boards = repository, request = PlayRequest.someoneElses("abc", version = 1, theirs, mine))
        viewModel.open()
        assertTrue(picker(viewModel.state.value)!!.intoMine)
        viewModel.picker.start()
        viewModel.playToTheEnd()

        val arranged = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        assertTrue(arranged.board.tiers.first { it.isPool }.items.isEmpty())
        assertEquals(6, arranged.board.tiers.sumOf { it.items.size })
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun undoingAResultPutInMine_saysItIsAnUndo_andHandsBackMineAsItWas() = runBlocking {
        val theirs = board(1L to listOf(1L, 2L, 3L), 2L to listOf(4L, 5L, 6L), POOL to emptyList(), id = 0)
        val mine = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList(), id = 0)
        val viewModel = viewModel(request = PlayRequest.someoneElses("abc", version = 1, theirs, mine))
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()
        val applied = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged

        viewModel.notices.undoArrange()

        val undone = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        assertFalse(applied.undone)
        assertTrue(undone.undone)
        assertEquals(mine, undone.board)

        viewModel.notices.redoArrange()

        val redone = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        assertFalse(redone.undone)
        assertEquals(applied.board, redone.board)
    }

    @Test
    fun undoingAPick_takesTheLastAnswerBack() = runBlocking {
        val viewModel = viewModel()
        viewModel.open()
        viewModel.picker.start()
        val first = (viewModel.state.value.stage as PlayStage.Dueling).duel
        viewModel.duel.pick(first = false)

        viewModel.duel.undo()

        assertEquals(first, (viewModel.state.value.stage as PlayStage.Dueling).duel)
    }

    private fun viewModel(
        games: SavedGames = FakeSavedGames(),
        boards: MovingTierRepository = MovingTierRepository(unranked),
        request: PlayRequest = PlayRequest.ownBoard(unranked),
    ) = PlayViewModel(games, boards).apply {
        clock = { NOW }
        seeds = { 7 }
        offer(request)
    }

    /** A store that takes a moment to answer, as the file does on a phone. */
    private class SlowSavedGames(private val games: SavedGames) : SavedGames by games {
        override suspend fun load(key: String): SavedGame? {
            delay(50)
            return games.load(key)
        }

        override suspend fun lastKind(): GameKind? {
            delay(50)
            return games.lastKind()
        }
    }

    private fun savedGame(kind: GameKind, ids: List<Long>, answered: Int) = SavedGame(
        spec = GameSpec(kind, ids.size, seed = 3),
        ids = ids,
        history = List(answered) { Outcome(2 * it, 2 * it + 1, winner = 2 * it) },
        round = 1,
        startedAt = NOW - DAY,
    )

    private fun picker(state: PlayState): Picker? = (state.stage as? PlayStage.Picking)?.picker

    private fun PlayViewModel.playToTheEnd(first: Boolean = true) {
        while (state.value.stage is PlayStage.Dueling) duel.pick(first)
    }

    /** For what the view model writes from its own coroutines, which no state says it has done. */
    private suspend fun eventually(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }
}
