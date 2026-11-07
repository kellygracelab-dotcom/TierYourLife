package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.PersonalData
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** A1: the boards of a deleted account sat under a uid nobody signs into again, and so did those of one whose answer never came. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccountErasureTest : SyncEngineBench() {

    private val preferences = FakeAppPreferences()
    private val accounts = FakeAccountRepository(initial = ann)
    private var now = 1_000L
    private val signOut: suspend () -> Unit = { accounts.signOut() }

    private fun merge() = RoomBoardMerge(
        dao,
        database.ownerDao(),
        boards,
        BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences),
        lock,
        preferences,
        accounts,
        ThePerson(preferences, setOf(PersonalData { forgotten++ })),
    ) { now }

    private var forgotten = 0

    private suspend fun ownerOf(uid: String) = dao.boardByUid(uid)!!.ownerUid

    private val noAnswer: suspend () -> Result<Unit> = { Result.failure(IOException("timeout")) }

    @Test
    fun deletingTheAccount_keepsItsBoardsHere_asBoardsWithoutAnAccount() = runBlocking {
        val films = givenLocalBoard("Films", publishedId = "pub-1", owner = "ann")
        val ranking = givenLocalBoard("My take on Bob's games", owner = "ann")
        database.tierDao().setRankingOf(dao.boardByUid(ranking)!!.id, "bobs-games")
        val others = givenLocalBoard("Bob's games", owner = "bob")
        engine(account = ann).sync()

        val result = merge().eraseKeepingBoards("ann", erase = { Result.success(Unit) }, signOut = signOut)

        assertTrue(result.isSuccess)
        assertNull(ownerOf(films))
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertNull("its rankings went with it", dao.boardByUid(ranking)!!.rankingOf)
        assertEquals("bob", ownerOf(others))
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        assertEquals(Account.Guest, accounts.current())
        assertNull(preferences.erasureUnderWay())
        val repository = RoomTierRepository(database.tierDao(), images, BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences))
        assertEquals(listOf("Films", "My take on Bob's games"), repository.getAllTierLists().map { it.title }.sorted())
    }

    @Test
    fun anAccountTheServerRefusedToDelete_keepsItsBoards() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        val refused = HttpException(Response.error<Unit>(403, "".toResponseBody()))

        val result = merge().eraseKeepingBoards("ann", erase = { Result.failure(refused) }, signOut = signOut)

        assertTrue(result.isFailure)
        assertEquals("ann", ownerOf(films))
        assertEquals(ann, accounts.current())
        assertNull(preferences.erasureUnderWay())
    }

    // Deleted on the server, the answer lost on the way back.
    @Test
    fun noAnswer_fromAnAccountThatIsGone_keepsItsBoards() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        accounts.exists = false

        val result = merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)

        assertTrue(result.isSuccess)
        assertNull(ownerOf(films))
        assertEquals(Account.Guest, accounts.current())
    }

    @Test
    fun noAnswer_fromAnAccountStillThere_waitsForALaterStart() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")

        assertTrue(merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut).isFailure)
        merge().settleUnansweredErase()
        assertEquals("still there: a re-sign-in would bring them down twice", "ann", ownerOf(films))
        assertNotNull(preferences.erasureUnderWay())

        accounts.exists = false
        merge().settleUnansweredErase()

        assertNull(ownerOf(films))
        assertEquals(Account.Guest, accounts.current())
        assertNull(preferences.erasureUnderWay())
        assertEquals("S22: forgotten as on any other way out", 1, forgotten)
    }

    @Test
    fun anEraseThatNeverHappened_isForgottenOnceItCouldNoLongerBeRunning() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)

        now += ERASE_ANSWERS_WITHIN_MS + 1
        merge().settleUnansweredErase()

        assertNull(preferences.erasureUnderWay())
        assertEquals("ann", ownerOf(films))
        assertEquals(ann, accounts.current())
    }

    // The phone was killed before it heard back; by the next start, Firebase
    // had found the user gone and signed it out.
    @Test
    fun signedOutByTheAccountGoing_keepsItsBoardsAtTheNextStart() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        preferences.setLastSignedIn("ann")
        accounts.exists = null
        merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)
        accounts.accounts.value = Account.Guest

        merge().settleUnansweredErase()

        assertNull(ownerOf(films))
        assertEquals(1, forgotten)
    }

    // A40: what is on the phone by then is whoever came after; only the erased account's boards are settled.
    @Test
    fun anEraseSettledAfterAnotherAccountSignedIn_forgetsNothingOfThem() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        accounts.exists = null
        merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)
        preferences.setLastSignedIn("bob")
        preferences.hideList("list-1", "Hidden by Bob")
        accounts.accounts.value = bob

        merge().settleUnansweredErase()

        assertNull(ownerOf(films))
        assertNull(preferences.erasureUnderWay())
        assertEquals(0, forgotten)
        assertEquals(1, preferences.hiddenLists().size)
    }

    @Test
    fun anEraseSettledAfterAGuestCarriedOn_forgetsNothingOfThem() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        accounts.exists = null
        merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)
        preferences.setLastSignedIn(null)
        accounts.accounts.value = Account.Guest

        merge().settleUnansweredErase()

        assertNull(ownerOf(films))
        assertEquals(0, forgotten)
    }

    @Test
    fun signingOutByChoice_withAnEraseUnanswered_leavesALiveAccountItsBoards() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)

        merge().signOutSettlingErase(signOut)
        merge().settleUnansweredErase()

        assertEquals(Account.Guest, accounts.current())
        assertEquals("ann", ownerOf(films))
        assertNull(preferences.erasureUnderWay())
    }

    @Test
    fun signingOutByChoice_afterTheAccountWent_keepsItsBoards() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        accounts.exists = null
        merge().eraseKeepingBoards("ann", erase = noAnswer, signOut = signOut)

        accounts.exists = false
        merge().signOutSettlingErase(signOut)

        assertNull(ownerOf(films))
        assertEquals(Account.Guest, accounts.current())
    }

    // A32: signing out sat behind the whole run, several seconds with nothing said.
    @Test
    fun signingOutDuringARun_stopsItAtTheNextStep() = runBlocking {
        givenLocalBoard("Films", owner = "ann")
        givenLocalBoard("Games", owner = "ann")
        val engine = engine(preferences = preferences, accounts = accounts)
        var leaving: Deferred<*>? = null
        boards.onIndex = {
            boards.onIndex = {}
            leaving = async { merge().signOutSettlingErase(signOut) }
            yield()
        }

        val report = engine.sync()
        leaving!!.await()

        assertTrue("nothing sent once the sign-out asked", boards.kept.isEmpty())
        assertEquals(2, report.refused)
        assertEquals(Account.Guest, accounts.current())
    }

    // A sync waiting on the erase ran next, still as the deleted account.
    @Test
    fun aSyncWaitingOnTheErase_doesNotRunAsTheDeletedAccount() = runBlocking {
        givenLocalBoard("Films", owner = "ann")
        val engine = engine(preferences = preferences, accounts = accounts)
        val scope = this
        var waiting: Deferred<*>? = null

        merge().eraseKeepingBoards(
            "ann",
            erase = {
                waiting = scope.async { engine.sync() }
                yield()
                Result.success(Unit)
            },
            signOut = signOut,
        )
        waiting!!.await()

        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun anAccountThatWasNotDeleted_isNotSettledAsIfItWere() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")

        merge().settleUnansweredErase()

        assertEquals("ann", ownerOf(films))
        assertEquals(ann, accounts.current())
    }

    @Test
    fun aMark_readsBackAsWritten() {
        assertEquals(ErasureMark("ann", 42), erasureMarkOf(ErasureMark("ann", 42).encoded()))
        assertNull(erasureMarkOf(null))
        assertNull(erasureMarkOf("garbled"))
    }
}
