package com.artiuillab.tieryourlife.e2e

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.artiuillab.tieryourlife.core.network.AppCheckInterceptor
import com.artiuillab.tieryourlife.core.network.IdTokenInterceptor
import com.artiuillab.tieryourlife.core.network.ProxyModule
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.data.di.CommunityModule
import com.artiuillab.tieryourlife.feature.community.data.repository.RetrofitCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.data.di.NetworkModule
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.data.sync.BoardSyncEngine
import com.artiuillab.tieryourlife.feature.tier.data.sync.Connection
import com.artiuillab.tieryourlife.feature.tier.data.sync.DeviceName
import com.artiuillab.tieryourlife.feature.tier.data.sync.OwnPicturesOnThisPhone
import com.artiuillab.tieryourlife.feature.tier.data.sync.PictureSync
import com.artiuillab.tieryourlife.feature.tier.data.sync.Pictures
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.Interceptor
import okhttp3.Response
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/** One call as it went over the wire. */
data class Answered(val method: String, val path: String, val status: Int, val body: String)

/**
 * One install of the app, wired as its Hilt modules wire it but over the
 * emulators' address. Only what Firebase would hand over is swapped: no App
 * Check token, and the ID token comes from the Auth emulator. Pictures stay
 * on the phone: the Storage part of sync is the Firebase SDK's, not ours.
 */
class Phone(identity: Identity) : Closeable {

    private val context: Context = ApplicationProvider.getApplicationContext()

    val answered = CopyOnWriteArrayList<Answered>()

    /** Runs after each answer arrives, before the app reads it: how a test makes another phone act mid-run. */
    @Volatile
    var afterAnswer: (Answered) -> Unit = {}

    private val database = Room.inMemoryDatabaseBuilder(context, TierDatabase::class.java).build()
    private val preferences = FakeAppPreferences()
    private val accounts = FakeAccountRepository(
        initial = Account.SignedIn(email = identity.email, photoUrl = null, displayName = "E2E", uid = identity.uid),
    )

    private val client = ProxyModule.provideProxyOkHttpClient(
        appCheckInterceptor = AppCheckInterceptor { null },
        idTokenInterceptor = IdTokenInterceptor { identity.idToken },
    ).newBuilder().addInterceptor(::record).build()
    private val retrofit = ProxyModule.proxyRetrofit(Emulators.functionsUrl, client)

    val boardsApi = NetworkModule.provideBoardsApi(retrofit)
    private val images = TierImageStore(context)
    private val ownership = BoardOwnership(accounts, database.ownerDao(), preferences)
    private val pictures = PictureSync(database.boardSyncDao(), images, PicturesStayHere, preferences, Metered)

    val boards = RoomTierRepository(database.tierDao(), images, ownership)
    val community = RetrofitCommunityRepository(
        api = CommunityModule.provideCommunityApi(retrofit),
        pictures = OwnPicturesOnThisPhone(images, pictures, accounts),
        auth = firebaseAuth(context),
    )
    val sync = BoardSyncEngine(
        dao = database.boardSyncDao(),
        api = boardsApi,
        accounts = accounts,
        json = networkJson,
        images = images,
        pictures = pictures,
        preferences = preferences,
        deviceName = DeviceName(context),
        community = community,
        ownership = ownership,
    )

    /** What the board screen's switch does: publish, then write the answer onto the board. */
    suspend fun publish(boardId: Long): Published {
        val board = board(boardId)
        val published = community.publish(board).getOrThrow()
        boards.setPublished(boardId, published.id, published.fingerprint)
        return published
    }

    suspend fun board(id: Long): TierList = checkNotNull(boards.getTierListById(id)) { "No board $id on this phone" }

    suspend fun boardByUid(uid: String): TierList? = boards.getAllTierLists().singleOrNull { it.uid == uid }

    fun answersTo(method: String, path: String): List<Answered> = answered.filter { it.method == method && it.path == path }

    /** A board as somebody would make one: the five default tiers, with [ranked] cards put in them by label. */
    suspend fun newBoard(
        title: String,
        category: String = ListCategory.FilmTv.id,
        ranked: Map<String, List<String>> = mapOf("S" to listOf("Alien"), "A" to listOf("Heat", "Ronin")),
    ): Long {
        val id = boards.createTierList(title, captions = emptyList())
        boards.setCategory(id, category)
        val tiers = board(id).tiers
        ranked.forEach { (label, titles) ->
            val tier = tiers.single { it.label == label }
            titles.forEachIndexed { position, card ->
                boards.moveItem(boards.addItemToPool(id, card, imageUrl = null), tier.id, position)
            }
        }
        return id
    }

    fun cardsIn(board: TierList, label: String): List<String> =
        board.tiers.single { it.label == label }.items.map { it.title }

    private fun record(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val path = chain.request().url.encodedPath.removePrefix(FUNCTIONS_PATH)
        val answer = Answered(chain.request().method, path, response.code, response.peekBody(MAX_KEPT_BODY).string())
        answered += answer
        afterAnswer(answer)
        return response
    }

    override fun close() = database.close()

    private object PicturesStayHere : Pictures {
        override suspend fun put(pictureId: String, bytes: ByteArray): Boolean = false

        override suspend fun get(pictureId: String): ByteArray? = null
    }

    private object Metered : Connection {
        override val available: Flow<Unit> = emptyFlow()

        override fun unmetered(): Boolean = false
    }

    private companion object {
        val FUNCTIONS_PATH = "/${Emulators.PROJECT}/europe-west1"
        const val MAX_KEPT_BODY = 64L * 1024

        /** Only read for who "me" is, which the server says anyway; nobody signs in through it here. */
        fun firebaseAuth(context: Context): FirebaseAuth {
            val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(Emulators.PROJECT)
                    .setApplicationId("1:1:android:e2e")
                    .setApiKey("demo")
                    .build(),
            )
            return FirebaseAuth.getInstance(app)
        }
    }
}
