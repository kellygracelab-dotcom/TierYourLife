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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A5: a publish the server finished but whose answer never came is linked
 * back by sync. Linked without a fingerprint, the board never showed as
 * behind its list, whatever was edited before or since.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RelinkedListTest : SyncEngineBench() {

    private class PublishedFrom(private val boardUid: String) : FakeCommunityRepository() {
        override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(
            listOf(
                PublishedListSummary(
                    id = "pub-1",
                    title = "Films",
                    authorUid = "",
                    authorName = "",
                    category = "films",
                    itemCount = 1,
                    updatedAtMillis = 0,
                    boardUid = boardUid,
                ),
            ),
        )
    }

    @Test
    fun aRelinkedBoard_showsAsBehindUntilItIsUpdated() = runBlocking {
        val board = TierListEntity(title = "Films", category = "films", ownerUid = someone)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(
                    tier = TierEntity(tierListId = 0, position = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE"),
                    items = listOf(TierItemEntity(tierId = 0, position = 0, title = "Alien", imageUrl = null)),
                ),
            ),
        )
        val preferences = FakeAppPreferences()
        val accounts = FakeAccountRepository(initial = FakeAccountRepository.signedIn)

        engine(preferences = preferences, accounts = accounts, community = PublishedFrom(board.uid)).sync()

        val here = dao.boardByUid(board.uid)!!
        assertEquals("pub-1", here.publishedId)
        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
        val repository = RoomTierRepository(database.tierDao(), images, BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences))
        assertTrue(repository.isKnownBehind(repository.getTierListById(here.id)!!))
    }
}
