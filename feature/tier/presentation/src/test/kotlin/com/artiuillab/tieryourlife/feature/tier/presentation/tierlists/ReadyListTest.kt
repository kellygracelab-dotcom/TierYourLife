package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** "Try a ready list" (artboard 50, PARITY 4.9): the first popular list of eight cards or more, for someone with no lists. */
class ReadyListTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Popular(private val answer: Result<List<PublishedListSummary>>) : FakeCommunityRepository() {
        val sorts = mutableListOf<FeedSort>()

        override suspend fun feed(
            category: String?,
            query: String?,
            author: String?,
            after: String?,
            sort: FeedSort,
            following: Boolean,
            topic: String?,
        ): Result<CommunityPage> {
            sorts += sort
            return answer.map { CommunityPage(it) }
        }
    }

    private fun summary(id: String, cards: Int) = PublishedListSummary(
        id = id,
        title = "List $id",
        authorUid = "uid",
        authorName = "Ann",
        category = "films",
        itemCount = cards,
        updatedAtMillis = 0L,
    )

    private fun viewModel(community: Popular, lists: List<TierList> = emptyList()) = TierListsViewModel(
        repository = FakeTierRepository(lists),
        community = community,
        preferences = FakeAppPreferences(),
        accounts = FakeAccountRepository(),
        boardSync = NoBoardSync,
        pictures = NoPictureRestore,
        owner = NoOwnerChange,
    )

    @Test
    fun withNoListsYet_theFirstPopularListOfEightCardsIsOffered() = runBlocking {
        val community = Popular(Result.success(listOf(summary("a", 5), summary("b", 12), summary("c", 30))))
        val viewModel = viewModel(community)

        viewModel.loadTierLists()
        val state = viewModel.state.firstWithin { (it as? TierListsUiState.Success)?.readyList != null } as TierListsUiState.Success

        assertEquals("b", state.readyList?.id)
        assertEquals(listOf(FeedSort.Popular), community.sorts)
    }

    @Test
    fun noListBigEnough_orNoAnswer_noCard() = runBlocking {
        for (answer in listOf(Result.success(listOf(summary("a", 7))), Result.failure(IOException("offline")))) {
            val viewModel = viewModel(Popular(answer))

            viewModel.loadTierLists()
            viewModel.state.firstWithin { it is TierListsUiState.Success }
            delay(100)

            assertNull((viewModel.state.value as TierListsUiState.Success).readyList)
        }
    }

    @Test
    fun someoneWithAListOfTheirOwn_isNotAsked() = runBlocking {
        val community = Popular(Result.success(listOf(summary("b", 12))))
        val own = TierList(id = 1, title = "Mine", tiers = listOf(Tier(id = 1, label = "S", colorLight = "#000000", colorDark = "#000000", items = emptyList())))
        val viewModel = viewModel(community, listOf(own))

        viewModel.loadTierLists()
        viewModel.state.firstWithin { (it as? TierListsUiState.Success)?.totalListCount == 1 }
        delay(100)

        assertNull((viewModel.state.value as TierListsUiState.Success).readyList)
        assertEquals(emptyList<FeedSort>(), community.sorts)
    }
}
