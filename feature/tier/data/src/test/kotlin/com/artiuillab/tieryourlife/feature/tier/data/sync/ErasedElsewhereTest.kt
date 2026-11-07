package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.ErasedAccounts
import com.artiuillab.tieryourlife.core.settings.AccountErasedNotice
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.PersonalData
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The proxy said ACCOUNT_ERASED: another phone deleted the account this one is still signed into. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ErasedElsewhereTest : SyncEngineBench() {

    private val preferences = FakeAppPreferences()
    private var atSignOut: Pair<Boolean, String?>? = null
    private val accounts = FakeAccountRepository(
        initial = ann,
        onSignOut = { atSignOut = preferences.accountErasedNoticePending() to preferences.erasedAccountToLeave() },
    )
    private val notice = AccountErasedNotice(preferences)
    private var forgotten = 0
    private val ownership by lazy { BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences) }

    private fun watcher() = ErasedElsewhere(
        ErasedAccounts(),
        RoomBoardMerge(
            dao,
            database.ownerDao(),
            boards,
            ownership,
            lock,
            preferences,
            accounts,
            ThePerson(preferences, setOf(PersonalData { forgotten++ })),
        ) { 0L },
        notice,
        preferences,
    )

    private suspend fun ownerOf(uid: String) = dao.boardByUid(uid)!!.ownerUid

    @Test
    fun theSignedInAccountErased_signsOutAndKeepsItsBoardsHere() = runBlocking {
        val films = givenLocalBoard("Films", publishedId = "pub-1", owner = "ann")
        val others = givenLocalBoard("Bob's games", owner = "bob")
        engine(account = ann).sync()
        preferences.setLastSignedIn("ann")

        watcher().leave("ann")

        assertEquals(Account.Guest, accounts.current())
        assertNull("the phone's own now, as after deleting it here", ownerOf(films))
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertEquals("bob", ownerOf(others))
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        assertNull("not a session lost: nothing is held for it", preferences.lastSignedIn())
        assertTrue(preferences.heldBoards().isEmpty())
        assertEquals(1, forgotten)
        assertTrue(notice.pending.value)
        assertTrue("still there after a restart", preferences.accountErasedNoticePending())
    }

    // Answered after a switch, or to a request still on its way when Bob signed out: his boards are let go, Ann is left be.
    @Test
    fun anotherAccountErased_keepsItsBoardsHere_andLeavesTheSignedInOneBe() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        val games = givenLocalBoard("Bob's games", owner = "bob")
        ownership.holdFor("bob")
        val books = givenLocalBoard("Bob's books", owner = "bob")
        preferences.setLastSignedIn("ann")

        watcher().leave("bob")

        assertNull("the phone's own now", ownerOf(books))
        assertNull(ownerOf(games))
        assertTrue("no longer held for an account that cannot come back", preferences.heldBoards().isEmpty())
        assertEquals(ann, accounts.current())
        assertEquals("ann", ownerOf(films))
        assertEquals("ann", preferences.lastSignedIn())
        assertEquals(0, forgotten)
        assertFalse(notice.pending.value)
    }

    // The session was already gone, and its boards held for it as for any lost one.
    @Test
    fun aSessionAlreadyLost_keepsItsBoardsHere_andSaysSo() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        accounts.accounts.value = Account.Guest
        ownership.holdFor("ann")
        preferences.setLastSignedIn("ann")

        watcher().leave("ann")

        assertNull(ownerOf(films))
        assertTrue(preferences.heldBoards().isEmpty())
        assertNull(preferences.lastSignedIn())
        assertEquals(Account.Guest, accounts.current())
        assertEquals(1, forgotten)
        assertTrue(notice.pending.value)
    }

    @Test
    fun aGuestWithNobodyBefore_keepsTheBoards_andSaysNothing() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        accounts.accounts.value = Account.Guest

        watcher().leave("ann")

        assertNull(ownerOf(films))
        assertEquals(0, forgotten)
        assertFalse(notice.pending.value)
    }

    @Test
    fun theNoticeAndTheLeave_areWrittenDownBeforeTheSignOut() = runBlocking {
        watcher().leave("ann")

        assertEquals(true to "ann", atSignOut)
        assertNull("done", preferences.erasedAccountToLeave())
    }

    // Killed before the sign-out: at the next start it is all done again.
    @Test
    fun aLeaveTheProcessDidNotFinish_isDoneAgainAtStart() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        preferences.setLastSignedIn("ann")
        preferences.setErasedAccountToLeave("ann")

        watcher().resume()

        assertEquals(Account.Guest, accounts.current())
        assertNull(ownerOf(films))
        assertTrue(notice.pending.value)
        assertNull(preferences.erasedAccountToLeave())
    }

    @Test
    fun leftTwice_endsAsOnce() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        preferences.setLastSignedIn("ann")

        watcher().leave("ann")
        watcher().leave("ann")

        assertEquals(Account.Guest, accounts.current())
        assertNull(ownerOf(films))
        assertEquals(1, forgotten)
        assertTrue(notice.pending.value)
    }

    @Test
    fun theNotice_goesOnceRead() {
        notice.raise()
        notice.dismiss()

        assertFalse(notice.pending.value)
        assertFalse(AccountErasedNotice(preferences).pending.value)
    }
}
