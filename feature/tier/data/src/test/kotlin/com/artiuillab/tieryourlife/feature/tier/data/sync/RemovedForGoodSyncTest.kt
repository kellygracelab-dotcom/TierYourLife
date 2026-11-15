package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardRemoval
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 15.11: Danylo emptied the trash on the website and found the boards in the
 * phone's trash. Deleted for good is gone everywhere; only a board with
 * something of this phone's own in it waits in the trash.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RemovedForGoodSyncTest : SyncEngineBench() {

    @Test
    fun aBoardDeletedForGoodElsewhere_isGoneHere_andNothingGoesBackUp() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forgetForever(uid)
        boards.kept.clear()

        repeat(3) { engine().sync() }

        assertNull(dao.boardByUid(uid))
        assertTrue(dao.allSyncRecords().none { it.listUid == uid })
        assertTrue(boards.kept.isEmpty())
        assertTrue(boards.removals.isEmpty())
        assertEquals(true, boards.server.getValue(uid).deleted)
    }

    @Test
    fun aBoardInTheTrashHere_isGone_whenDeletedForGoodElsewhere() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        database.tierDao().markTierListsDeleted(listOf(dao.boardByUid(uid)!!.id), deletedAt = 1)
        boards.forgetForever(uid)

        engine().sync()

        assertNull(dao.boardByUid(uid))
    }

    // What was left of the 15.11 library: in the trash for a marker without a
    // reason, then deleted for good after all.
    @Test
    fun aBoardTrashedForAnEarlierMarker_isGone_whenThatBecomesForever() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forget(uid)
        engine().sync()
        boards.forgetForever(uid)

        engine().sync()

        assertNull(dao.boardByUid(uid))
    }

    @Test
    fun anEditHere_waitsInTheTrash_andGoesBackUpOnlyWhenTakenOut() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forgetForever(uid)
        dao.renameBoard(uid, "Films, edited here")
        boards.kept.clear()

        repeat(2) { engine().sync() }

        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
        assertTrue(boards.kept.isEmpty())

        database.tierDao().restoreTierLists(listOf(dao.boardByUid(uid)!!.id))
        engine().sync()

        assertNull(dao.boardByUid(uid)!!.deletedAt)
        assertFalse(boards.server.getValue(uid).deleted)
        assertEquals("Films, edited here", boards.server.getValue(uid).title)
    }

    @Test
    fun aBoardThisPhoneNeverAgreedOn_waitsInTheTrash_forAForeverMarker() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.server[uid] = theirBoard(uid, title = "Films", revision = 3).copy(deleted = true, removal = BoardRemoval.FOREVER)

        repeat(2) { engine().sync() }

        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun aMarkerWithoutAReason_stillOnlyTrashes() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forget(uid)

        engine().sync()

        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
    }

    @Test
    fun emptyingTheTrashHere_tellsTheAccountItIsForever() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        throwAway(uid, signedInAs = someone)

        engine().sync()

        assertEquals(listOf(uid to BoardRemoval.FOREVER), boards.removals)
    }
}
