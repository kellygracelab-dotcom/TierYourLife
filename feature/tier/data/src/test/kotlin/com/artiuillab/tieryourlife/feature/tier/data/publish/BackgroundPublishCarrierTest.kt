package com.artiuillab.tieryourlife.feature.tier.data.publish

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.repository.Carried
import com.artiuillab.tieryourlife.feature.community.domain.repository.CarryRequest
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.data.sync.Connection
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Designer, approved by Danylo 26.11.2026: Update goes on in the background, through a lost connection and a closed app. */
@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundPublishCarrierTest {

    @Test
    fun aLostConnection_isTriedAfter2_5And10Seconds_thenWhenOneComesBack() = runTest {
        val h = Harness(this)
        repeat(4) { h.community.answers += Result.failure(PublishRefused(PublishError.Offline)) }

        h.carrier.send(CarryRequest(h.boards.board, OWNER))
        runCurrent()
        assertEquals(1, h.community.sent.size)
        advanceTimeBy(2_001)
        assertEquals(2, h.community.sent.size)
        advanceTimeBy(5_001)
        assertEquals(3, h.community.sent.size)
        advanceTimeBy(10_001)
        assertEquals(4, h.community.sent.size)
        assertTrue(h.carrier.going.value.getValue(BOARD).offline)

        // No connection: nothing more goes, however long.
        advanceTimeBy(600_000)
        assertEquals(4, h.community.sent.size)

        h.connection.back.emit(Unit)
        advanceUntilIdle()
        assertEquals(5, h.community.sent.size)
        assertEquals(listOf(BOARD to "pub-1"), h.boards.published)
        assertTrue(h.ended.single() is Carried.Out)
        assertTrue(h.carrier.going.value.isEmpty())
        assertEquals(listOf(BOARD), h.backup.dropped.distinct())
    }

    @Test
    fun overAMinute_itSaysSo_andIsNoErrorYet() = runTest {
        val h = Harness(this)
        val answer = CompletableDeferred<Result<Published>>()
        h.community.waitFor = answer

        h.carrier.send(CarryRequest(h.boards.board, OWNER))
        advanceTimeBy(60_001)
        assertTrue(h.carrier.going.value.getValue(BOARD).slow)

        answer.complete(Result.success(Published("pub-1", "fp")))
        advanceUntilIdle()
        assertTrue(h.ended.single() is Carried.Out)
    }

    @Test
    fun anEditDuringAnUpdate_waitsForTheNextTap() = runTest {
        val h = Harness(this)
        val tapped = h.boards.board
        val answer = CompletableDeferred<Result<Published>>()
        h.community.waitFor = answer

        h.carrier.send(CarryRequest(tapped, OWNER))
        runCurrent()
        h.boards.board = tapped.copy(title = "Films, edited after the tap")
        answer.complete(Result.success(Published("pub-1", "fp")))
        advanceUntilIdle()

        assertEquals(listOf("Films"), h.community.sent.map { it.title })
        assertEquals("Films", (h.ended.single() as Carried.Out).sent.title)
    }

    @Test
    fun takeItDown_whileTheRequestIsOnItsWay_waitsForItsAnswer() = runTest {
        val h = Harness(this)
        val answer = CompletableDeferred<Result<Published>>()
        h.community.waitFor = answer
        h.carrier.send(CarryRequest(h.boards.board, OWNER))
        runCurrent()

        val stopped = launch { h.carrier.stop(BOARD) }
        runCurrent()
        assertFalse(stopped.isCompleted)

        answer.complete(Result.success(Published("pub-1", "fp")))
        advanceUntilIdle()
        assertTrue(stopped.isCompleted)
        assertEquals(1, h.community.sent.size)
    }

    @Test
    fun takeItDown_whileWaitingToTryAgain_sendsNothingMore() = runTest {
        val h = Harness(this)
        h.community.answers += Result.failure(PublishRefused(PublishError.Offline))
        h.carrier.send(CarryRequest(h.boards.board, OWNER))
        runCurrent()

        h.carrier.stop(BOARD)
        advanceUntilIdle()

        assertEquals(1, h.community.sent.size)
        assertTrue(h.ended.isEmpty())
        assertTrue(h.carrier.going.value.isEmpty())
        assertEquals(listOf(BOARD), h.backup.dropped.distinct())
    }

    @Test
    fun aRefusal_endsIt_andNothingIsWritten() = runTest {
        val h = Harness(this)
        h.community.answers += Result.failure(PublishRefused(PublishError.ListGone))

        h.carrier.send(CarryRequest(h.boards.board, OWNER))
        advanceUntilIdle()

        assertEquals(Carried.Refused(BOARD, update = true, reason = PublishError.ListGone), h.ended.single())
        assertTrue(h.boards.published.isEmpty())
    }

    @Test
    fun theAppClosedOnIt_theBackupFinishesIt_fromTheBoardOnThisPhone() = runTest {
        val before = Harness(this)
        before.community.waitFor = CompletableDeferred()
        before.carrier.send(CarryRequest(before.boards.board, OWNER))
        runCurrent()
        val note = before.backup.kept.single()
        before.life.cancel()

        // A new process: nothing of the old one but the board on this phone and the note.
        val after = Harness(this, before.boards)
        after.carrier.resume(note)
        advanceUntilIdle()

        assertEquals(listOf("Films"), after.community.sent.map { it.title })
        assertEquals(listOf(BOARD to "pub-1"), after.boards.published)
    }

    @Test
    fun theAppClosedOnIt_andTheBoardChangedSince_waitsForTheNextTap() = runTest {
        val before = Harness(this)
        before.community.waitFor = CompletableDeferred()
        before.carrier.send(CarryRequest(before.boards.board, OWNER))
        runCurrent()
        val note = before.backup.kept.single()
        before.life.cancel()
        before.boards.board = before.boards.board.copy(title = "Films, edited after the tap")

        val after = Harness(this, before.boards)
        after.carrier.resume(note)
        advanceUntilIdle()

        assertTrue(after.community.sent.isEmpty())
        assertEquals(listOf(BOARD), after.backup.dropped)
    }

    @Test
    fun anotherAccountSignedIn_nothingGoes() = runTest {
        val h = Harness(this)
        h.accounts.accounts.value = Account.Guest

        h.carrier.send(CarryRequest(h.boards.board, OWNER))
        advanceUntilIdle()

        assertTrue(h.community.sent.isEmpty())
        assertTrue(h.carrier.going.value.isEmpty())
    }

    @Test
    fun aNote_comesBackFromWorkData_asItWent() {
        val note = BackupNote(BOARD, OWNER, "fp", originVersion = 10, categoryIfNone = "other")
        assertEquals(note, note.toData().toNote())
        assertEquals(note.copy(originVersion = null, categoryIfNone = null), note.copy(originVersion = null, categoryIfNone = null).toData().toNote())
    }

    private class Harness(scope: TestScope, val boards: Boards = Boards()) {
        /** The app's process: ended, it takes everything in memory with it. */
        val life = Job()
        val community = Community()
        val accounts = FakeAccountRepository(FakeAccountRepository.signedIn)
        val connection = Reconnecting()
        val backup = Backup()
        val ended = mutableListOf<Carried>()
        val carrier = BackgroundPublishCarrier(
            boards, community, accounts, FakeAppPreferences(), NoPictures, connection, backup, CoroutineScope(scope.coroutineContext + life),
            BackgroundPublishCarrier.Pauses(),
        )

        init {
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { carrier.ended.collect { ended += it } }
        }
    }

    private class Boards(var board: TierList = films()) : TierRepository by RecordingTierRepository() {
        val published = mutableListOf<Pair<Long, String?>>()
        override suspend fun getTierListById(id: Long): TierList? = board.takeIf { it.id == id }
        override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) {
            published += id to publishedId
        }
    }

    private class Community : FakeCommunityRepository() {
        val sent = mutableListOf<TierList>()
        val answers = ArrayDeque<Result<Published>>()
        var waitFor: CompletableDeferred<Result<Published>>? = null
        override suspend fun publishOwned(list: TierList, expectedOwnerUid: String): Result<Published> {
            sent += list
            waitFor?.let { return it.await() }
            return answers.removeFirstOrNull() ?: Result.success(Published("pub-1", "fp"))
        }
    }

    private class Reconnecting : Connection {
        val back = MutableSharedFlow<Unit>()
        override val available: Flow<Unit> = back
        override fun unmetered(): Boolean = true
    }

    private class Backup : CarryBackup {
        val kept = mutableListOf<BackupNote>()
        val dropped = mutableListOf<Long>()
        override fun keep(note: BackupNote) {
            kept += note
        }
        override fun drop(boardId: Long) {
            dropped += boardId
        }
    }

    private object NoPictures : OwnPictures {
        override fun pictureIdOf(imageUrl: String?): String? = null
        override suspend fun sendNow(pictureIds: List<String>): Set<String> = emptySet()
    }

    private companion object {
        const val BOARD = 7L
        val OWNER = (FakeAccountRepository.signedIn as Account.SignedIn).recordKey

        fun films() = TierList(
            id = BOARD,
            title = "Films",
            publishedId = "pub-1",
            tiers = listOf(Tier(1, "S", "#111111", "#EEEEEE", listOf(TierItem(id = 1, title = "Alien", imageUrl = null)))),
        )
    }
}
