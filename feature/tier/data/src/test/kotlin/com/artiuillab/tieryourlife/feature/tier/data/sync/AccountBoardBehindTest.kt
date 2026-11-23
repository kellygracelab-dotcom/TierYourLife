package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PublishFingerprint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A board taken from the account knows its list but not what was sent. On
 * the website such a board had 176 cards against 149 in its list and never
 * offered Update; here its cards are counted against the list's.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccountBoardBehindTest : SyncEngineBench() {

    private class Published(private val tierCounts: List<Int>, private val itemCount: Int) : FakeCommunityRepository() {
        override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(
            listOf(
                PublishedListSummary(
                    id = "pub-1",
                    title = "Games",
                    authorUid = "",
                    authorName = "",
                    category = "games",
                    itemCount = itemCount,
                    updatedAtMillis = 0,
                    tierCounts = tierCounts,
                ),
            ),
        )
    }

    /** S holds two cards, the pool one: [2, 1]. */
    private suspend fun boardFromTheAccount(): TierListEntity {
        val board = TierListEntity(title = "Games", category = "games", ownerUid = someone, publishedId = "pub-1")
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = listOf(
                        TierItemEntity(tierId = 0, position = 0, title = "Hades", imageUrl = null),
                        TierItemEntity(tierId = 0, position = 1, title = "Celeste", imageUrl = null),
                    ),
                ),
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 1, label = "", colorLight = "#222222", colorDark = "#DDDDDD", isPool = true),
                    items = listOf(TierItemEntity(tierId = 0, position = 0, title = "Inside", imageUrl = null)),
                ),
            ),
        )
        return board
    }

    private suspend fun syncAgainst(community: FakeCommunityRepository): TierListEntity {
        val board = boardFromTheAccount()
        val accounts = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        engine(preferences = FakeAppPreferences(), accounts = accounts, community = community).sync()
        return dao.boardByUid(board.uid)!!
    }

    @Test
    fun moreCardsThanItsList_showsAsBehind() = runBlocking {
        val here = syncAgainst(Published(tierCounts = listOf(1, 1), itemCount = 2))

        assertEquals("pub-1", here.publishedId)
        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
        val preferences = FakeAppPreferences()
        val accounts = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val repository = RoomTierRepository(database.tierDao(), images, BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences))
        assertTrue(repository.isKnownBehind(repository.getTierListById(here.id)!!))
    }

    @Test
    fun theSameCountsAsItsList_sayNothing() = runBlocking {
        val here = syncAgainst(Published(tierCounts = listOf(2, 1), itemCount = 3))

        assertNull(here.publishedFingerprint)
    }

    @Test
    fun countsByTier_orTheTotalWhenTheServerGaveNone() {
        assertTrue(cardsDifferFromPublished(listOf(2, 1), listOf(1, 2), publishedTotal = 3))
        assertFalse(cardsDifferFromPublished(listOf(2, 1), listOf(2, 1), publishedTotal = 3))
        assertTrue(cardsDifferFromPublished(listOf(2, 1), emptyList(), publishedTotal = 2))
        assertFalse(cardsDifferFromPublished(listOf(2, 1), emptyList(), publishedTotal = 3))
    }
}
