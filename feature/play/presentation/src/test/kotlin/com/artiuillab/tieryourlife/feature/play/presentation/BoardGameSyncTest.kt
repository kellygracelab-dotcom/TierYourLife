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
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
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

        viewModel.events.firstWithin { it is PlayEvent.Arranged }
        eventually { repository.board.play == null && KEY !in games.games }
    }

    @Test
    fun aGameOnSomeoneElsesList_staysOnThisPhone() = runBlocking {
        val games = FakeSavedGames()
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository, PlayRequest.someoneElses("abc", version = 7, unranked.copy(id = 0), unranked.copy(id = 0)))
        viewModel.open()
        viewModel.picker.start()

        viewModel.duel.pick(first = true)

        eventually { "list:abc@7" in games.games }
        assertTrue(repository.plays.isEmpty())
        assertEquals(false, games.games.getValue("list:abc@7").onBoard)
    }

    @Test
    fun aGameOfAnOlderVersionOfTheList_isNotOffered_andGoes() = runBlocking {
        val older = saved(answers = 2, onBoard = false)
        val games = FakeSavedGames(mapOf("list:abc@6" to older, "list:abc" to older, "list:abcd@6" to older, KEY to older))
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository, PlayRequest.someoneElses("abc", version = 7, unranked.copy(id = 0), unranked.copy(id = 0)))
        viewModel.open()

        eventually { "list:abc@6" !in games.games && "list:abc" !in games.games }
        assertNull(picker(viewModel.state.value)?.resume)
        assertEquals(setOf("list:abcd@6", KEY), games.games.keys)
    }

    @Test
    fun aGameOfTheSameVersion_isOffered() = runBlocking {
        val games = FakeSavedGames(mapOf("list:abc@7" to saved(answers = 2, onBoard = false)))
        val repository = MovingTierRepository(unranked)
        val viewModel = viewModel(games, repository, PlayRequest.someoneElses("abc", version = 7, unranked.copy(id = 0), unranked.copy(id = 0)))
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        assertEquals(2, offer.played)
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

    // QA 490 N2b: a game the website saved after the board was opened showed only once it was opened again.
    @Test
    fun aGameTheWebsiteSavedSinceTheBoardOpened_isWhatThePickerOffers() = runBlocking {
        val repository = MovingTierRepository(unranked.copy(play = shared(answers = 1)))
        val games = FakeSavedGames(mapOf(KEY to saved(answers = 1, onBoard = true)))
        val bracket = GameSpec(GameKind.Bracket, 6, seed = 5)
        val website = SharedGameJson.encode(
            SharedGame(bracket, ids = (1..6).map { "c$it" }, history = historyOf(bracket, answers = 2), round = 1, startedAt = NOW),
        )
        val sync = object : BoardSync {
            override suspend fun sync(): SyncReport {
                repository.board = repository.board.copy(play = website)
                return SyncReport(signedIn = true, carried = 1)
            }
        }
        val viewModel = viewModel(games, repository, sync = sync)

        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume?.kind == GameKind.Bracket }.let { picker(it)!!.resume!! }
        assertEquals(2, offer.played)
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
    fun aMissingParticipantRequiresAnExplicitRestart() = runBlocking {
        val withAGoneCard = SharedGameJson.encode(
            SharedGame(spec, ids = listOf("c1", "c2", "c3", "c4", "c5", "gone"), history = history(2), round = 1, startedAt = NOW - DAY),
        )
        val repository = MovingTierRepository(unranked.copy(play = withAGoneCard))
        val viewModel = viewModel(FakeSavedGames(), repository)
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }

        assertEquals(0, offer.joined)
        assertTrue(offer.changed)
    }

    // The account lets a game go a month after its last write, which the board does not say.
    @Test
    fun aGameStartedAMonthAgo_onTheBoard_isStillOffered() = runBlocking {
        val old = SharedGameJson.encode(SharedGameJson.decode(shared(answers = 2))!!.copy(startedAt = NOW - 31 * DAY))
        val repository = MovingTierRepository(unranked.copy(play = old))
        val viewModel = viewModel(FakeSavedGames(), repository)

        viewModel.open()

        assertEquals(2, viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!!.played })
        assertEquals(old, repository.board.play)
    }

    @Test
    fun aGameLeftAMonthAgo_goesFromThisPhoneAndFromTheBoard() = runBlocking {
        val left = saved(answers = 2, onBoard = true).copy(startedAt = NOW - 40 * DAY, touchedAt = NOW - 31 * DAY)
        val onBoard = SharedGameJson.encode(SharedGameJson.decode(shared(answers = 2))!!.copy(startedAt = NOW - 40 * DAY))
        val repository = MovingTierRepository(unranked.copy(play = onBoard))
        val games = FakeSavedGames(mapOf(KEY to left))
        val viewModel = viewModel(games, repository)

        viewModel.open()

        eventually { repository.board.play == null && KEY !in games.games }
        assertNull(picker(viewModel.state.value)?.resume)
    }

    @Test
    fun aGameLeftHereAMonthAgo_butPlayedOnElsewhere_staysOnTheBoard() = runBlocking {
        val left = saved(answers = 2, onBoard = true).copy(touchedAt = NOW - 31 * DAY)
        val repository = MovingTierRepository(unranked.copy(play = shared(answers = 3)))
        val games = FakeSavedGames(mapOf(KEY to left))
        val viewModel = viewModel(games, repository)

        viewModel.open()

        assertEquals(3, viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!!.played })
        assertEquals(shared(answers = 3), repository.board.play)
        assertTrue(KEY !in games.games)
    }

    @Test
    fun aSyncThatWritesEveryRowAfresh_keepsTheGame_withEveryAnswer() = runBlocking {
        GameKind.entries.forEach { kind ->
            val games = FakeSavedGames()
            val repository = MovingTierRepository(unranked)
            val before = viewModel(games, repository)
            before.open()
            before.picker.choose(kind)
            before.picker.start()
            before.duel.pick(first = true)
            before.duel.pick(first = false)
            before.duel.quit()
            eventually { games.games[KEY]?.history?.size == 2 && repository.board.play != null }
            val play = repository.board.play
            repository.board = renumbered(repository.board)

            val after = viewModel(games, repository)
            after.open()
            val offer = after.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
            after.picker.resume()

            assertEquals(kind.name, 2, offer.played)
            assertFalse(kind.name, offer.changed)
            assertEquals(kind.name, 3, (after.state.value.stage as PlayStage.Dueling).duel.number)
            assertEquals(kind.name, play, repository.board.play)
        }
    }

    @Test
    fun aGameSavedBeforeItsUidsWereKept_takesTheBoardsCopy_afterSuchASync() = runBlocking {
        val repository = MovingTierRepository(renumbered(unranked).copy(play = shared(answers = 2)))
        val games = FakeSavedGames(mapOf(KEY to saved(answers = 2, onBoard = true)))
        val viewModel = viewModel(games, repository)
        viewModel.open()

        val offer = viewModel.state.firstWithin { picker(it)?.resume != null }.let { picker(it)!!.resume!! }
        viewModel.picker.resume()

        assertEquals(0, offer.joined)
        assertEquals(3, (viewModel.state.value.stage as PlayStage.Dueling).duel.number)
    }

    @Test
    fun aResumeWithMissingParticipantsDoesNotRewriteOrShortenTheSavedGame() = runBlocking {
        val playedCardsGone = SharedGameJson.encode(
            SharedGame(
                spec,
                ids = (1..6).map { "c$it" },
                history = listOf(Outcome(0, 1, winner = 0), Outcome(2, 3, winner = 2)),
                round = 1,
                startedAt = NOW - DAY,
            ),
        )
        val repository = MovingTierRepository(
            board(1L to emptyList(), 2L to emptyList(), 3L to emptyList(), POOL to listOf(5L, 6L, 7L)).copy(play = playedCardsGone),
        )
        val viewModel = viewModel(FakeSavedGames(), repository)
        viewModel.open()
        viewModel.state.firstWithin { picker(it)?.resume != null }

        viewModel.picker.resume()

        assertTrue(viewModel.state.value.stage is PlayStage.Picking)
        assertTrue(picker(viewModel.state.value)!!.resume!!.changed)
        delay(100)
        assertTrue(repository.plays.isEmpty())
        assertEquals(playedCardsGone, repository.board.play)
    }

    /** The board as a sync that takes the account's copy leaves it: the same uids, every row numbered anew. */
    private fun renumbered(board: TierList): TierList = board.copy(
        tiers = board.tiers.map { tier -> tier.copy(id = tier.id + 100, items = tier.items.map { it.copy(id = it.id + 10) }) },
    )

    private fun history(answers: Int): List<Outcome> {
        val history = mutableListOf<Outcome>()
        repeat(answers) {
            val (first, second) = nextPair(spec, history)!!
            history += Outcome(first, second, winner = first)
        }
        return history
    }

    private fun historyOf(spec: GameSpec, answers: Int): List<Outcome> {
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
        sync: BoardSync = BoardSync.None,
    ) = PlayViewModel(games, boards, sync).apply {
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
