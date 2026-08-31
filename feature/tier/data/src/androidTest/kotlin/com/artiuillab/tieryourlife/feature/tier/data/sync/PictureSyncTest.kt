package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * Getting a card's own picture to the account and back.
 *
 * Neither direction keeps a queue, and these are the cases that prove it: what
 * still has to move is worked out from the database and the disk every time, so
 * a run that failed yesterday is simply a shorter run today.
 */
@RunWith(AndroidJUnit4::class)
class PictureSyncTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: TierDatabase
    private lateinit var dao: BoardSyncDao
    private lateinit var images: TierImageStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TierDatabase::class.java).build()
        dao = database.boardSyncDao()
        images = TierImageStore(directory = folder.root, openSource = { null })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun aPictureOnThePhone_goesUpOnce_andIsNotSentAgain() = runBlocking {
        val vault = FakePictures()
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(pictureId = "pic-1", bytes = byteArrayOf(1, 2, 3))

        sync.push()
        sync.push()

        assertEquals(listOf("pic-1"), vault.written)
        assertArrayEquals(byteArrayOf(1, 2, 3), vault.held["pic-1"])
    }

    // Recording it anyway would lose it quietly, which is the one outcome the
    // whole of this exists to prevent.
    @Test
    fun aPictureThatWillNotGoUp_isTriedAgainNextTime() = runBlocking {
        val vault = FakePictures(refuse = true)
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(pictureId = "pic-1", bytes = byteArrayOf(1))

        sync.push()
        sync.push()

        assertEquals(listOf("pic-1", "pic-1"), vault.written)
    }

    // A poster lives on somebody else's server. Copying it into our account
    // would be paying to store what is already free.
    @Test
    fun aPosterFromACatalogue_isNotSentAnywhere() = runBlocking {
        val vault = FakePictures()
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(imageUrl = "https://image.tmdb.org/t/p/w500/a.jpg")

        sync.push()

        assertTrue(vault.written.isEmpty())
    }

    // A card in the trash can be restored for thirty days, and restoring it to
    // a blank tile is not restoring it.
    @Test
    fun aPictureOnATrashedCard_stillGoesUp() = runBlocking {
        val vault = FakePictures()
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(pictureId = "pic-1", bytes = byteArrayOf(1), deletedAt = 1_700_000_000_000)

        sync.push()

        assertEquals(listOf("pic-1"), vault.written)
    }

    @Test
    fun aPictureAboardSays_shouldBeHere_comesDownAndIsWrittenToTheCard() = runBlocking {
        val vault = FakePictures(held = mutableMapOf("pic-1" to byteArrayOf(9, 9)))
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(imageUrl = images.pathFor("pic-1"), itemUid = "item-1")

        sync.pull(mapOf("item-1" to "pic-1"))

        assertTrue(images.holds("pic-1"))
        assertArrayEquals(byteArrayOf(9, 9), images.read("pic-1"))
        assertEquals(images.pathFor("pic-1"), dao.itemsOf(1L).single().imageUrl)
    }

    @Test
    fun aPictureAlreadyHere_isNotFetchedAgain() = runBlocking {
        val vault = FakePictures(held = mutableMapOf("pic-1" to byteArrayOf(9)))
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(pictureId = "pic-1", bytes = byteArrayOf(1), itemUid = "item-1")

        sync.pull(mapOf("item-1" to "pic-1"))

        assertTrue(vault.read.isEmpty())
    }

    // One that came down never needs sending back, and saying so here is what
    // stops the next push re-uploading everything a new phone just fetched.
    @Test
    fun aPictureThatCameDown_isNotSentStraightBackUp() = runBlocking {
        val vault = FakePictures(held = mutableMapOf("pic-1" to byteArrayOf(9)))
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(imageUrl = images.pathFor("pic-1"), itemUid = "item-1")

        sync.pull(mapOf("item-1" to "pic-1"))
        sync.push()

        assertTrue(vault.written.isEmpty())
    }

    @Test
    fun theProgressCounts_andIsBackToNothingAtTheEnd() = runBlocking {
        val vault = FakePictures(held = mutableMapOf("pic-1" to byteArrayOf(1), "pic-2" to byteArrayOf(2)))
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(imageUrl = images.pathFor("pic-1"), itemUid = "item-1")
        givenCard(imageUrl = images.pathFor("pic-2"), itemUid = "item-2", boardId = 2, title = "Other")

        assertTrue(sync.restoring.value.finished)
        sync.pull(mapOf("item-1" to "pic-1", "item-2" to "pic-2"))

        assertTrue(sync.restoring.value.finished)
        assertEquals(PictureRestore.Progress.Idle, sync.restoring.value)
    }

    @Test
    fun aPictureThatWillNotComeDown_leavesTheCardAlone() = runBlocking {
        val vault = FakePictures()
        val sync = PictureSync(dao, images, vault, OnWifiAlways, OnWifiAlways)
        givenCard(imageUrl = images.pathFor("pic-1"), itemUid = "item-1")

        sync.pull(mapOf("item-1" to "pic-1"))

        assertFalse(images.holds("pic-1"))
        // Still pointing where the picture will land, so the next run asks
        // for it again rather than forgetting it ever existed.
        assertEquals(images.pathFor("pic-1"), dao.itemsOf(1L).single().imageUrl)
    }

    private suspend fun givenCard(
        pictureId: String? = null,
        bytes: ByteArray? = null,
        imageUrl: String? = null,
        itemUid: String = "item-1",
        deletedAt: Long? = null,
        boardId: Long = 1,
        title: String = "Sci-fi films",
    ) {
        val path = when {
            pictureId != null && bytes != null -> images.write(pictureId, bytes)
            else -> imageUrl
        }
        if (dao.boardByUid("board-$boardId") == null) {
            dao.insertBoard(TierListEntity(id = boardId, title = title, uid = "board-$boardId"))
            dao.insertTier(
                TierEntity(
                    id = boardId * 10,
                    tierListId = boardId,
                    position = 0,
                    label = "S",
                    colorLight = "#B03A32",
                    colorDark = "#F1948C",
                    uid = "tier-$boardId",
                ),
            )
        }
        dao.insertItem(
            TierItemEntity(
                tierId = boardId * 10,
                position = 0,
                title = "Arrival",
                imageUrl = path,
                deletedAt = deletedAt,
                uid = itemUid,
            ),
        )
    }
}

/**
 * Wi-Fi, always, because these cases are about what moves rather than about
 * when it is allowed to.
 */
private object OnWifiAlways : AppPreferences, Connection {
    override val available = MutableStateFlow(Unit)
    override fun unmetered(): Boolean = true

    override fun picturesOnWifiOnly(): Boolean = true
    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) = Unit
    override fun themeChoice(): ThemeChoice = ThemeChoice.SYSTEM
    override fun setThemeChoice(choice: ThemeChoice) = Unit
    override fun languageTag(): String? = null
    override fun setLanguageTag(tag: String?) = Unit
    override fun lastKnownCredits(): Int? = null
    override fun setLastKnownCredits(credits: Int?) = Unit

    override fun lastKnownPendingReports(): Int? = null

    override fun setLastKnownPendingReports(reports: Int?) = Unit

    override fun lastKnownTrashCount(): Int = 0

    override fun setLastKnownTrashCount(count: Int) = Unit
    override fun hiddenListIds(): Set<String> = emptySet()
    override fun hiddenLists(): List<HiddenEntry> = emptyList()
    override fun hideList(publishedId: String, title: String) = Unit
    override fun unhideList(publishedId: String) = Unit
    override fun hiddenAuthorUids(): Set<String> = emptySet()
    override fun hiddenAuthors(): List<HiddenEntry> = emptyList()
    override fun hideAuthor(authorUid: String, name: String) = Unit
    override fun unhideAuthor(authorUid: String) = Unit
    private var asPictures = false

    override fun boardsAsPictures(): Boolean = asPictures

    override fun setBoardsAsPictures(asPictures: Boolean) {
        this.asPictures = asPictures
    }

    override fun backUpBoards(): Boolean = true
    override fun setBackUpBoards(backUp: Boolean) = Unit
    override fun signInOfferAnswered(): Boolean = false
    override fun markSignInOfferAnswered() = Unit
    override fun lastSyncedAtMs(): Long? = null
    override fun setLastSyncedAtMs(atMs: Long?) = Unit
    override fun conflictsSeen(): Set<String> = emptySet()
    override fun markConflictSeen(listUid: String) = Unit
}

private class FakePictures(
    val held: MutableMap<String, ByteArray> = mutableMapOf(),
    private val refuse: Boolean = false,
) : Pictures {

    val written = mutableListOf<String>()
    val read = mutableListOf<String>()

    override suspend fun put(pictureId: String, bytes: ByteArray): Boolean {
        written += pictureId
        if (refuse) return false
        held[pictureId] = bytes
        return true
    }

    override suspend fun get(pictureId: String): ByteArray? {
        read += pictureId
        return held[pictureId]
    }
}
