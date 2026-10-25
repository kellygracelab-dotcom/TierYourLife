package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.BoardConflictDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardIndexDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardRevisionDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardSummaryDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptTierDto
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException

/** What a sync run does with the steps the plan decides. */
@RunWith(AndroidJUnit4::class)
class BoardSyncEngineTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: TierDatabase
    private lateinit var dao: BoardSyncDao
    private lateinit var images: TierImageStore
    private val boards = FakeBoards()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TierDatabase::class.java).build()
        dao = database.boardSyncDao()
        images = TierImageStore(directory = folder.root, openSource = { null })
    }

    @After
    fun tearDown() = database.close()

    private fun engine(
        account: Account = FakeAccountRepository.signedIn,
        backUp: Boolean = true,
    ): BoardSyncEngine {
        val preferences = FakeAppPreferences(backUpBoards = backUp)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return BoardSyncEngine(
            dao = dao,
            api = boards,
            accounts = FakeAccountRepository(initial = account),
            json = networkJson,
            images = images,
            pictures = PictureSync(dao, images, NoPictures, preferences, Metered),
            preferences = preferences,
            deviceName = DeviceName(context),
            community = FakeCommunityRepository(),
        )
    }

    @Test
    fun aGuest_sendsNothing() = runBlocking {
        givenLocalBoard("Films")

        val report = engine(account = Account.Guest).sync()

        assertFalse(report.signedIn)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun withBackupOff_nothingIsSent() = runBlocking {
        givenLocalBoard("Films")

        val report = engine(backUp = false).sync()

        assertFalse(report.signedIn)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun aNewBoard_goesUpOnce_andIsRemembered() = runBlocking {
        val uid = givenLocalBoard("Films")

        val first = engine().sync()
        engine().sync()

        assertEquals(1, first.carried)
        assertEquals(listOf(uid), boards.kept.map { it.first })
        assertNull(boards.kept.single().second.basedOn)
        assertEquals(1, dao.allSyncRecords().single { it.listUid == uid }.revision)
    }

    @Test
    fun aBoardFromAnotherPhone_arrivesWithItsTiersAndCards() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)

        engine().sync()

        val arrived = dao.boardByUid("remote-1")!!
        assertEquals("Games", arrived.title)
        assertEquals(listOf("S"), dao.tiersOf(arrived.id).map { it.label })
        assertEquals(listOf("Doom"), dao.itemsOf(arrived.id).map { it.title })
        assertEquals(4, dao.allSyncRecords().single { it.listUid == "remote-1" }.revision)
    }

    // The answer to our last write never arrived. Taking our own board back
    // as a copy would leave a duplicate every time the app is killed mid-sync.
    @Test
    fun aRefusalCarryingOurOwnBoard_takesTheMissedRevisionAndMakesNoCopy() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseNextWith = theirBoard(uid, title = "Films", revision = 7, fingerprint = fingerprintOf(uid))

        engine().sync()

        assertEquals(1, dao.allBoards().size)
        assertEquals(7, dao.allSyncRecords().single { it.listUid == uid }.revision)
    }

    @Test
    fun aRefusalCarryingADifferentBoard_keepsBothAndSendsOursOnTop() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseNextWith = theirBoard(uid, title = "Films (tablet)", revision = 3, deviceName = "Tablet")

        engine().sync()

        val copy = dao.allBoards().single { it.uid != uid }
        assertEquals("Films (tablet)", copy.title)
        assertEquals("Tablet", copy.arrivedFrom)
        assertEquals(uid, boards.kept.last().first)
        assertEquals(3, boards.kept.last().second.basedOn)
    }

    // A board the account keeps turning down is not backed up, and saying it
    // was would hide the stuck banner from the one person who needs it.
    @Test
    fun aWriteRefusedForAnotherReason_isCountedAsRefused_notCarried() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseWithCode = 413

        val report = engine().sync()

        assertEquals(1, report.refused)
        assertEquals(0, report.carried)
        assertTrue(dao.allSyncRecords().none { it.listUid == uid })
    }

    @Test
    fun aStepThatFails_isCountedAndTheRunCarriesOn() = runBlocking {
        givenLocalBoard("Films")
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 1)
        boards.keepFails = true

        val report = engine().sync()

        assertEquals(1, report.refused)
        assertEquals(1, report.carried)
        assertNotNull(dao.boardByUid("remote-1"))
    }

    private suspend fun givenLocalBoard(title: String): String {
        val board = TierListEntity(title = title)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = listOf(TierItemEntity(tierId = 0, position = 0, title = "Alien", imageUrl = null)),
                ),
            ),
        )
        return board.uid
    }

    private suspend fun fingerprintOf(uid: String): String {
        val board = dao.boardByUid(uid)!!
        return BoardFingerprint.of(board, dao.tiersOf(board.id), dao.itemsOf(board.id), images::pictureIdOf)
    }

    private fun theirBoard(
        uid: String,
        title: String,
        revision: Int,
        fingerprint: String? = "theirs",
        deviceName: String? = null,
    ) = KeptBoardDto(
        uid = uid,
        revision = revision,
        fingerprint = fingerprint,
        deviceName = deviceName,
        title = title,
        tiers = listOf(KeptTierDto(uid = "$uid-tier", position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE")),
        items = listOf(KeptItemDto(uid = "$uid-item", tierUid = "$uid-tier", position = 0, title = "Doom")),
    )

    private class FakeBoards : BoardsApi {
        val server = mutableMapOf<String, KeptBoardDto>()
        val kept = mutableListOf<Pair<String, KeepBoardRequestDto>>()
        var refuseNextWith: KeptBoardDto? = null
        var keepFails = false
        var refuseWithCode: Int? = null

        override suspend fun index(after: String?) = KeptBoardIndexDto(
            server.values.map { KeptBoardSummaryDto(it.uid, it.revision, it.deleted, it.fingerprint) },
        )

        override suspend fun board(uid: String): KeptBoardDto = server.getValue(uid)

        override suspend fun keep(uid: String, request: KeepBoardRequestDto): Response<KeptBoardRevisionDto> {
            if (keepFails) throw IOException("offline")
            refuseWithCode?.let { code -> return Response.error(code, "".toResponseBody("application/json".toMediaType())) }
            refuseNextWith?.let { theirs ->
                refuseNextWith = null
                val body = networkJson.encodeToString(BoardConflictDto.serializer(), BoardConflictDto(board = theirs))
                return Response.error(HTTP_CONFLICT, body.toResponseBody("application/json".toMediaType()))
            }
            kept += uid to request
            val revision = (server[uid]?.revision ?: 0) + 1
            server[uid] = KeptBoardDto(uid = uid, revision = revision, fingerprint = request.fingerprint, title = request.title)
            return Response.success(KeptBoardRevisionDto(uid, revision))
        }

        override suspend fun forget(uid: String) {
            server.remove(uid)
        }
    }

    private object NoPictures : Pictures {
        override suspend fun put(pictureId: String, bytes: ByteArray) = true

        override suspend fun get(pictureId: String): ByteArray? = null
    }

    /** Pictures wait for Wi-Fi, which keeps them out of a test about boards. */
    private object Metered : Connection {
        override val available = emptyFlow<Unit>()

        override fun unmetered() = false
    }

    private companion object {
        const val HTTP_CONFLICT = 409
    }
}
