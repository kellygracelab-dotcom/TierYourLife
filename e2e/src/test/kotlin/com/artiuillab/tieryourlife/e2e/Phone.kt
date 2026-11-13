package com.artiuillab.tieryourlife.e2e

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.artiuillab.tieryourlife.core.network.AppCheckInterceptor
import com.artiuillab.tieryourlife.core.network.ErasedAccounts
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
import com.artiuillab.tieryourlife.feature.tier.data.sync.SyncLock
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.Interceptor
import okhttp3.Response
import org.robolectric.Shadows.shadowOf
import java.io.Closeable
import java.net.URI
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/** One call as it went over the wire. */
data class Answered(val method: String, val path: String, val status: Int, val body: String)

/**
 * One install of the app, wired as its Hilt modules wire it but over the
 * emulators' address. Only what Firebase would hand over is swapped: no App
 * Check token, and the ID token comes from the Auth emulator. Pictures stay
 * on the phone: the Storage part of sync is the Firebase SDK's, not ours.
 */
class Phone(requestedIdentity: Identity) : Closeable {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val session = firebaseSession(context, requestedIdentity)
    private val identity = session.identity

    val answered = CopyOnWriteArrayList<Answered>()

    /** Runs after each answer arrives, before the app reads it: how a test makes another phone act mid-run. */
    @Volatile
    var afterAnswer: (Answered) -> Unit = {}

    private val database = Room.inMemoryDatabaseBuilder(context, TierDatabase::class.java).build()
    private val preferences = FakeAppPreferences()
    private val accounts = FakeAccountRepository(
        initial = if (session.auth.currentUser?.isAnonymous == true) Account.Guest else Account.SignedIn(email = identity.email, photoUrl = null, displayName = "E2E", uid = identity.uid),
    )

    private val client = ProxyModule.provideProxyOkHttpClient(
        appCheckInterceptor = AppCheckInterceptor { null },
        idTokenInterceptor = IdTokenInterceptor { identity.idToken },
        erasedAccounts = ErasedAccounts(),
    ).newBuilder().addInterceptor(::record).build()
    private val retrofit = ProxyModule.proxyRetrofit(Emulators.functionsUrl, client)

    val boardsApi = NetworkModule.provideBoardsApi(retrofit)
    private val images = TierImageStore(context)
    val ownership = BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences)
    private val pictures = PictureSync(database.boardSyncDao(), images, PicturesStayHere, preferences, Metered)

    val boards = RoomTierRepository(database.tierDao(), images, ownership)
    val community = RetrofitCommunityRepository(
        api = CommunityModule.provideCommunityApi(retrofit),
        pictures = OwnPicturesOnThisPhone(images, pictures, accounts),
        auth = session.auth,
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
        lock = SyncLock(),
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

    override fun close() {
        try {
            database.close()
        } finally {
            session.auth.signOut()
            // The SDK's emulator registry shares the demo key across apps and still visits them on useEmulator.
            // Keep these bounded test-worker apps alive; each phone still has its own signed-out Auth instance.
        }
    }

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

        /** Each phone has its own SDK user and bearer, over the local Auth emulator only. */
        fun firebaseSession(context: Context, requested: Identity): FirebaseSession {
            val endpoint = URI("http://${Emulators.authHost}")
            val host = endpoint.host?.removePrefix("[")?.removeSuffix("]")
            check(host in setOf("127.0.0.1", "localhost", "::1") && endpoint.port in 1..65535) { "Auth fixture requires a localhost emulator" }
            val app = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(Emulators.PROJECT)
                    .setApplicationId("1:1:android:e2e")
                    .setApiKey("demo")
                    .build(),
                "e2e-phone-${UUID.randomUUID()}",
            )
            val auth = FirebaseAuth.getInstance(app)
            auth.useEmulator(checkNotNull(host), endpoint.port)
            val anonymous = requested.email == null && requested.password == null
            val signIn = if (anonymous) auth.signInAnonymously() else auth.signInWithEmailAndPassword(checkNotNull(requested.email), checkNotNull(requested.password))
            val user = checkNotNull(awaitSdk(signIn).user)
            if (!anonymous) check(user.uid == requested.uid) { "SDK signed in as a different synthetic account" }
            check(user.isAnonymous == anonymous) { "SDK provider differs from the requested fixture" }
            val token = awaitSdk(user.getIdToken(false))
            check((token.claims["user_id"] ?: token.claims["sub"]) == user.uid) { "SDK and bearer identities differ" }
            // A REST anonymous uid cannot be attached to signInAnonymously: use the SDK's actual guest for both sides.
            val identity = requested.copy(uid = user.uid, idToken = checkNotNull(token.token), email = user.email)
            return FirebaseSession(auth, identity)
        }

        /** SDK completions may post to Robolectric's main looper; never block it with Tasks.await. */
        fun <T> awaitSdk(task: Task<T>): T {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (!task.isComplete && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                if (!task.isComplete) Thread.sleep(10)
            }
            check(task.isComplete) { "Auth emulator SDK task timed out" }
            task.exception?.let { throw it }
            return task.result
        }
    }

    private data class FirebaseSession(val auth: FirebaseAuth, val identity: Identity)
}
