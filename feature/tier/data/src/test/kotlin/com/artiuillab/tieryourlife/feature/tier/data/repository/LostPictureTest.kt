package com.artiuillab.tieryourlife.feature.tier.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A copied card takes the picture its original shows now, with what names it, and only in place of the one that failed. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LostPictureTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var database: TierDatabase
    private lateinit var repository: RoomTierRepository

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TierDatabase::class.java).build()
        repository = RoomTierRepository(database.tierDao(), TierImageStore(folder.newFolder(), openSource = { null }), { null }) { 1000 }
    }

    @After fun close() = database.close()

    private var board = 0L

    private suspend fun card() = repository.getTierListById(board)!!.tiers.flatMap { it.items }.single()

    private suspend fun copiedCard(): TierItem {
        val source = TierList(
            id = 0,
            title = "Every game console",
            tiers = listOf(
                Tier(1, "Pool", "#222222", "#DDDDDD", listOf(TierItem(0, "NES", OLD, credit = "Old", sourceIndex = 0)), isPool = true),
            ),
        )
        board = repository.createRankingCopy(source, "consoles", 1)
        return card()
    }

    @Test fun theOriginalsPictureReplacesTheOneThatFailed() = runBlocking {
        val card = copiedCard()
        val attribution = PictureAttribution(source = "commons", author = "Evan-Amos")

        repository.replaceLostPicture(card.id, OLD, card.copy(imageUrl = NEW, credit = "Evan-Amos", attribution = attribution, focus = CoverFocus(0.5f, 0.4f, 1.2f)))

        val now = card()
        assertEquals(NEW, now.imageUrl)
        assertEquals("Evan-Amos", now.credit)
        assertEquals(attribution, now.attribution)
        assertEquals(CoverFocus(0.5f, 0.4f, 1.2f), now.focus)
    }

    @Test fun aPictureChosenMeanwhileStays() = runBlocking {
        val card = copiedCard()
        database.tierDao().updateTierItemImage(card.id, "/data/user/0/app/files/tier_images/mine")

        repository.replaceLostPicture(card.id, OLD, card.copy(imageUrl = NEW))

        val now = card()
        assertEquals("/data/user/0/app/files/tier_images/mine", now.imageUrl)
        assertNull(now.credit)
    }

    @Test fun anOriginalCardWithNoPictureChangesNothing() = runBlocking {
        val card = copiedCard()

        repository.replaceLostPicture(card.id, OLD, card.copy(imageUrl = null))

        assertEquals(OLD, card().imageUrl)
    }

    private companion object {
        const val OLD = "https://tieryourlife.com/img/original/published/consoles/nes_p_old"
        const val NEW = "https://tieryourlife.com/img/original/published/consoles/nes_p_new"
    }
}
