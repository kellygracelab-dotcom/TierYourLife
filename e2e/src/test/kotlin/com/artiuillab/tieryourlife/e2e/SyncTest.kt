package com.artiuillab.tieryourlife.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

/** Two phones signed into one account, each with its own database, meeting only through the proxy. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SyncTest {

    private lateinit var first: Phone
    private lateinit var second: Phone

    @Before
    fun signInTwice() {
        val account = Emulators.newAccount()
        first = Phone(account)
        second = Phone(Emulators.signIn(account))
    }

    @After
    fun closePhones() {
        first.close()
        second.close()
    }

    @Test
    fun aBoardMadeOnOnePhone_arrivesOnTheOther_andSoDoItsChanges() = runBlocking<Unit> {
        val id = first.newBoard("Albums", ranked = mapOf("S" to listOf("Kid A"), "A" to listOf("Blue Lines", "Mezzanine")))
        val uid = first.board(id).uid

        assertEquals(1, first.sync.sync().carried)
        second.sync.sync()

        val arrived = checkNotNull(second.boardByUid(uid))
        assertEquals("Albums", arrived.title)
        assertEquals(listOf("S", "A", "B", "C", "D"), arrived.tiers.filterNot { it.isPool }.map { it.label })
        assertEquals(listOf("Kid A"), second.cardsIn(arrived, "S"))
        assertEquals(listOf("Blue Lines", "Mezzanine"), second.cardsIn(arrived, "A"))

        val board = first.board(id)
        first.boards.renameTierList(id, "Albums, revised")
        val mezzanine = board.tiers.single { it.label == "A" }.items.single { it.title == "Mezzanine" }
        first.boards.moveItem(mezzanine.id, board.tiers.single { it.label == "S" }.id, 1)
        first.sync.sync()
        second.sync.sync()

        val changed = checkNotNull(second.boardByUid(uid))
        assertEquals("Albums, revised", changed.title)
        assertEquals(listOf("Kid A", "Mezzanine"), second.cardsIn(changed, "S"))
        assertEquals(listOf(200, 200), first.answersTo("PUT", "/boards/$uid").map { it.status })
    }

    @Test
    fun displayModes_surviveTheTripBothWays() = runBlocking<Unit> {
        val scrolling = first.newBoard("Scrolling")
        val ranked = first.newBoard("Ranked")
        first.boards.setTierListDisplayMode(scrolling, TierListDisplayMode.HORIZONTAL_SCROLL)
        first.boards.setTierListDisplayMode(ranked, TierListDisplayMode.FLAT_RANKED)
        val scrollingUid = first.board(scrolling).uid
        val rankedUid = first.board(ranked).uid

        first.sync.sync()
        second.sync.sync()

        assertEquals("HORIZONTAL_SCROLL", second.boardsApi.board(scrollingUid).displayMode)
        assertEquals("FLAT_RANKED", second.boardsApi.board(rankedUid).displayMode)
        assertEquals(TierListDisplayMode.HORIZONTAL_SCROLL, second.boardByUid(scrollingUid)?.displayMode)
        assertEquals(TierListDisplayMode.FLAT_RANKED, second.boardByUid(rankedUid)?.displayMode)

        val there = checkNotNull(second.boardByUid(scrollingUid))
        second.boards.setTierListDisplayMode(there.id, TierListDisplayMode.FLAT_RANKED)
        second.sync.sync()
        first.sync.sync()

        assertEquals(TierListDisplayMode.FLAT_RANKED, first.board(scrolling).displayMode)
    }

    @Test
    fun editsOnBothPhones_keepBothBoards() = runBlocking<Unit> {
        val id = first.newBoard("Films")
        val uid = first.board(id).uid
        first.sync.sync()
        second.sync.sync()
        val there = checkNotNull(second.boardByUid(uid))

        first.boards.renameTierList(id, "Films, first phone")
        first.sync.sync()
        second.boards.renameTierList(there.id, "Films, second phone")
        val report = second.sync.sync()

        assertEquals(0, report.refused)
        assertEquals("Films, second phone", second.boardByUid(uid)?.title)
        val kept = second.boards.getAllTierLists().single { it.uid != uid }
        assertEquals("Films, first phone", kept.title)
        assertNotNull(kept.arrivedFrom)
        assertEquals("Films, second phone", second.boardsApi.board(uid).title)

        first.sync.sync()
        assertEquals("Films, second phone", first.board(id).title)
    }

    // The other phone writes between this one reading the index and sending its
    // own: the account refuses with 409 and the board it holds, and nothing is lost.
    @Test
    fun aWriteThatLostTheRace_isRefused_andKeepsBoth() = runBlocking<Unit> {
        val id = first.newBoard("Games")
        val uid = first.board(id).uid
        first.sync.sync()
        second.sync.sync()
        val there = checkNotNull(second.boardByUid(uid))
        second.boards.renameTierList(there.id, "Games, second phone")

        val raced = AtomicBoolean(false)
        second.afterAnswer = { answer ->
            if (answer.method == "GET" && answer.path == "/boards" && raced.compareAndSet(false, true)) {
                runBlocking {
                    first.boards.renameTierList(id, "Games, first phone")
                    first.sync.sync()
                }
            }
        }
        val report = second.sync.sync()

        assertTrue(raced.get())
        assertEquals(0, report.refused)
        val writes = second.answersTo("PUT", "/boards/$uid")
        assertEquals(listOf(409, 200), writes.map { it.status })
        assertTrue("CONFLICT" in writes.first().body)
        assertEquals("Games, second phone", second.boardByUid(uid)?.title)
        assertEquals("Games, first phone", second.boards.getAllTierLists().single { it.uid != uid }.title)
        assertEquals("Games, second phone", second.boardsApi.board(uid).title)
    }

    @Test
    fun aBoardThrownAwayOnOnePhone_goesToTheTrashOnTheOther() = runBlocking<Unit> {
        val id = first.newBoard("Sport")
        val uid = first.board(id).uid
        first.sync.sync()
        second.sync.sync()

        first.boards.deleteTierListPermanently(id)
        first.sync.sync()
        second.sync.sync()

        assertEquals(listOf(204), first.answersTo("DELETE", "/boards/$uid").map { it.status })
        assertNull(second.boardByUid(uid))
        assertEquals(listOf("Sport"), second.boards.getTrashEntries().map { it.title })
        assertNotEquals(0, second.boardsApi.index().boards.count { it.uid == uid && it.deleted })
    }
}
