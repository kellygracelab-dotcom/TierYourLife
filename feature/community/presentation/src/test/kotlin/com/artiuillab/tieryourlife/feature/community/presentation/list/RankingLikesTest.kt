package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.LikeRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
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

/** SPEC list-flow п. 5, К1–К2: a thumb up on anyone else's placing, no counts, and this reader's own in its place. */
class RankingLikesTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private val kyle = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)

    private fun viewModel(community: TestCommunity, signedIn: Boolean = true): CommunityListViewModel {
        val accounts = if (signedIn) FakeAccountRepository(Account.SignedIn(email = "ann@example.com", photoUrl = null)) else FakeAccountRepository()
        return CommunityListViewModel(
            community = community,
            tiers = RecordingTierRepository(),
            preferences = FakeAppPreferences(),
            savedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
            accounts = accounts,
            openedLists = FakeOpenedLists(),
            afterSignIn = FollowAfterSignIn(accounts),
            drafts = RankingDrafts.None,
        ).also { made += it }
    }

    private suspend fun withRankings(viewModel: CommunityListViewModel, signedIn: Boolean = true): CommunityListUiState.Success {
        viewModel.state.firstWithin { it is CommunityListUiState.Success && it.signedIn == signedIn && !it.arriving }
        viewModel.rankings.load()
        return viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true } as CommunityListUiState.Success
    }

    @Test
    fun aThumbUp_likesIt_andASecondTapTakesItBack() = runBlocking {
        val community = TestCommunity(rankings = listOf(kyle))
        val viewModel = viewModel(community)
        val shown = withRankings(viewModel)

        viewModel.rankings.like(shown.rankings.single())
        val liked = viewModel.state.firstWithin { (it as CommunityListUiState.Success).rankings.single().likedByMe && community.likes.size == 1 }
        viewModel.rankings.like((liked as CommunityListUiState.Success).rankings.single())
        viewModel.state.firstWithin { !(it as CommunityListUiState.Success).rankings.single().likedByMe && community.likes.size == 2 }

        assertEquals(listOf("r1" to true, "r1" to false), community.likes)
    }

    @Test
    fun aGuest_isAskedToSignIn_andNothingIsSent() = runBlocking {
        val community = TestCommunity(rankings = listOf(kyle))
        val viewModel = viewModel(community, signedIn = false)
        val shown = withRankings(viewModel, signedIn = false)

        viewModel.rankings.like(shown.rankings.single())

        assertEquals(RankingNotice.SignInToLike, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertTrue(community.likes.isEmpty())
        assertFalse((viewModel.state.value as CommunityListUiState.Success).rankings.single().likedByMe)
    }

    @Test
    fun overTheHourlyLimit_theThumbGoesBack_andSaysSo() = runBlocking {
        val community = TestCommunity(rankings = listOf(kyle), likeRefusal = LikeRefusal.TooMany)
        val viewModel = viewModel(community)
        val shown = withRankings(viewModel)

        viewModel.rankings.like(shown.rankings.single())

        assertEquals(RankingNotice.TooManyLikes, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertFalse((viewModel.state.value as CommunityListUiState.Success).rankings.single().likedByMe)
    }

    @Test
    fun onesOwn_hasNoThumb() = runBlocking {
        val community = TestCommunity(rankings = listOf(kyle, myOldRanking))
        val viewModel = viewModel(community)
        val shown = withRankings(viewModel)

        viewModel.rankings.like(shown.rankings.first { it.mine })

        assertTrue(community.likes.isEmpty())
    }

    @Test
    fun showingMineAgain_keepsItInItsPlace_notFirst() = runBlocking {
        val community = TestCommunity(rankings = listOf(kyle, myOldRanking), shownIsNew = false)
        val viewModel = viewModel(community)
        withRankings(viewModel)
        arranged(viewModel)

        viewModel.showMyRanking()

        assertEquals(RankingNotice.Shown, withTimeout(5_000) { viewModel.rankingNotices.first() })
        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(listOf("r1", myOldRanking.uid), after.rankings.map { it.uid })
        assertTrue(after.rankings.last().mine)
    }
}
