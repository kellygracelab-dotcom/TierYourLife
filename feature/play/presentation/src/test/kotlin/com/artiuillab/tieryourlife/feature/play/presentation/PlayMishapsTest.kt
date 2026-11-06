package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private const val NOW = 1_800_000_000_000L
private const val KEY = "board:1"

/** What Play does when a write fails, or when the list it plays is published again under it. */
class PlayMishapsTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val unranked = board(1L to emptyList(), 2L to emptyList(), 3L to emptyList(), POOL to (1L..6L).toList())
    private val theirs = board(1L to listOf(1L, 2L, 3L), 2L to listOf(4L, 5L, 6L), POOL to emptyList(), id = 0)
    private val mine = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList(), id = 0)

    @Test
    fun anApplyThatFailsHalfway_putsTheBoardBack_andKeepsTheGameToApplyAgain() = runBlocking {
        val repository = MovingTierRepository(unranked)
        val games = FakeSavedGames()
        val viewModel = viewModel(games, FlakyBoards(repository, failing = 3))
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()

        viewModel.result.apply()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.Failed, said.notice)
        assertEquals(unranked.ids(), repository.board.ids())
        assertNotNull(games.games[KEY])
        viewModel.open()
        viewModel.state.firstWithin { picker(it)?.resume != null }
        viewModel.picker.resume()
        assertTrue(viewModel.state.value.stage is PlayStage.Finished)
    }

    @Test
    fun anApplyOnABoardThatWillNotRead_saysSo_andKeepsTheGame() = runBlocking {
        val boards = FlakyBoards(MovingTierRepository(unranked), failing = 0)
        val games = FakeSavedGames()
        val viewModel = viewModel(games, boards)
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()
        boards.gone = true

        viewModel.result.apply()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.Failed, said.notice)
        assertNotNull(games.games[KEY])
    }

    @Test
    fun quittingWhenNothingTookTheGame_saysItWasNotSaved() = runBlocking {
        val games = FakeSavedGames().apply { writes = false }
        val viewModel = viewModel(games, MovingTierRepository(unranked), PlayRequest.someoneElses("abc", 7, theirs, mine))
        viewModel.open()
        viewModel.picker.start()
        viewModel.duel.pick(first = true)

        viewModel.duel.quit()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.NotSaved, said.notice)
        assertFalse("list:abc@7" in viewModel.state.value.saved)
    }

    @Test
    fun quittingWhenOnlyTheBoardTookTheGame_stillSaysSaved() = runBlocking {
        val games = FakeSavedGames().apply { writes = false }
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository)
        viewModel.open()
        viewModel.picker.start()
        viewModel.duel.pick(first = true)

        viewModel.duel.quit()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertTrue(said.notice is PlayNotice.GameSaved)
        assertNotNull(repository.board.play)
    }

    @Test
    fun aResultOfTheOldVersion_isNotPutIntoTheNewOnesMine() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games, MovingTierRepository(unranked), PlayRequest.someoneElses("abc", 7, theirs, mine))
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()
        viewModel.offer(PlayRequest.someoneElses("abc", 8, theirs, mine))

        viewModel.result.apply()

        val first = viewModel.events.firstWithin { it is PlayEvent.Said || it is PlayEvent.Arranged }
        assertEquals(PlayEvent.Said(PlayNotice.ListUpdated), first)
        eventually { games.games.isEmpty() }
    }

    @Test
    fun quittingAGameOfTheOldVersion_saysItWent_ratherThanSaved() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games, MovingTierRepository(unranked), PlayRequest.someoneElses("abc", 7, theirs, mine))
        viewModel.open()
        viewModel.picker.start()
        viewModel.duel.pick(first = true)
        viewModel.offer(PlayRequest.someoneElses("abc", 8, theirs, mine))

        viewModel.duel.quit()

        val said = viewModel.events.firstWithin { it is PlayEvent.Said } as PlayEvent.Said
        assertEquals(PlayNotice.ListUpdated, said.notice)
        eventually { games.games.isEmpty() }
    }

    @Test
    fun playingAgainAfterANewVersion_offersNothingOfTheOldOne() = runBlocking {
        val games = FakeSavedGames()
        val viewModel = viewModel(games, MovingTierRepository(unranked), PlayRequest.someoneElses("abc", 7, theirs, mine))
        viewModel.open()
        viewModel.picker.start()
        viewModel.playToTheEnd()
        viewModel.offer(PlayRequest.someoneElses("abc", 8, theirs, mine))

        viewModel.result.playAgain()

        eventually { games.games.isEmpty() }
        delay(100)
        assertNull(picker(viewModel.state.value)!!.resume)
    }

    private fun viewModel(
        games: FakeSavedGames,
        boards: TierRepository,
        request: PlayRequest = PlayRequest.ownBoard(unranked),
    ) = PlayViewModel(games, boards).apply {
        clock = { NOW }
        seeds = { 7 }
        offer(request)
    }

    /** Moves as [inner] does, except its [failing]th move, which fails once; and its board can go. */
    private class FlakyBoards(private val inner: MovingTierRepository, private val failing: Int) : TierRepository by inner {
        private var made = 0
        var gone = false

        override suspend fun getTierListById(id: Long): TierList? = if (gone) null else inner.getTierListById(id)

        override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
            made += 1
            if (made == failing) throw IOException("The disk is full")
            inner.moveItem(itemId, toTierId, toPosition)
        }
    }

    private fun picker(state: PlayState): Picker? = (state.stage as? PlayStage.Picking)?.picker

    private fun PlayViewModel.playToTheEnd() {
        while (state.value.stage is PlayStage.Dueling) duel.pick(first = true)
    }

    private suspend fun eventually(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }
}
