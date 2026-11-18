package com.artiuillab.tieryourlife.feature.community.data.sync

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.data.cache.DraftRecord
import com.artiuillab.tieryourlife.feature.community.data.cache.FileRankingDrafts
import com.artiuillab.tieryourlife.feature.community.data.remote.KeepStartedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.KeptStartedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedApi
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedConflictDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedIndexDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.StartedRemoval
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedSyncOutcome
import com.artiuillab.tieryourlife.feature.community.domain.repository.startedGameKey
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartedListsSyncTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = FakeStartedServer()

    /** Stopped: each test runs its rounds itself, with nothing going on behind it. */
    private val stopped = CoroutineScope(Job().apply { cancel() })
    private val live = CoroutineScope(Job())
    private val ann = Account.SignedIn(email = null, photoUrl = null, uid = "ann")

    @After
    fun stop() = live.cancel()

    private inner class Device(name: String, account: Account = ann, scope: CoroutineScope = stopped) {
        var clock = 0L
        val drafts = FileRankingDrafts(File(folder.root, name)) { clock }
        val games = FakeSavedGames()
        val accounts = FakeAccountRepository(account)
        val sync = StartedListsSync(server, drafts, games, accounts, emptyFlow(), scope)

        suspend fun keep(at: Long, draft: RankingDraft, owner: String? = "ann") {
            clock = at
            drafts.keep(LIST, draft, owner)
        }

        suspend fun read(owner: String? = "ann") = drafts.read(LIST, owner)
    }

    @Test
    fun aListBegunHere_goesUpWithItsGame_andComesDownOnAnotherDevice() = runBlocking {
        val phone = Device("phone")
        phone.keep(1_000, HALF)
        phone.games.save(GAME_KEY, game())

        // Sent, with nothing here changed by it.
        assertEquals(StartedSyncOutcome(), phone.sync.sync())

        val kept = server.lists.getValue(LIST)
        assertEquals(HALF.rows, kept.rows)
        assertEquals(HALF.pool, kept.poolOrder)
        assertEquals(4, kept.itemCount)
        assertEquals(2, kept.tierCount)
        assertEquals("hill", kept.play?.jsonObject?.get("kind")?.jsonPrimitive?.content)
        assertNull(server.lastKeep?.basedOn)
        assertEquals(StartedSyncOutcome(), phone.sync.sync())

        val tablet = Device("tablet")
        assertEquals(StartedSyncOutcome(changed = true, replaced = 0), tablet.sync.sync())
        assertEquals(HALF, tablet.read())
        assertEquals(listOf("list"), tablet.drafts.started("ann").map { it.listId })
        val game = checkNotNull(tablet.games.load(GAME_KEY))
        assertEquals(listOf(0L, 1L, 2L, 3L), game.ids)
        assertFalse("someone else's list keeps its game in the file", game.onBoard)
    }

    @Test
    fun aDiscard_takesItOutEverywhere_andItsUndo_bringsItBack() = runBlocking {
        val phone = Device("phone")
        val tablet = Device("tablet")
        phone.keep(1_000, HALF)
        phone.games.save(GAME_KEY, game())
        phone.sync.sync()
        tablet.sync.sync()

        phone.keep(2_000, HALF.copy(rows = listOf(emptyList(), emptyList()), pool = listOf(0, 1, 2, 3)))
        phone.games.discard(GAME_KEY)
        phone.sync.sync()
        assertEquals(StartedRemoval.DISCARD, server.lists.getValue(LIST).removed)
        val tombstone = server.lists.getValue(LIST).updatedAt

        // Taken out, not replaced: nothing to tell.
        assertEquals(StartedSyncOutcome(changed = true, replaced = 0), tablet.sync.sync())
        assertTrue(tablet.drafts.started("ann").isEmpty())
        assertNull(tablet.games.load(GAME_KEY))

        phone.keep(3_000, HALF)
        phone.sync.sync()
        assertEquals(tombstone, server.lastKeep?.basedOn)
        assertNull(server.lists.getValue(LIST).removed)

        tablet.sync.sync()
        assertEquals(HALF, tablet.read())
        assertEquals(1, tablet.drafts.started("ann").size)
    }

    @Test
    fun anEarlierChangeGivesWay_toALaterOneFromAnotherDevice_andSaysSo() = runBlocking {
        val phone = Device("phone")
        val tablet = Device("tablet")
        phone.keep(1_000, HALF)
        phone.sync.sync()
        tablet.sync.sync()

        val later = HALF.copy(rows = listOf(listOf(3), listOf(2)), pool = listOf(0, 1))
        tablet.keep(3_000, later)
        tablet.sync.sync()
        phone.keep(2_000, HALF.copy(rows = listOf(listOf(0, 1, 2), emptyList()), pool = listOf(3)))

        assertEquals(StartedSyncOutcome(changed = true, replaced = 1), phone.sync.sync())
        assertEquals(later, phone.read())

        // Replaced without a write of its own: the next round finds it and says so too.
        val latest = HALF.copy(rows = listOf(listOf(3, 2), listOf(1)), pool = listOf(0))
        tablet.keep(4_000, latest)
        tablet.sync.sync()
        assertEquals(StartedSyncOutcome(changed = true, replaced = 1), phone.sync.sync())
        assertEquals(latest, phone.read())
    }

    @Test
    fun savedAndFinishedLists_leaveContinueWithTheirReason() = runBlocking {
        val phone = Device("phone")
        phone.keep(1_000, HALF)
        phone.sync.sync()
        phone.keep(2_000, HALF.copy(savedBoardUid = "board"))
        phone.sync.sync()
        assertEquals(StartedRemoval.SAVED, server.lists.getValue(LIST).removed)

        phone.keep(3_000, HALF)
        phone.sync.sync()
        phone.keep(4_000, HALF.copy(rows = listOf(listOf(0, 1), listOf(2, 3)), pool = emptyList()))
        phone.sync.sync()
        assertEquals(StartedRemoval.DONE, server.lists.getValue(LIST).removed)

        // Never in the account: there is nothing there to take out.
        val fresh = Device("fresh")
        fresh.clock = 5_000
        fresh.drafts.keep("other", HALF.copy(savedBoardUid = "board"), "ann")
        fresh.sync.sync()
        assertFalse("other" in server.lists)
    }

    @Test
    fun aGuestsLists_joinTheAccount_withoutOverwritingALaterCopyThere() = runBlocking {
        val tablet = Device("tablet")
        val kept = HALF.copy(rows = listOf(listOf(3), listOf(2)), pool = listOf(0, 1))
        tablet.keep(5_000, kept)
        tablet.sync.sync()

        val phone = Device("phone", Account.Guest)
        phone.keep(1_000, HALF, owner = null)
        phone.clock = 2_000
        phone.drafts.keep("own", HALF.copy(title = "Own"), null)
        assertEquals(StartedSyncOutcome(), phone.sync.sync())
        assertFalse("own" in server.lists)

        phone.accounts.accounts.value = ann
        phone.sync.sync()

        assertNull(server.lastKeepOf("own")?.basedOn)
        assertEquals(HALF.copy(title = "Own"), phone.drafts.read("own", "ann"))
        assertNull(phone.drafts.read("own", null))
        assertNull(phone.read(owner = null))
        // The account's copy changed later than the guest's: it stays, and the phone takes it.
        assertEquals(kept, phone.read())
        assertEquals(kept.rows, server.lists.getValue(LIST).rows)
    }

    @Test
    fun offline_whatChangedWaits_andGoesWithTheNextRound() = runBlocking {
        val phone = Device("phone")
        phone.keep(1_000, HALF)
        server.offline = true

        assertEquals(StartedSyncOutcome(), phone.sync.sync())
        assertTrue(server.lists.isEmpty())
        assertEquals(1, phone.drafts.started("ann").size)

        server.offline = false
        phone.sync.sync()
        assertEquals(HALF.rows, server.lists.getValue(LIST).rows)
    }

    @Test
    fun aServerWithoutTheRoutes_leavesEverythingHereAsItIs() = runBlocking {
        val phone = Device("phone")
        phone.keep(1_000, HALF)
        server.refuse = 404

        assertEquals(StartedSyncOutcome(), phone.sync.sync())
        assertEquals(1, phone.drafts.started("ann").size)
        assertTrue(checkNotNull(phone.drafts.record("ann", LIST)).dirty)
    }

    @Test
    fun aListTheAccountRefusesForItsSize_isNotSentAgainAndAgain() = runBlocking {
        val phone = Device("phone")
        phone.keep(1_000, HALF)
        server.refuse = 413

        phone.sync.sync()
        server.refuse = null
        phone.sync.sync()
        assertEquals(1, server.attempts)
        assertEquals(1, phone.drafts.started("ann").size)
    }

    @Test
    fun aRecordFromBeforeSync_readsAsNotYetInTheAccount_andGoesUp() = runBlocking {
        val old = """{"version":7,"rows":[[0],[1]],"pool":[2,3],"listId":"list","owner":"ann","title":"Films","keptAt":900}"""
        val record = Json { ignoreUnknownKeys = true }.decodeFromString<DraftRecord>(old)
        assertTrue(record.dirty)
        assertNull(record.updatedAt)
        assertEquals(900L, record.lastChange)
        assertNotNull(record.started("ann"))

        val phone = Device("phone")
        phone.drafts.write(record)
        phone.sync.sync()
        assertEquals(900L, server.lists.getValue(LIST).changedAt)
        assertEquals(listOf(listOf(0), listOf(1)), server.lists.getValue(LIST).rows)
    }

    @Test
    fun aKeepHere_isSentOnItsOwn_andTheRoundIsAnnounced() = runBlocking {
        val tablet = Device("tablet")
        tablet.keep(1_000, HALF)
        tablet.sync.sync()
        val phone = Device("phone", scope = live)
        val announced = async { withTimeout(10_000) { phone.sync.synced.first() } }
        yield()

        phone.sync.request()
        assertEquals(StartedSyncOutcome(changed = true, replaced = 0), announced.await())
        assertEquals(HALF, phone.read())
    }

    private fun game() = SavedGame(
        spec = GameSpec(GameKind.Hill, count = 4, seed = 7),
        ids = listOf(0, 1, 2, 3),
        history = listOf(Outcome(0, 1, 0)),
        round = 0,
        startedAt = System.currentTimeMillis(),
        scope = listOf(-1L),
    )

    private companion object {
        const val LIST = "list"
        val GAME_KEY = startedGameKey(LIST, 7)
        val HALF = RankingDraft(7, listOf(listOf(0), listOf(1)), listOf(2, 3), title = "Films", section = "movies")
    }
}

/** The started routes as the contract has them: the later change wins, a stale one is answered with what is there. */
private class FakeStartedServer : StartedApi {
    val lists = mutableMapOf<String, StartedListDto>()
    private val keeps = mutableMapOf<String, KeepStartedDto>()
    var lastKeep: KeepStartedDto? = null
    var offline = false
    var refuse: Int? = null
    var attempts = 0
    private var clock = 10_000L

    fun lastKeepOf(listId: String) = keeps[listId]

    override suspend fun index(since: Long?): StartedIndexDto {
        answer()
        val newer = lists.values.filter { it.updatedAt > (since ?: 0) }
        return StartedIndexDto(newer.filter { it.removed == null }, newer.filter { it.removed != null }, clock)
    }

    override suspend fun keep(listId: String, body: KeepStartedDto): Response<KeptStartedDto> {
        attempts++
        answer()?.let { return it }
        val there = lists[listId]
        if (there != null && body.basedOn != there.updatedAt && body.changedAt <= there.changedAt) return conflict(there)
        lastKeep = body
        keeps[listId] = body
        val kept = StartedListDto(
            listId = listId,
            updatedAt = ++clock,
            changedAt = body.changedAt,
            listVersion = body.listVersion,
            itemCount = body.itemCount,
            tierCount = body.tierCount,
            title = body.title,
            category = body.category,
            rows = body.rows,
            poolOrder = body.poolOrder,
            placed = body.rows.sumOf { it.size },
            play = body.play,
        )
        lists[listId] = kept
        return Response.success(KeptStartedDto(listId, kept.updatedAt, kept.placed))
    }

    override suspend fun remove(listId: String, reason: String, basedOn: Long?, changedAt: Long): Response<KeptStartedDto> {
        attempts++
        answer()?.let { return it }
        val there = lists[listId]
        if (there != null && basedOn != there.updatedAt && there.changedAt > changedAt) return conflict(there)
        val mark = StartedListDto(listId = listId, updatedAt = ++clock, changedAt = changedAt, removed = reason)
        lists[listId] = mark
        return Response.success(KeptStartedDto(listId, mark.updatedAt, removed = reason))
    }

    private fun answer(): Response<KeptStartedDto>? {
        if (offline) throw IOException("offline")
        val code = refuse ?: return null
        val refused = Response.error<KeptStartedDto>(code, """{"code":"REFUSED"}""".toResponseBody())
        if (code != TOO_LARGE) throw retrofit2.HttpException(refused)
        return refused
    }

    private fun conflict(there: StartedListDto): Response<KeptStartedDto> =
        Response.error(409, networkJson.encodeToString(StartedConflictDto("CONFLICT", there)).toResponseBody())

    private companion object {
        const val TOO_LARGE = 413
    }
}
