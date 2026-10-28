package com.artiuillab.tieryourlife.feature.play.data

import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileSavedGamesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val game = SavedGame(
        spec = GameSpec(GameKind.Swiss, 4, seed = -7, refine = true),
        ids = listOf(10, 20, 30, 40),
        history = listOf(Outcome(0, 1, winner = 1), Outcome(2, 3, winner = null)),
        round = 2,
        startedAt = 1_700_000_000_000,
        scope = listOf(5, -1),
        keepTier = false,
    )

    private fun store(file: File = File(folder.root, "play/saved-games.json")) = FileSavedGames(file, Dispatchers.Unconfined)

    @Test
    fun aSavedGame_readsBackWhole_inAFreshStore() = runBlocking {
        store().save("board:3", game)

        assertEquals(game, store().load("board:3"))
    }

    @Test
    fun discarding_forgetsOneBoardsGame_andKeepsTheOthers() = runBlocking {
        val games = store()
        games.save("board:3", game)
        games.save("list:abc", game.copy(round = 1))

        games.discard("board:3")

        assertNull(store().load("board:3"))
        assertEquals(1, store().load("list:abc")?.round)
    }

    @Test
    fun theLastGameKind_isRememberedForTheDevice() = runBlocking {
        assertNull(store().lastKind())
        store().rememberKind(GameKind.Hill)
        assertEquals(GameKind.Hill, store().lastKind())
    }

    @Test
    fun aFileThatWillNotRead_isNoGames_ratherThanAFailure() = runBlocking {
        val file = File(folder.root, "broken.json").apply { writeText("{ not json") }

        assertNull(store(file).load("board:3"))
        store(file).save("board:3", game)
        assertEquals(game, store(file).load("board:3"))
    }

    @Test
    fun aGameOfAKindThisVersionDoesNotKnow_isDropped() = runBlocking {
        val file = File(folder.root, "future.json").apply {
            writeText(
                """{"games":{"board:3":{"kind":"Ladder","count":3,"seed":1,"refine":false,"ids":[1,2,3],""" +
                    """"history":[],"round":0,"startedAt":0}}}""",
            )
        }

        assertNull(store(file).load("board:3"))
    }
}
