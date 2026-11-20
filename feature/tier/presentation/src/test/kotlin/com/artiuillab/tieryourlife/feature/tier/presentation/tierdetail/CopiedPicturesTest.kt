package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.ListTranslations
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CopiedPictures
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.pictureNow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Backend, 20.11: a copy keeps its original's addresses, and the server retires them 30 days after the original stops showing them. */
@OptIn(ExperimentalCoroutinesApi::class)
class CopiedPicturesTest {

    @Test
    fun aCardWhosePictureFailed_takesTheOneItsOriginalShowsNow() = runTest {
        val h = Harness(this)

        h.pictures.lost(nes)
        advanceUntilIdle()

        assertEquals(listOf(Replaced(nes.id, OLD_NES, NEW_NES, "Evan-Amos")), h.boards.replaced)
        assertEquals(1, h.reloads)
    }

    @Test
    fun everyCardThatFailsTogether_isMendedFromOneRead() = runTest {
        val h = Harness(this)

        h.pictures.lost(nes)
        h.pictures.lost(wii)
        h.pictures.lost(nes)
        advanceUntilIdle()

        assertEquals(1, h.community.opened)
        assertEquals(listOf(nes.id, wii.id), h.boards.replaced.map { it.card })
        assertEquals(1, h.reloads)
    }

    @Test
    fun aCardFailingLater_usesTheOriginalAlreadyRead() = runTest {
        val h = Harness(this)
        h.pictures.lost(nes)
        advanceUntilIdle()

        h.pictures.lost(wii)
        advanceUntilIdle()

        assertEquals(1, h.community.opened)
        assertEquals(listOf(nes.id, wii.id), h.boards.replaced.map { it.card })
    }

    @Test
    fun aBoardOfOnesOwn_readsNothing() = runTest {
        val h = Harness(this, board = copy.copy(takenFrom = null))

        h.pictures.lost(nes)
        advanceUntilIdle()

        assertEquals(0, h.community.opened)
        assertTrue(h.boards.replaced.isEmpty())
    }

    @Test
    fun aPictureOnThisPhone_isNotTheOriginalsToMend() = runTest {
        val h = Harness(this)

        h.pictures.lost(nes.copy(imageUrl = "/data/user/0/app/files/tier_images/mine"))
        advanceUntilIdle()

        assertEquals(0, h.community.opened)
    }

    @Test
    fun offline_nothingChanges_andTheNextFailureTriesAgain() = runTest {
        val h = Harness(this, reachable = false)
        h.pictures.lost(nes)
        advanceUntilIdle()
        assertTrue(h.boards.replaced.isEmpty())
        assertEquals(0, h.reloads)

        h.community.reachable = true
        h.pictures.lost(wii)
        advanceUntilIdle()

        assertEquals(listOf(wii.id), h.boards.replaced.map { it.card })
    }

    @Test
    fun anOriginalThatStillShowsTheSamePicture_leavesTheCard() = runTest {
        val h = Harness(this, original = original(nesPicture = OLD_NES))

        h.pictures.lost(nes)
        advanceUntilIdle()

        assertTrue(h.boards.replaced.isEmpty())
        assertEquals(0, h.reloads)
    }

    @Test
    fun aCardMovedInTheOriginal_isFoundByItsName() {
        val moved = original().let { it.copy(items = it.items.reversed()) }

        assertEquals(NEW_NES, pictureNow(nes, moved)?.imageUrl)
    }

    @Test
    fun aCardRenamedByItsOwner_isLeftAlone() {
        assertNull(pictureNow(nes.copy(title = "My first console"), original()))
    }

    @Test
    fun twoCardsOfOneName_trustOnlyTheirPlace() {
        val twins = original().let { it.copy(items = it.items + it.items.first().copy(id = 2, sourceIndex = 2, imageUrl = "https://x/other")) }

        assertEquals(NEW_NES, pictureNow(nes, twins)?.imageUrl)
        assertNull(pictureNow(nes.copy(sourceIndex = null), twins))
    }

    @Test
    fun aCopyReadInAnotherLanguage_findsItsCardByTheTranslatedName() {
        val translated = original().copy(translations = ListTranslations(mapOf("uk" to mapOf("NES" to "Денді"))))

        assertEquals(NEW_NES, pictureNow(nes.copy(title = "Денді", sourceIndex = null), translated)?.imageUrl)
    }

    private data class Replaced(val card: Long, val lost: String, val now: String?, val credit: String?)

    private class Boards(private val rest: TierRepository = RecordingTierRepository()) : TierRepository by rest {
        val replaced = mutableListOf<Replaced>()
        override suspend fun replaceLostPicture(itemId: Long, lost: String, picture: TierItem) {
            replaced += Replaced(itemId, lost, picture.imageUrl, picture.credit)
        }
    }

    private class Community(private val original: PublishedList, var reachable: Boolean) : FakeCommunityRepository() {
        var opened = 0
        override suspend fun open(id: String): Result<PublishedList> {
            opened++
            return if (reachable) Result.success(original) else Result.failure(java.io.IOException("offline"))
        }
    }

    private class Harness(scope: TestScope, board: TierList = copy, original: PublishedList = original(), reachable: Boolean = true) {
        val boards = Boards()
        val community = Community(original, reachable)
        var reloads = 0
        val pictures = CopiedPictures(boards, community, scope, current = { board }, reload = { reloads++ })
    }

    private companion object {
        const val OLD_NES = "https://tieryourlife.com/img/original/published/consoles/nes_p_old"
        const val NEW_NES = "https://tieryourlife.com/img/original/published/consoles/nes_p_new"
        const val OLD_WII = "https://tieryourlife.com/img/original/published/consoles/wii_p_old"
        const val NEW_WII = "https://tieryourlife.com/img/original/published/consoles/wii_p_new"

        val nes = TierItem(11, "NES", OLD_NES, sourceIndex = 0)
        val wii = TierItem(12, "Wii", OLD_WII, sourceIndex = 1)
        val copy = TierList(
            id = 5,
            title = "Every game console",
            tiers = listOf(Tier(1, "S", "#111111", "#EEEEEE", listOf(nes, wii))),
            takenFrom = "consoles",
        )

        fun original(nesPicture: String = NEW_NES) = PublishedList(
            summary = PublishedListSummary(
                id = "consoles", title = "Every game console", authorUid = "", authorName = "", category = "games",
                itemCount = 2, updatedAtMillis = 2,
            ),
            tiers = emptyList(),
            items = listOf(
                TierItem(0, "NES", nesPicture, credit = "Evan-Amos", sourceIndex = 0),
                TierItem(1, "Wii", NEW_WII, sourceIndex = 1),
            ),
        )
    }
}
