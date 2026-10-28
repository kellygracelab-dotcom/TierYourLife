package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SharedGame
import com.artiuillab.tieryourlife.feature.play.domain.SharedGameJson
import com.artiuillab.tieryourlife.feature.play.domain.nextPair
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val NOW = 1_800_000_000_000L
private const val DAY = 24 * 60 * 60 * 1000L
private const val KEY = "board:1"

/** A game on your own board follows the account through the board's `play`; one on someone else's list stays here. */
class BoardGameSyncTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val unranked = board(1L to emptyList(), 2L to emptyList(), 3L to emptyList(), POOL to (1L..6L).toList())
    private val spec = GameSpec(GameKind.Swiss, 6, seed = 3)

    @Test
    fun aPickOnYourOwnBoard_putsTheGameOnTheBoard_inTheSharedForm() = runBlocking {
        val games = FakeSavedGames()
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository)
        viewModel.open()
        viewModel.picker.start()
        val first = (viewModel.state.value.stage as PlayStage.Dueling).duel

        viewModel.duel.pick(first = true)

        eventually { repository.board.play != null && games.games[KEY]?.onBoard == true }
        val shared = SharedGameJson.decode(repository.board.play!!)!!
        assertEquals(games.games.getValue(KEY).ids.map { "c$it" }, shared.ids)
        val answer = shared.history.single()
        assertEquals("c${first.first.id}", shared.ids[answer.first])
        assertEquals(answer.first, answer.winner)
        assertEquals(NOW, shared.startedAt)
        assertNull(shared.scope)
        assertEquals(1, shared.round)
    }

    @Test
    fun applying_takesTheGameOffTheBoard() = runBlocking {
        val games = FakeSavedGames()
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository)
        viewModel.open()
        viewModel.picker.start()
        while (viewModel.state.value.stage is PlayStage.Dueling) viewModel.duel.pick(first = true)
        eventually { repository.board.play != null }

        viewModel.result.apply()

        viewModel.events.firstWithin { it is PlayEvent.Arranged }
        eventually { repository.board.play == null && KEY !in games.games }
    }

    @Test
    fun aGameOnSomeoneElsesList_staysOnThisPhone() = runBlocking {
        val games = FakeSavedGames()
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository, PlayRequest.someoneElses("abc", unranked.copy(id = 0), unranked.copy(id = 0)))
        viewModel.open()
        viewModel.picker.start()

        viewModel.duel.pick(first = true)

        eventually { "list:abc" in games.games }
        assertTrue(repository.plays.isEmpty())
        assertEquals(false, games.games.getValue("list:abc").onBoard)
    }

    @Test
    fun theBoardsCopy_whenNewer_isTheOneOffered() = runBlocking {
        val repository = MovingTierRepository(unranked.copy(play = shared(answers = 3)))
        val games = FakeSavedGames(mapOf(KEY to saved(answers = 2, onBoard = true)))
        val viewModel = viewModel(games, repository)
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        viewModel.picker.resume()

        assertEquals(3, offer.played)
        assertEquals(4, (viewModel.state.value.stage as PlayStage.Dueling).duel.number)
    }

    @Test
    fun theFilesCopy_whenNewer_isTheOneOffered() = runBlocking {
        val repository = MovingTierRepository(unranked.copy(play = shared(answers = 1)))
        val games = FakeSavedGames(mapOf(KEY to saved(answers = 3, onBoard = true)))
        val viewModel = viewModel(games, repository)
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }

        assertEquals(3, offer.played)
    }

    @Test
    fun aGameOnTheBoardOnly_isOfferedOnAPhoneThatNeverSawIt() = runBlocking {
        val repository = MovingTierRepository(unranked.copy(play = shared(answers = 2)))
        val viewModel = viewModel(FakeSavedGames(), repository)

        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        assertEquals(2, offer.played)
        assertTrue(KEY in viewModel.state.value.saved)
    }

    @Test
    fun aGameThatEndedElsewhere_isNotOffered_andLeavesThisPhoneToo() = runBlocking {
        val games = FakeSavedGames(mapOf(KEY to saved(answers = 2, onBoard = true)))
        val viewModel = viewModel(games, MovingTierRepository(unranked))

        viewModel.open()

        eventually { KEY !in games.games }
        assertNull(picker(viewModel.state.value)?.resume)
    }

    @Test
    fun aGameNeverPutOnTheBoard_isStillOffered() = runBlocking {
        val games = FakeSavedGames(mapOf(KEY to saved(answers = 2, onBoard = false)))
        val viewModel = viewModel(games, MovingTierRepository(unranked))

        viewModel.open()

        assertEquals(2, viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!!.played })
    }

    @Test
    fun startingAnotherGame_takesTheOldOneOffTheBoard_andTheFirstPickPutsTheNewOneOn() = runBlocking {
        val repository = MovingTierRepository(unranked.copy(play = shared(answers = 2)))
        val viewModel = viewModel(FakeSavedGames(), repository)
        viewModel.open()
        viewModel.state.firstWithin { picker(it)?.resume != null }

        viewModel.picker.choose(GameKind.Hill)
        viewModel.picker.start()
        eventually { repository.board.play == null }
        viewModel.duel.pick(first = false)

        eventually { repository.board.play?.let(SharedGameJson::decode)?.spec?.kind == GameKind.Hill }
    }

    @Test
    fun aCardTheBoardNoLongerHas_dropsOutOfASwissGameFromTheAccount() = runBlocking {
        val withAGoneCard = SharedGameJson.encode(
            SharedGame(spec, ids = listOf("c1", "c2", "c3", "c4", "c5", "gone"), history = history(2), round = 1, startedAt = NOW - DAY),
        )
        val repository = MovingTierRepository(unranked.copy(play = withAGoneCard))
        val viewModel = viewModel(FakeSavedGames(), repository)
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }

        assertEquals(1, offer.joined)
    }

    @Test
    fun aMonthOldGameOnTheBoard_isTakenOffIt() = runBlocking {
        val old = SharedGameJson.encode(SharedGameJson.decode(shared(answers = 2))!!.copy(startedAt = NOW - 31 * DAY))
        val repository = MovingTierRepository(unranked.copy(play = old))
        val viewModel = viewModel(FakeSavedGames(), repository)

        viewModel.open()

        eventually { repository.board.play == null }
        assertNull(picker(viewModel.state.value)?.resume)
    }

    private fun history(answers: Int): List<Outcome> {
        val history = mutableListOf<Outcome>()
        repeat(answers) {
            val (first, second) = nextPair(spec, history)!!
            history += Outcome(first, second, winner = first)
        }
        return history
    }

    private fun saved(answers: Int, onBoard: Boolean) = SavedGame(
        spec = spec,
        ids = (1L..6L).toList(),
        history = history(answers),
        round = 1,
        startedAt = NOW - DAY,
        onBoard = onBoard,
    )

    private fun shared(answers: Int): String = SharedGameJson.encode(
        SharedGame(spec, ids = (1..6).map { "c$it" }, history = history(answers), round = 1, startedAt = NOW - DAY),
    )

    private fun viewModel(
        games: FakeSavedGames,
        boards: MovingTierRepository,
        request: PlayRequest = PlayRequest.ownBoard(boards.board),
    ) = PlayViewModel(games, boards).apply {
        clock = { NOW }
        seeds = { 7 }
        offer(request)
    }

    private fun picker(state: PlayState): Picker? = (state.stage as? PlayStage.Picking)?.picker

    private suspend fun eventually(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }
}
