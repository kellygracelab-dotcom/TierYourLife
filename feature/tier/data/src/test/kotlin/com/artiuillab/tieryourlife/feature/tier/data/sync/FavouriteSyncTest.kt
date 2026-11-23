package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The star follows the account: the later change wins on either side, and it is never an edit. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FavouriteSyncTest : SyncEngineBench() {

    private suspend fun keptBoard(): String = givenLocalBoard("Films").also { engine().sync() }

    private suspend fun starHere(uid: String, at: Long?, changedAt: Long) =
        database.tierDao().setFavouritedAt(dao.boardByUid(uid)!!.id, at, changedAt)

    private fun starThere(uid: String, at: Long?, changedAt: Long) {
        val stored = boards.server.getValue(uid)
        boards.server[uid] = stored.copy(revision = stored.revision + 1, favouritedAt = at, favouriteChangedAt = changedAt)
    }

    @Test
    fun aStarOnTheWeb_comesDown_withoutAWrite() = runBlocking {
        val uid = keptBoard()
        val fingerprint = fingerprintOf(uid)
        starThere(uid, at = T1, changedAt = T1)

        engine().sync()

        val board = dao.boardByUid(uid)!!
        assertEquals(T1, board.favouritedAt)
        assertEquals(1, boards.kept.size)
        assertEquals("the star is not an edit", fingerprint, fingerprintOf(uid))
        assertEquals(emptySet<String>(), engine().unsent())
    }

    @Test
    fun aStarHere_goesUp_once() = runBlocking {
        val uid = keptBoard()
        starHere(uid, at = T1, changedAt = T1)
        assertTrue(engine().unsent().isNotEmpty())

        engine().sync()
        engine().sync()

        assertEquals(2, boards.kept.size)
        val sent = boards.kept.last().second
        assertEquals(JsonPrimitive(T1), sent.favouritedAt)
        assertEquals(T1, sent.favouriteChangedAt)
        assertEquals(T1, boards.server.getValue(uid).favouritedAt)
        assertEquals(emptySet<String>(), engine().unsent())
    }

    @Test
    fun unstarringHere_afterTheWebStarred_takesItOffThere() = runBlocking {
        val uid = keptBoard()
        starThere(uid, at = T1, changedAt = T1)
        engine().sync()

        starHere(uid, at = null, changedAt = T2)
        engine().sync()

        assertEquals(JsonNull, boards.kept.last().second.favouritedAt)
        assertNull(boards.server.getValue(uid).favouritedAt)
        assertNull(dao.boardByUid(uid)!!.favouritedAt)
    }

    @Test
    fun anOlderChangeHere_givesWayToTheAccounts() = runBlocking {
        val uid = keptBoard()
        starHere(uid, at = T1, changedAt = T1)
        starThere(uid, at = null, changedAt = T2)

        engine().sync()

        assertNull(dao.boardByUid(uid)!!.favouritedAt)
        assertEquals(1, boards.kept.size)
        assertEquals(emptySet<String>(), engine().unsent())
    }

    // Starred before the star followed the account: counted from the star itself, sent once.
    @Test
    fun aStarFromBefore_goesUpOnce() = runBlocking {
        val uid = keptBoard()
        dao.setStar(dao.boardByUid(uid)!!.id, at = T1, changedAt = null)

        engine().sync()
        engine().sync()

        assertEquals(2, boards.kept.size)
        assertEquals(T1, boards.kept.last().second.favouriteChangedAt)
        assertEquals(T1, boards.server.getValue(uid).favouritedAt)
    }

    @Test
    fun aBoardNeverStarred_sendsNoStar_soTheAccountKeepsItsOwn() = runBlocking {
        givenLocalBoard("Films")

        engine().sync()

        val sent = boards.kept.single().second
        assertNull(sent.favouritedAt)
        assertNull(sent.favouriteChangedAt)
    }

    // The device was behind by a star alone: 409 with the account's board, merged, written again.
    @Test
    fun aRefusalOverTheStarAlone_keepsTheLaterOne_andWritesAgain() = runBlocking {
        val uid = keptBoard()
        starHere(uid, at = T2, changedAt = T2)
        boards.refuseNextWith = boards.server.getValue(uid).copy(revision = 5, favouritedAt = null, favouriteChangedAt = T1)

        engine().sync()

        val again = boards.kept.last().second
        assertEquals(5, again.basedOn)
        assertEquals(T2, again.favouriteChangedAt)
        assertEquals(T2, dao.boardByUid(uid)!!.favouritedAt)
        assertEquals("no second board over a star", 1, dao.boardsOf(someone).size)
        assertEquals(emptySet<String>(), engine().unsent())
    }

    @Test
    fun aRefusalWithALaterStar_takesIt_andWritesNothing() = runBlocking {
        val uid = keptBoard()
        starHere(uid, at = T1, changedAt = T1)
        boards.refuseNextWith = boards.server.getValue(uid).copy(revision = 5, favouritedAt = T3, favouriteChangedAt = T3)

        engine().sync()

        assertEquals(T3, dao.boardByUid(uid)!!.favouritedAt)
        assertEquals(1, boards.kept.size)
        assertEquals(1, dao.boardsOf(someone).size)
    }

    @Test
    fun aBoardChangedOnTheWeb_bringsItsStar_unlessTheOneHereIsLater() = runBlocking {
        val later = keptBoard()
        val earlier = givenLocalBoard("Games").also { engine().sync() }
        starHere(later, at = T3, changedAt = T3)
        starHere(earlier, at = T1, changedAt = T1)
        listOf(later, earlier).forEach { uid ->
            val stored = boards.server.getValue(uid)
            boards.server[uid] = theirBoard(uid, title = "Renamed", revision = stored.revision + 1).copy(favouritedAt = T2, favouriteChangedAt = T2)
        }

        engine().sync()

        assertEquals("Renamed", dao.boardByUid(earlier)!!.title)
        assertEquals(T2, dao.boardByUid(earlier)!!.favouritedAt)
        assertEquals("Renamed", dao.boardByUid(later)!!.title)
        assertEquals(T3, dao.boardByUid(later)!!.favouritedAt)
        assertEquals("the later one here went up", T3, boards.server.getValue(later).favouriteChangedAt)
    }

    @Test
    fun aBoardNewHere_comesWithItsStar() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 3).copy(favouritedAt = T1, favouriteChangedAt = T1)

        engine().sync()

        assertEquals(T1, dao.boardByUid("remote-1")!!.favouritedAt)
        assertEquals(emptySet<String>(), engine().unsent())
    }

    private companion object {
        const val T1 = 1_795_000_000_000
        const val T2 = 1_795_000_100_000
        const val T3 = 1_795_000_200_000
    }
}
