package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.estimatedDuels
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    fun thePicker_offersTheLastGamePlayedOnThisDevice() = runBlocking {
        val viewModel = viewModel(games = FakeSavedGames(last = GameKind.Hill))

        viewModel.open()

        val picker = viewModel.state.firstWithin { picker(it)?.kind == GameKind.Hill }.let(::picker)
        assertEquals(PickerPage.Games, picker?.page)
    }

    @Test
    fun aBoardTooBigToPlayWhole_opensOnChoosingCards_withThePoolChosenWhenItFits() = runBlocking {
        val big = board(1L to (100L..159L).toList(), POOL to (1L..10L).toList())
        val viewModel = viewModel(boards = MovingTierRepository(big), request = PlayRequest.ownBoard(big))

        viewModel.open()

        val picker = picker(viewModel.state.value)!!
        assertEquals(PickerPage.Cards, picker.page)
        assertTrue(picker.cardsFirst)
        assertEquals(setOf(POOL), picker.chosen)
        assertTrue(picker.canPlay)
    }

    @Test
    fun aWholeGame_endsInAResult_thatPlacesEveryCard() = runBlocking {
        val viewModel = viewModel()
        viewModel.open()
        viewModel.picker.start()

        viewModel.playToTheEnd()

        val result = (viewModel.state.value.stage as PlayStage.Finished).result
        assertEquals((1L..6L).toSet(), result.tiers.flatMap { tier -> tier.cards.map { it.item.id } }.toSet())
        assertEquals(listOf(1L, 2L, 3L), result.tiers.map { it.tierId })
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
    fun aSwissGame_takesInCardsAddedSince() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..5L).toList(), answered = 2)
        val grown = board(1L to emptyList(), POOL to (1L..7L).toList())
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games, boards = MovingTierRepository(grown), request = PlayRequest.ownBoard(grown))
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        viewModel.picker.resume()

        assertEquals(2, offer.joined)
        assertFalse(offer.changed)
        eventually { games.games[KEY]?.ids == (1L..7L).toList() }
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

    @Test
    fun aMonthOldGame_goesWithoutAWord() = runBlocking {
        val saved = savedGame(GameKind.Swiss, (1L..6L).toList(), answered = 2).copy(startedAt = NOW - 31 * DAY)
        val games = FakeSavedGames(mapOf(KEY to saved))
        val viewModel = viewModel(games = games)

        viewModel.open()

        eventually { KEY !in games.games }
        assertNull(picker(viewModel.state.value)?.resume)
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

    @Test
    fun applyingOnOwnBoard_movesThePlayedCardsThroughTheRepository_andUndoPutsThemBack() = runBlocking {
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(boards = repository)
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()
        val result = (viewModel.state.value.stage as PlayStage.Finished).result

        viewModel.result.apply()

        val arranged = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.BoardArranged(result.duels), said.notice)
        val expected = result.tiers.map { tier -> tier.tierId to tier.cards.map { it.item.id } } + (POOL to emptyList())
        assertEquals(expected, repository.board.ids())
        assertEquals(expected, arranged.board.ids())

        viewModel.notices.undoArrange()

        viewModel.events.firstWithin { it is PlayEvent.Arranged }
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
        val order = (viewModel.state.value.stage as PlayStage.Finished).result.tiers.single().cards.map { it.item.id }
        viewModel.result.apply()

        viewModel.events.firstWithin { it is PlayEvent.Arranged }
        assertEquals(listOf(1L to order, 2L to listOf(5L), POOL to listOf(6L)), repository.board.ids())
        assertEquals((1L..4L).toSet(), order.toSet())
    }

    @Test
    fun onSomeoneElsesList_theResultFillsMine_andNothingIsWritten() = runBlocking {
        val theirs = board(1L to listOf(1L, 2L, 3L), 2L to listOf(4L, 5L, 6L), POOL to emptyList(), id = 0)
        val mine = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList(), id = 0)
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(boards = repository, request = PlayRequest.someoneElses("abc", theirs, mine))
        viewModel.open()
        assertTrue(picker(viewModel.state.value)!!.intoMine)
        viewModel.picker.start()
        viewModel.playToTheEnd()
        assertTrue((viewModel.state.value.stage as PlayStage.Finished).result.intoMine)

        viewModel.result.apply()

        val arranged = viewModel.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged
        assertTrue(arranged.board.tiers.first { it.isPool }.items.isEmpty())
        assertEquals(6, arranged.board.tiers.sumOf { it.items.size })
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun cancellingAResult_discardsIt_andUndoKeepsItAgain() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games = games)
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()

        viewModel.result.cancel()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.ResultDiscarded, said.notice)
        eventually { KEY !in games.games }
        viewModel.notices.undoDiscard()
        eventually { KEY in games.games }
    }

    @Test
    fun refine_asksTheCloseCallsOnce() = runBlocking {
        val viewModel = viewModel()
        viewModel.open()
        viewModel.picker.choose(GameKind.Hill)
        viewModel.picker.start()
        viewModel.playToTheEnd()
        val offered = (viewModel.state.value.stage as PlayStage.Finished).result
        assertTrue(offered.canRefine)

        viewModel.result.refine()

        val duel = (viewModel.state.value.stage as PlayStage.Dueling).duel
        assertTrue(duel.refining)
        assertEquals(1, duel.number)
        assertEquals(offered.refineDuels, duel.total)
        viewModel.playToTheEnd()
        assertFalse((viewModel.state.value.stage as PlayStage.Finished).result.canRefine)
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

    @Test
    fun playingAgain_reopensThePickerOverTheSameCards() = runBlocking {
        val ranked = board(1L to listOf(1L, 2L, 3L), 2L to listOf(4L), POOL to listOf(5L))
        val viewModel = viewModel(boards = MovingTierRepository(ranked), request = PlayRequest.ownBoard(ranked))
        viewModel.open()
        viewModel.picker.toggle(2L)
        viewModel.picker.start()
        viewModel.playToTheEnd()

        viewModel.result.playAgain()

        val picker = picker(viewModel.state.value)!!
        assertEquals(setOf(1L, POOL), picker.chosen)
        assertEquals(4, picker.count)
        assertTrue(picker.resume != null)
    }

    private fun viewModel(
        games: FakeSavedGames = FakeSavedGames(),
        boards: MovingTierRepository = MovingTierRepository(unranked),
        request: PlayRequest = PlayRequest.ownBoard(unranked),
    ) = PlayViewModel(games, boards).apply {
        clock = { NOW }
        seeds = { 7 }
        offer(request)
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
