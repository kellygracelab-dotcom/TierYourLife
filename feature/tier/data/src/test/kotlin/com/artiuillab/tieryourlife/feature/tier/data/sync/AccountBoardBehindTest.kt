package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PublishFingerprint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * A board taken from the account knows its list but not what was sent. On
 * the website such a board had 176 cards against 149 in its list and never
 * offered Update; here its cards are counted against the list's, and with the
 * counts equal, compared with the list card by card.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccountBoardBehindTest : SyncEngineBench() {

    private class Published(
        tierCounts: List<Int> = listOf(2, 1),
        itemCount: Int = 3,
        private val cards: List<Pair<String, String?>> = listOf("Hades" to null, "Celeste" to null, "Inside" to null),
        private val arrangement: List<Int?> = listOf(0, 0, null),
        private val shown: Boolean = true,
    ) : FakeCommunityRepository() {
        var opened = 0

        private val summary = PublishedListSummary(
            id = "pub-1",
            title = "Games",
            authorUid = "",
            authorName = "",
            category = "games",
            itemCount = itemCount,
            updatedAtMillis = 0,
            tierCounts = tierCounts,
        )

        override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(listOf(summary))

        override suspend fun open(id: String): Result<PublishedList> {
            opened++
            if (!shown) return Result.failure(IOException("offline"))
            return Result.success(
                PublishedList(
                    summary = summary,
                    tiers = listOf(Tier(id = 0, label = "S", colorLight = "#111111", colorDark = "#EEEEEE", items = emptyList())),
                    items = cards.mapIndexed { index, (title, picture) -> TierItem(id = index.toLong(), title = title, imageUrl = picture) },
                    arrangement = arrangement,
                ),
            )
        }
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

    private suspend fun syncAgainst(community: FakeCommunityRepository, change: suspend (TierListEntity) -> Unit = {}): TierListEntity {
        val board = boardFromTheAccount()
        change(dao.boardByUid(board.uid)!!)
        val accounts = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        engine(preferences = FakeAppPreferences(), accounts = accounts, community = community).sync()
        return dao.boardByUid(board.uid)!!
    }

    @Test
    fun moreCardsThanItsList_showsAsBehind() = runBlocking {
        val here = syncAgainst(Published(tierCounts = listOf(1, 1), itemCount = 2))

        assertEquals("pub-1", here.publishedId)
        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
        assertTrue(behind(here))
    }

    @Test
    fun theSameBoardAsItsList_isNotBehind_untilItChanges() = runBlocking {
        val here = syncAgainst(Published())

        assertNotNull(here.publishedFingerprint)
        assertFalse(behind(here))
        dao.renameBoard(here.uid, "Games I finished")
        assertTrue(behind(dao.boardByUid(here.uid)!!))
    }

    @Test
    fun twoCardsSwappedBetweenTiers_withTheSameCounts_showAsBehind() = runBlocking {
        val here = syncAgainst(Published(cards = listOf("Hades" to null, "Inside" to null, "Celeste" to null)))

        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
    }

    @Test
    fun aCardMovedWithinItsTier_showsAsBehind() = runBlocking {
        val here = syncAgainst(Published(cards = listOf("Celeste" to null, "Hades" to null, "Inside" to null)))

        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
    }

    @Test
    fun aRenamedCard_showsAsBehind() = runBlocking {
        val here = syncAgainst(Published(cards = listOf("Hades II" to null, "Celeste" to null, "Inside" to null)))

        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
    }

    @Test
    fun aCardWithAnotherPicture_showsAsBehind() = runBlocking {
        val here = syncAgainst(Published(cards = listOf("Hades" to "https://i/hades.jpg", "Celeste" to null, "Inside" to null)))

        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
    }

    // The list's copy of an own photo keeps its name in the address.
    @Test
    fun anOwnPicture_matchesTheListsCopyOfIt() = runBlocking {
        val copy = "https://storage/feed/pub-1/p-hades.jpg?alt=media"
        val here = syncAgainst(Published(cards = listOf("Hades" to copy, "Celeste" to null, "Inside" to null))) { board ->
            val hades = dao.itemsOf(board.id).first { it.title == "Hades" }
            dao.updateItem(hades.copy(imageUrl = images.pathFor("p-hades.jpg")))
        }

        assertFalse(behind(here))
    }

    // Published before cards' places were kept: nobody can say, so Update is offered.
    @Test
    fun aListThatNeverSaidWhereCardsWent_showsAsBehind() = runBlocking {
        val here = syncAgainst(Published(arrangement = emptyList()))

        assertEquals(PublishFingerprint.UNKNOWN, here.publishedFingerprint)
    }

    @Test
    fun aListThatWouldNotOpen_isTriedAgainNextRun() = runBlocking {
        val community = Published(shown = false)
        val here = syncAgainst(community)

        assertNull(here.publishedFingerprint)
        assertEquals(1, community.opened)
    }

    private suspend fun behind(board: TierListEntity): Boolean {
        val accounts = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val ownership = BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), FakeAppPreferences())
        val repository = RoomTierRepository(database.tierDao(), images, ownership)
        return repository.isKnownBehind(repository.getTierListById(board.id)!!)
    }

    @Test
    fun countsByTier_orTheTotalWhenTheServerGaveNone() {
        assertTrue(cardsDifferFromPublished(listOf(2, 1), listOf(1, 2), publishedTotal = 3))
        assertFalse(cardsDifferFromPublished(listOf(2, 1), listOf(2, 1), publishedTotal = 3))
        assertTrue(cardsDifferFromPublished(listOf(2, 1), emptyList(), publishedTotal = 2))
        assertFalse(cardsDifferFromPublished(listOf(2, 1), emptyList(), publishedTotal = 3))
    }
}
