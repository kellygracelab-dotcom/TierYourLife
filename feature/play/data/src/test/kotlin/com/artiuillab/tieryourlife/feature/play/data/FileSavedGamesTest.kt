package com.artiuillab.tieryourlife.feature.play.data

import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        onBoard = true,
        uids = listOf("a", "b", "c", "d"),
        scopeUids = listOf("t-5", "t-1"),
        touchedAt = 1_700_000_500_000,
    )

    private fun store(file: File = File(folder.root, "play/saved-games.json")) = FileSavedGames(file, Dispatchers.Unconfined)

    @Test
    fun aSavedGame_readsBackWhole_inAFreshStore() = runBlocking {
        store().save("board:3", game)

        assertEquals(game, store().load("board:3"))
    }

    @Test
    fun aLargeLocalCohortAndLongHistoryRemainWholeAcrossReopening() = runBlocking {
        for (count in listOf(91, 2000, 5000)) {
            val large = game.copy(
                spec = game.spec.copy(count = count), ids = (1L..count.toLong()).toList(),
                history = List(3001) { Outcome(0, 1, 0) }, uids = null, scopeUids = null, onBoard = false,
            )
            assertTrue(store().save("large-$count", large))
            assertEquals(large, store().load("large-$count"))
        }
    }

    @Test
    fun damagedCohortsAndOutcomeIndicesNeverBecomeAPlayableSession() = runBlocking {
        val valid = """{"kind":"Swiss","count":3,"seed":7,"refine":false,"ids":[1,2,3],"history":[[0,1,0]],"round":1,"startedAt":5}"""
        val damaged = listOf(
            valid.replace("[1,2,3]", "[1,1,3]"), valid.replace("\"count\":3", "\"count\":4"),
            valid.replace("[0,1,0]", "[0,3,0]"), valid.replace("[0,1,0]", "[0,0,0]"),
            valid.replace("[0,1,0]", "[0,1,2]"), valid.replace("[0,1,0]", "[0,1]"),
        )
        for ((index, record) in damaged.withIndex()) {
            val file = File(folder.root, "damaged-$index.json").apply { writeText("""{"games":{"board":$record}}""") }
            assertNull(store(file).load("board"))
        }
    }

    @Test
    fun discarding_forgetsOneBoardsGame_andKeepsTheOthers() = runBlocking {
        val games = store()
        games.save("board:3", game)
        games.save("list:abc", game.copy(round = 1))

        games.discard("board:3")

        assertNull(store().load("board:3"))
        assertEquals(1, store().load("list:abc")?.round)
        assertEquals(setOf("list:abc"), store().keys())
    }

    // S22: the games were played on the last person's boards.
    @Test
    fun forgettingAll_leavesNothingForTheNextPerson() = runBlocking {
        val games = store()
        games.save("board:3", game)
        games.rememberKind(GameKind.Swiss)

        games.forgetAll()

        assertEquals(emptySet<String>(), store().keys())
        assertNull(store().lastKind())
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
    fun aFileWrittenBeforeGamesWentOnBoards_readsAsNeverSent() = runBlocking {
        val file = File(folder.root, "older.json").apply {
            writeText(
                """{"games":{"board:3":{"kind":"Hill","count":3,"seed":1,"refine":false,"ids":[1,2,3],""" +
                    """"history":[[0,1,1]],"round":0,"startedAt":5}}}""",
            )
        }

        val older = store(file).load("board:3")
        assertEquals(false, older?.onBoard)
        assertNull(older?.uids)
        assertEquals(5L, older?.touchedAt)
    }

    @Test
    fun aGameThatCouldNotBeWritten_saysSo_andIsNotTakenAsKept() = runBlocking {
        val notAFolder = folder.newFile("taken")
        val games = store(File(notAFolder, "saved-games.json"))

        assertFalse(games.save("board:3", game))
        assertNull(games.load("board:3"))
        assertTrue(store().save("board:3", game))
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
