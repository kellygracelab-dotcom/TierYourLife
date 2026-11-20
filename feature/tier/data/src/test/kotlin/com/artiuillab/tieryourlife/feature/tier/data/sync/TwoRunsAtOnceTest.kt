package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Crashlytics, builds 404 and 424: UNIQUE constraint failed: tier_lists.uid.
 * The list screen and a returning connection each started a run at launch;
 * both planned to bring the same new board down and the second insert failed,
 * so that run refused the board.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TwoRunsAtOnceTest : SyncEngineBench() {

    @Test
    fun twoRunsStartedTogether_bringANewBoardDownOnce() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 3)
        val engine = engine()

        val first = async { engine.sync() }
        val second = async { engine.sync() }

        assertEquals(0, first.await().refused + second.await().refused)
        assertEquals(listOf("remote-1"), dao.allBoards().map { it.uid })
    }

    // Both runs past their check before either writes: what the lock keeps
    // apart today, and what any other writer of the uid would meet.
    @Test
    fun aBoardWrittenBetweenTheCheckAndTheInsert_isNeitherRefusedNorCopied() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 3)
        val bothFetched = CompletableDeferred<Unit>()
        var fetching = 0
        boards.onBoard = {
            if (++fetching == 2) bothFetched.complete(Unit)
            withTimeoutOrNull(OTHER_RUN_MS) { bothFetched.await() }
        }

        val first = async { engine(lock = SyncLock()).sync() }
        val second = async { engine(lock = SyncLock()).sync() }

        assertEquals(0, first.await().refused + second.await().refused)
        assertEquals(listOf("remote-1"), dao.allBoards().map(TierListEntity::uid))
        assertEquals(1, dao.syncRecordsOf(someone).count { it.listUid == "remote-1" })
    }

    // The one case a board here under that uid is not this account's to keep.
    @Test
    fun anotherOwnersBoardUnderTheUid_stillGetsTheAccountsCopyBeside() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 3)
        boards.onBoard = { uid ->
            boards.onBoard = {}
            dao.addBoard(TierListEntity(uid = uid, title = "Bob's games", ownerUid = "bob"), emptyList())
        }

        val report = engine().sync()

        assertEquals(0, report.refused)
        val mine = dao.boardsOf(someone).single()
        assertEquals(standInFor(someone, "remote-1"), mine.uid)
        assertEquals("Games", mine.title)
        assertEquals("Bob's games", dao.boardByUid("remote-1")!!.title)
    }
}
