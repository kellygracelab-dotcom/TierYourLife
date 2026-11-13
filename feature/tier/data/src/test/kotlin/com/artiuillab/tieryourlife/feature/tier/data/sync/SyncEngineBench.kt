package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
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
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.IOException

/** A real Room, a fake account and the engine between them, for the sync tests. */
abstract class SyncEngineBench {

    @get:Rule
    val folder = TemporaryFolder()

    protected lateinit var database: TierDatabase
    protected lateinit var dao: BoardSyncDao
    protected lateinit var images: TierImageStore
    protected val boards = FakeBoards()
    protected val lock = SyncLock()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TierDatabase::class.java).build()
        dao = database.boardSyncDao()
        images = TierImageStore(directory = folder.root, openSource = { null })
    }

    @After
    fun tearDown() = database.close()

    protected fun engine(
        account: Account = FakeAccountRepository.signedIn,
        backUp: Boolean = true,
        preferences: FakeAppPreferences = FakeAppPreferences(backUpBoards = backUp),
        community: CommunityRepository = FakeCommunityRepository(),
        api: BoardsApi = boards,
        accounts: FakeAccountRepository = FakeAccountRepository(initial = account),
        vault: Pictures = NoPictures,
        connection: Connection = Metered,
        ownership: BoardOwnership = BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences),
    ): BoardSyncEngine {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return BoardSyncEngine(
            dao = dao,
            api = api,
            accounts = accounts,
            json = networkJson,
            images = images,
            pictures = PictureSync(dao, images, vault, preferences, connection),
            preferences = preferences,
            deviceName = DeviceName(context),
            community = community,
            ownership = ownership,
            lock = lock,
        )
    }

    protected suspend fun throwAway(uid: String, signedInAs: String) {
        val tiers = database.tierDao()
        tiers.markTierListsDeleted(listOf(dao.boardByUid(uid)!!.id), deletedAt = 1)
        tiers.emptyTrash(owner = signedInAs)
    }

    protected class Publishing(private vararg val ids: String) : FakeCommunityRepository() {
        override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(
            ids.map { PublishedListSummary(id = it, title = it, authorUid = "", authorName = "", category = "other", itemCount = 1, updatedAtMillis = 0) },
        )
    }

    protected suspend fun givenLocalBoard(
        title: String,
        publishedId: String? = null,
        credit: String? = null,
        owner: String? = someone,
        attribution: PictureAttribution? = null,
        itemTitle: String = "Alien",
    ): String {
        val board = TierListEntity(title = title, publishedId = publishedId, ownerUid = owner)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = listOf(TierItemEntity(tierId = 0, position = 0, title = itemTitle, imageUrl = null, credit = credit, attribution = attribution)),
                ),
            ),
        )
        return board.uid
    }

    protected suspend fun fingerprintOf(uid: String): String {
        val board = dao.boardByUid(uid)!!
        return BoardFingerprint.of(board, dao.tiersOf(board.id), dao.itemsOf(board.id), images::pictureIdOf)
    }

    protected fun theirBoard(
        uid: String,
        title: String,
        revision: Int,
        fingerprint: String? = "theirs",
        deviceName: String? = null,
        pictureId: String? = null,
    ) = KeptBoardDto(
        uid = uid,
        revision = revision,
        fingerprint = fingerprint,
        deviceName = deviceName,
        title = title,
        tiers = listOf(KeptTierDto(uid = "$uid-tier", position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE")),
        items = listOf(KeptItemDto(uid = "$uid-item", pictureId = pictureId, tierUid = "$uid-tier", position = 0, title = "Doom")),
    )

    protected class FakeBoards : BoardsApi {
        val server = mutableMapOf<String, KeptBoardDto>()
        val kept = mutableListOf<Pair<String, KeepBoardRequestDto>>()
        var refuseNextWith: KeptBoardDto? = null
        var thenRefuseWithCode: Int? = null
        var keepFails = false
        var refuseWithCode: Int? = null

        /** Refused as too large, these alone. */
        var refuseUids: Set<String> = emptySet()
        var refusalBody = ""
        var onKeep: () -> Unit = {}

        /** As the proxy's shared copies: catalogue address to ours, named back for the ones a write shows. */
        var copies: Map<String, String> = emptyMap()
        var onIndex: suspend () -> Unit = {}
        var onBoard: suspend (String) -> Unit = {}

        override suspend fun index(after: String?): KeptBoardIndexDto {
            onIndex()
            val all = server.values.map { KeptBoardSummaryDto(it.uid, it.revision, it.deleted, it.fingerprint, title = it.title) }
            val from = after?.let { last -> all.indexOfFirst { it.uid == last } + 1 } ?: 0
            val page = all.drop(from).take(pageSize)
            return KeptBoardIndexDto(page, next = page.lastOrNull()?.uid?.takeIf { from + page.size < all.size })
        }

        /** The proxy's is 100. */
        var pageSize = Int.MAX_VALUE

        override suspend fun board(uid: String): KeptBoardDto {
            onBoard(uid)
            return server.getValue(uid)
        }

        override suspend fun keep(uid: String, request: KeepBoardRequestDto): Response<KeptBoardRevisionDto> {
            if (keepFails) throw IOException("offline")
            refuseWithCode?.let { code -> return Response.error(code, refusalBody.toResponseBody("application/json".toMediaType())) }
            if (uid in refuseUids) return Response.error(HTTP_TOO_LARGE, refusalBody.toResponseBody("application/json".toMediaType()))
            refuseNextWith?.let { theirs ->
                refuseNextWith = null
                refuseWithCode = thenRefuseWithCode
                val body = networkJson.encodeToString(BoardConflictDto.serializer(), BoardConflictDto(board = theirs))
                return Response.error(HTTP_CONFLICT, body.toResponseBody("application/json".toMediaType()))
            }
            kept += uid to request
            onKeep()
            val revision = (server[uid]?.revision ?: 0) + 1
            server[uid] = KeptBoardDto(uid = uid, revision = revision, fingerprint = request.fingerprint, title = request.title)
            val shown = request.items.mapNotNull { it.imageUrl } + listOfNotNull(request.coverImageUrl) + request.coverImageUrls
            return Response.success(KeptBoardRevisionDto(uid, revision, copies.filterKeys { it in shown }))
        }

        // As the proxy: a marker, not nothing, so the other phone hears of it.
        override suspend fun forget(uid: String) {
            server[uid]?.let { stored -> server[uid] = stored.copy(revision = stored.revision + 1, deleted = true) }
        }
    }

    protected object NoPictures : Pictures {
        override suspend fun put(pictureId: String, bytes: ByteArray) = true

        override suspend fun get(pictureId: String): ByteArray? = null
    }

    protected class Holding(private val pictureId: String) : Pictures {
        override suspend fun put(pictureId: String, bytes: ByteArray) = true

        override suspend fun get(pictureId: String): ByteArray? = byteArrayOf(1, 2, 3).takeIf { pictureId == this.pictureId }
    }

    /** Pictures wait for Wi-Fi, which keeps them out of a test about boards. */
    protected object Metered : Connection {
        override val available = emptyFlow<Unit>()

        override fun unmetered() = false
    }

    protected object Unmetered : Connection {
        override val available = emptyFlow<Unit>()

        override fun unmetered() = true
    }

    protected companion object {
        const val HTTP_CONFLICT = 409
        const val HTTP_TOO_LARGE = 413
        const val OTHER_RUN_MS = 1_000L
        val someone = (FakeAccountRepository.signedIn as Account.SignedIn).recordKey
        val ann = Account.SignedIn(email = "ann@example.com", photoUrl = null, uid = "ann")
        val bob = Account.SignedIn(email = "bob@example.com", photoUrl = null, uid = "bob")
    }
}
