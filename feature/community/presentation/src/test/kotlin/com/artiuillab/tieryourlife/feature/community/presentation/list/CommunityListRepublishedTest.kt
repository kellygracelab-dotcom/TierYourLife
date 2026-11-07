package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.PageGone
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingsPage
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** What the list's author did meanwhile: republished it, kept its cover, took a ranking down. */
class CommunityListRepublishedTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private val signedIn = FakeAccountRepository(Account.SignedIn(email = "ann@example.com", photoUrl = null))

    private fun viewModel(
        community: CommunityRepository = TestCommunity(),
        tiers: RecordingTierRepository = RecordingTierRepository(),
        accounts: FakeAccountRepository = FakeAccountRepository(),
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = FakeAppPreferences(),
        savedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
        accounts = accounts,
        openedLists = FakeOpenedLists(),
        afterSignIn = FollowAfterSignIn(accounts),
    ).also { made += it }

    // The first Update would otherwise send no cover, and the server would build one from the first cards.
    @Test
    fun gettingMyListBack_keepsTheCoverItWentOutWith() = runBlocking {
        val cover = "https://firebasestorage.googleapis.com/v0/b/app/o/published%2Fabc%2Fcover?alt=media"
        val tiers = RecordingTierRepository()
        val community = TestCommunity(arrangement = listOf(0, null), summary = { it.copy(mine = true, coverImageUrl = cover) })
        val viewModel = viewModel(community = community, tiers = tiers)
        lookedUp(viewModel)

        viewModel.ownList.getBack()

        assertEquals(ListNotice.OpenBoard(7), nextNotice(viewModel))
        assertEquals(listOf(7L to listOf(cover)), tiers.covers)
    }

    @Test
    fun gettingMyListBack_withTheCoverLeftToTheCards_choosesNone() = runBlocking {
        val tiers = RecordingTierRepository()
        val community = TestCommunity(arrangement = listOf(0, null), summary = { it.copy(mine = true, previewImages = listOf("https://i/2.jpg")) })
        val viewModel = viewModel(community = community, tiers = tiers)
        lookedUp(viewModel)

        viewModel.ownList.getBack()

        assertEquals(ListNotice.OpenBoard(7), nextNotice(viewModel))
        assertTrue(tiers.covers.isEmpty())
    }

    // Rows name cards by their place: after a republish they would land on other cards.
    @Test
    fun showingMyRanking_afterTheAuthorRepublished_saysTheListChanged_andSendsNothing() = runBlocking {
        var version = 1L
        val community = TestCommunity(summary = { it.copy(updatedAtMillis = version) })
        val viewModel = viewModel(community = community, accounts = signedIn)
        arranged(viewModel)

        version = 2L
        viewModel.showMyRanking()

        assertEquals(RankingNotice.ListChanged, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertTrue(community.shownRankings.isEmpty())
    }

    @Test
    fun showingMyRanking_readsTheListAgainEvenWhenItCameFromTheServer() = runBlocking {
        val community = TestCommunity()
        val viewModel = viewModel(community = community, accounts = signedIn)
        arranged(viewModel)
        val opensBefore = community.opens

        viewModel.showMyRanking()

        assertEquals(RankingNotice.Shown, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertEquals(opensBefore + 1, community.opens)
    }

    // 409 CURSOR_GONE: a ranking on the last page was withdrawn meanwhile.
    @Test
    fun aPageOfRankingsThatIsGone_startsTheRankingsAgainFromTheFirst() = runBlocking {
        val asked = mutableListOf<String?>()
        val community = object : FakeCommunityRepository() {
            override suspend fun open(id: String): Result<PublishedList> =
                Result.success(published.copy(summary = published.summary.copy(rankingCount = 3)))

            override suspend fun rankings(listId: String, after: String?): Result<RankingsPage> {
                asked += after
                return when {
                    after != null -> Result.failure(PageGone())
                    asked.size == 1 -> Result.success(RankingsPage(listOf(myOldRanking.copy(uid = "x")), nextCursor = "x"))
                    else -> Result.success(RankingsPage(listOf(myOldRanking.copy(uid = "y")), nextCursor = null))
                }
            }
        }
        val viewModel = viewModel(community = community)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.moreRankings == true }

        viewModel.rankings.loadMore()

        val state = viewModel.state.firstWithin {
            (it as? CommunityListUiState.Success)?.rankings?.map { ranking -> ranking.uid } == listOf("y")
        } as CommunityListUiState.Success
        assertEquals(listOf(null, "x", null), asked)
        assertFalse(state.moreRankings)
    }

    @Test
    fun aBannedReader_isToldWhy() = runBlocking {
        val community = TestCommunity(showRefusal = RankingRefusal.Banned)
        val viewModel = viewModel(community = community, accounts = signedIn)
        arranged(viewModel)

        viewModel.showMyRanking()

        assertEquals(RankingNotice.Banned, withTimeout(5_000) { viewModel.rankingNotices.first() })
    }

    // A18: a list gone or one's own came back as "Something went wrong".
    @Test
    fun aListGoneOrOnesOwn_isNamed() = runBlocking {
        listOf(RankingRefusal.ListGone to RankingNotice.ListGone, RankingRefusal.OwnList to RankingNotice.OwnList).forEach { (refusal, notice) ->
            val viewModel = viewModel(community = TestCommunity(showRefusal = refusal), accounts = signedIn)
            arranged(viewModel)

            viewModel.showMyRanking()

            assertEquals(notice, withTimeout(5_000) { viewModel.rankingNotices.first() })
        }
    }
}
