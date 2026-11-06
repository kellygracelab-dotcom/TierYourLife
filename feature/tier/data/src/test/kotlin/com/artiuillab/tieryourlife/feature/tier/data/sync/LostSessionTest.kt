package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.PersonalData
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Firebase ending a session nobody signed out of left the boards under a uid only a sign-in back would show. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LostSessionTest : SyncEngineBench() {

    private val preferences = FakeAppPreferences()
    private val accounts = FakeAccountRepository(initial = ann)
    private val ownership by lazy { BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences) }
    private var forgotten = 0
    private val thePerson = ThePerson(preferences, setOf(PersonalData { forgotten++ }))

    private val session by lazy { LostSession(accounts, database.ownerDao(), database.guestBoardsDao(), ownership, lock, preferences, thePerson) }
    private val merge by lazy {
        RoomBoardMerge(dao, database.ownerDao(), database.guestBoardsDao(), boards, ownership, lock, preferences, accounts, thePerson) { 0L }
    }

    private fun annsEngine() = engine(preferences = preferences, accounts = accounts, ownership = ownership)

    private suspend fun ownerOf(uid: String) = dao.boardByUid(uid)!!.ownerUid

    private suspend fun annWithTwoBoardsSynced(): Pair<String, String> {
        val films = givenLocalBoard("Films", publishedId = "pub-1", owner = "ann")
        val games = givenLocalBoard("Games", owner = "ann", itemTitle = "Doom")
        session.notice()
        annsEngine().sync()
        boards.kept.clear()
        return films to games
    }

    private suspend fun sessionLost() {
        accounts.accounts.value = Account.Guest
        session.notice()
    }

    private suspend fun signIn(account: Account.SignedIn) {
        accounts.accounts.value = account
        session.notice()
    }

    @Test
    fun aLostSession_leavesTheBoardsOnThePhone_andForgetsThePerson() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()

        sessionLost()

        assertNull(ownerOf(films))
        assertNull(ownerOf(games))
        assertEquals("kept for signing back in", setOf(films, games), dao.syncRecordsOf("ann").map { it.listUid }.toSet())
        assertEquals(1, forgotten)
        assertNull(preferences.lastSignedIn())
    }

    @Test
    fun signingBackIn_findsEachBoardBesideItsCopy_withNoQuestionAndNoDuplicate() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()
        sessionLost()
        database.tierDao().updateTierListTitle(dao.boardByUid(games)!!.id, "Games I finished")
        val madeSignedOut = givenLocalBoard("Books", owner = null)

        signIn(ann)
        val asked = merge.guestBoards()
        merge.keepHere()
        annsEngine().sync()

        assertEquals("only the board made while signed out", 1, asked)
        assertEquals("ann", ownerOf(films))
        assertEquals("ann", ownerOf(games))
        assertNull(ownerOf(madeSignedOut))
        assertEquals(listOf(games), boards.kept.map { it.first })
        assertEquals(setOf(films, games), boards.server.keys)
        assertTrue(boards.server.values.none { it.deleted })
    }

    @Test
    fun aRunAsTheAccount_takesItsBoardsBackBeforePlanning_soNoneIsDeletedInIt() = runBlocking {
        annWithTwoBoardsSynced()
        sessionLost()

        // Signed back in with nothing noticing yet: the run alone must not read the boards as thrown away.
        accounts.accounts.value = ann
        annsEngine().sync()

        assertTrue(boards.server.values.none { it.deleted })
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun signingIntoAnotherAccount_asksAboutThem_andTheFirstKeepsItsCopies() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()
        sessionLost()

        signIn(bob)
        val asked = merge.guestBoards()
        merge.moveIn(fromThisPhone = null)

        assertEquals(2, asked)
        assertEquals("bob", ownerOf(films))
        assertEquals("bob", ownerOf(games))
        assertTrue("ann's records went with the boards", dao.syncRecordsOf("ann").isEmpty())

        accounts.accounts.value = ann
        annsEngine().sync()
        assertTrue(boards.server.values.none { it.deleted })
    }

    @Test
    fun aSignOutByChoice_isNotALostSession() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()

        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()

        assertEquals("ann", ownerOf(films))
        assertEquals(0, forgotten)
        assertNull(preferences.lastSignedIn())
    }

    @Test
    fun withAnEraseUnanswered_theBoardsAreKeptAsTheDeletedAccounts() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        preferences.setErasureUnderWay(ErasureMark("ann", 0L).encoded())

        sessionLost()

        assertNull(ownerOf(films))
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        assertNull(preferences.erasureUnderWay())
        assertEquals(1, forgotten)
    }

    @Test
    fun theSameAccountStillSignedIn_changesNothing() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()

        session.notice()

        assertEquals("ann", ownerOf(films))
        assertEquals("ann", preferences.lastSignedIn())
        assertEquals(0, forgotten)
    }

    @Test
    fun movingBoardsIn_dropsOnlyOtherAccountsRecordsOfThem() = runBlocking {
        val guest = givenLocalBoard("Guest's", owner = null)
        val bobs = givenLocalBoard("Bob's", owner = "bob")
        dao.remember(BoardSyncEntity(account = "ann", listUid = guest, revision = 1, fingerprint = "f", syncedAt = 0))
        dao.remember(BoardSyncEntity(account = BoardSyncEntity.UNCLAIMED, listUid = guest, revision = 1, fingerprint = "f", syncedAt = 0))
        dao.remember(BoardSyncEntity(account = "ann", listUid = bobs, revision = 1, fingerprint = "f", syncedAt = 0))

        database.guestBoardsDao().moveGuestBoardsTo("carol")

        assertEquals("carol", ownerOf(guest))
        assertEquals(listOf(bobs), dao.syncRecordsOf("ann").map { it.listUid })
        assertEquals(listOf(guest), dao.syncRecordsOf(BoardSyncEntity.UNCLAIMED).map { it.listUid })
    }
}
