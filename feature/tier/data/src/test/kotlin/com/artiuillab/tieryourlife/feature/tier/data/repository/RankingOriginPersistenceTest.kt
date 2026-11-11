package com.artiuillab.tieryourlife.feature.tier.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RankingOriginPersistenceTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var database: TierDatabase
    private lateinit var repository: RoomTierRepository
    private var owner: String? = "alice"

    @Before fun setUp() = open()

    @After fun close() = database.close()

    private fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.databaseBuilder(context, TierDatabase::class.java, File(folder.root, "ranking.db").absolutePath).build()
        repository = RoomTierRepository(database.tierDao(), TierImageStore(folder.newFolder(), openSource = { null }), { owner }) { 1000 }
    }

    private fun source() = TierList(
        id = 0, title = "Films", category = "film_tv",
        tiers = listOf(
            Tier(0, "S", "#111111", "#EEEEEE", listOf(TierItem(0, "Duplicate", null, sourceIndex = 1))),
            Tier(1, "Pool", "#222222", "#DDDDDD", listOf(TierItem(1, "Duplicate", null, sourceIndex = 0)), isPool = true),
        ),
    )

    @Test fun aRankingCopySurvivesReopenWithDuplicateBindingsAndUndo() = runBlocking {
        val id = repository.createRankingCopy(source(), "original", 123, RankingChoice.Private)
        val first = repository.getTierListById(id)!!
        database.close()
        open()
        val loaded = repository.getTierListByUid(first.uid)!!
        assertEquals("original", loaded.takenFrom)
        assertEquals(123L, loaded.originVersion)
        assertEquals(RankingChoice.Private, loaded.rankingChoice)
        assertEquals("film_tv", loaded.category)
        assertEquals(listOf(1, 0), loaded.tiers.flatMap { it.items }.map { it.sourceIndex })
        val removed = repository.deleteTierItem(loaded.tiers.first().items.single().id)!!
        repository.restoreTierItem(removed)
        assertEquals(1, repository.getTierListById(id)!!.tiers.first().items.single().sourceIndex)
        repository.setRankingChoice(id, RankingChoice.Offered)
        assertEquals(RankingChoice.Private, repository.getTierListById(id)!!.rankingChoice)
    }

    @Test fun anotherOwnerCannotReadOrChangeProvenanceAndAChangedVersionUnbindsIndices() = runBlocking {
        val id = repository.createRankingCopy(source(), "original", 123, RankingChoice.Private)
        val uid = repository.getTierListById(id)!!.uid
        owner = "bob"
        assertNull(repository.getTierListById(id))
        assertNull(repository.getTierListByUid(uid))
        repository.setRankingChoice(id, RankingChoice.Ranking)
        repository.setRankingOrigin(id, 999)
        owner = "alice"
        assertEquals(123L, repository.getTierListById(id)!!.originVersion)
        assertEquals(RankingChoice.Private, repository.getTierListById(id)!!.rankingChoice)
        repository.setRankingOrigin(id, 124)
        val updated = repository.getTierListById(id)!!
        assertEquals(124L, updated.originVersion)
        assertTrue(updated.tiers.flatMap { it.items }.all { it.sourceIndex == null })
    }

    @Test fun aFailedCopyRollsBackTheBoardAndAllEarlierCards() = runBlocking {
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_copy BEFORE INSERT ON tier_items WHEN NEW.title = 'Abort' BEGIN SELECT RAISE(ABORT, 'injected'); END")
        val source = source().let { it.copy(tiers = it.tiers + Tier(2, "A", "#333333", "#CCCCCC", listOf(TierItem(2, "Abort", null)))) }
        val result = runCatching { repository.createRankingCopy(source, "original", 123, RankingChoice.Offered) }
        assertTrue(result.isFailure)
        assertTrue(repository.getAllTierLists().isEmpty())
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM tier_items").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }
}
