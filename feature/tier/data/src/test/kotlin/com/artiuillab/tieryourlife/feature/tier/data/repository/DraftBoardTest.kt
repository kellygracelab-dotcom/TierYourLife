package com.artiuillab.tieryourlife.feature.tier.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** New list makes a draft: its own board shows it, nothing else does, until something is done on it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DraftBoardTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var database: TierDatabase
    private lateinit var repository: RoomTierRepository

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.databaseBuilder(context, TierDatabase::class.java, File(folder.root, "drafts.db").absolutePath).build()
        repository = RoomTierRepository(database.tierDao(), TierImageStore(folder.newFolder(), openSource = { null }), { "alice" }) { 1000 }
    }

    @After fun close() = database.close()

    @Test fun aDraft_opensOnItsBoard_butNoListOfBoardsNorTheAccountSeesIt() = runBlocking {
        val kept = repository.createTierList("Films", emptyList())
        val draft = repository.createDraft("Untitled", emptyList())

        assertNotNull(repository.getTierListById(draft))
        assertEquals(listOf(kept), repository.getTierListSummaries().map { it.id })
        assertEquals(listOf(kept), repository.getAllTierLists().map { it.id })
        assertEquals(listOf("Films"), database.boardSyncDao().boardsOf("alice").map { it.title })
    }

    @Test fun anUntouchedDraft_isDiscarded_andAKeptOneStays() = runBlocking {
        val left = repository.createDraft("Untitled", emptyList())
        repository.discardDraft(left)
        assertNull(repository.getTierListById(left))

        val used = repository.createDraft("Untitled", emptyList())
        repository.renameTierList(used, "Films")
        repository.keepDraft(used)
        repository.discardDraft(used)

        assertEquals(listOf("Films"), repository.getTierListSummaries().map { it.title })
        assertEquals(listOf("Films"), database.boardSyncDao().boardsOf("alice").map { it.title })
    }

    @Test fun discardingOnlyEverTakesADraft() = runBlocking {
        val board = repository.createTierList("Untitled", emptyList())

        repository.discardDraft(board)

        assertNotNull(repository.getTierListById(board))
    }

    @Test fun aDraftLeftByAClosedApp_goesWithTheNextNewList() = runBlocking {
        val forgotten = repository.createDraft("Untitled", emptyList())

        val next = repository.createDraft("Untitled", emptyList())

        assertNull(repository.getTierListById(forgotten))
        assertNotNull(repository.getTierListById(next))
    }
}
