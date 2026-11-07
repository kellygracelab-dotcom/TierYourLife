package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A24: a card or tier that took a new uid here, because another board held it, left the board's game naming the old one. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class UidCollisionPlayTest {

    private lateinit var database: TierDatabase
    private lateinit var dao: BoardSyncDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, TierDatabase::class.java).build()
        dao = database.boardSyncDao()
    }

    @After
    fun tearDown() = database.close()

    private fun tier(uid: String, vararg items: String) = IncomingTier(
        tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE", uid = uid),
        items = items.mapIndexed { index, item -> TierItemEntity(tierId = 0, position = index, title = item, imageUrl = null, uid = item) },
    )

    private fun game(ids: List<String>, scope: String) =
        """{"kind":"hill","count":${ids.size},"seed":1,"refine":false,"ids":[${ids.joinToString(",") { "\"$it\"" }}],""" +
            """"history":[],"round":0,"startedAt":1,"scope":["$scope"]}"""

    @Test
    fun aBoardArrivingUnderUidsHeldHere_keepsItsGameUnderTheUidsItTook() = runBlocking {
        dao.addBoard(TierListEntity(title = "Mine", uid = "mine"), listOf(tier("t1", "a")))

        dao.addBoard(TierListEntity(title = "Site", uid = "site", play = game(listOf("a", "b"), scope = "t1")), listOf(tier("t1", "a", "b")))

        val site = dao.boardByUid("site")!!
        val tier = dao.tiersOf(site.id).single().uid
        val (a, b) = dao.itemsOf(site.id).sortedBy { it.position }.map { it.uid }
        assertNotEquals("a", a)
        assertNotEquals("t1", tier)
        assertEquals(game(listOf(a, b), scope = tier), site.play)
    }

    @Test
    fun aCardArrivingOnABoardHereUnderAUidHeldElsewhere_isRenamedInItsGame() = runBlocking {
        dao.addBoard(TierListEntity(title = "Mine", uid = "mine"), listOf(tier("t1", "a")))
        dao.addBoard(TierListEntity(title = "Site", uid = "site"), listOf(tier("t2", "b")))
        val site = dao.boardByUid("site")!!

        dao.replaceContents(site.copy(play = game(listOf("b", "a"), scope = "t2")), listOf(tier("t2", "b", "a")))

        val now = dao.boardByUid("site")!!
        val a = dao.itemsOf(site.id).single { it.title == "a" }.uid
        assertNotEquals("a", a)
        assertEquals(game(listOf("b", a), scope = "t2"), now.play)
    }

    @Test
    fun aGameNamingACardTheBoardLacks_goesWhenAUidIsRenamed() = runBlocking {
        dao.addBoard(TierListEntity(title = "Mine", uid = "mine"), listOf(tier("t1", "a")))

        dao.addBoard(TierListEntity(title = "Site", uid = "site", play = game(listOf("a", "z"), scope = "t1")), listOf(tier("t1", "a")))

        assertNull(dao.boardByUid("site")!!.play)
    }

    @Test
    fun noUidTaken_leavesTheGameAsItCame() = runBlocking {
        val sent = game(listOf("a", "z"), scope = "t1")

        dao.addBoard(TierListEntity(title = "Site", uid = "site", play = sent), listOf(tier("t1", "a")))

        assertEquals(sent, dao.boardByUid("site")!!.play)
    }
}
