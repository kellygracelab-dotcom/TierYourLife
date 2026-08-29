package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private val published = PublishedList(
    summary = PublishedListSummary(
        id = "abc",
        title = "Every A24 film",
        authorUid = "author-1",
        authorName = "Danylo K.",
        category = ListCategory.FilmTv,
        itemCount = 2,
        updatedAtMillis = 0,
    ),
    tiers = listOf(
        Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
        Tier(id = 1, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", items = emptyList()),
    ),
    items = listOf(TierItem(0, "Hereditary", null), TierItem(1, "Moonlight", "https://i/2.jpg")),
)

@RunWith(AndroidJUnit4::class)
class CommunityListViewModelTest {

    @Test
    fun opening_putsTheAuthorsCardsInThePool_andLeavesEveryTierEmpty() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.state.first { it is CommunityListUiState.Success } as CommunityListUiState.Success

        assertEquals("Danylo K.", state.authorName)
        assertEquals(2, state.list.tiers.first { it.isPool }.items.size)
        assertTrue(state.list.tiers.filterNot { it.isPool }.all { it.items.isEmpty() })
    }

    // Ranking someone else's list writes nothing: backing out has to cost the
    // reader only the arrangement they were told was unsaved.
    @Test
    fun ranking_changesNothingInTheDatabase() = runBlocking {
        val tiers = FakeTierRepositoryForCommunity()
        val viewModel = viewModel(tiers = tiers)
        val loaded = viewModel.state.first { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val poolItem = loaded.list.tiers.first { it.isPool }.items.first()
        val target = loaded.list.tiers.first { !it.isPool }

        viewModel.moveItem(poolItem.id, target.id, 0)
        val ranked = viewModel.state.first {
            (it as? CommunityListUiState.Success)?.arranged == true
        } as CommunityListUiState.Success

        assertEquals(1, ranked.list.tiers.first { it.id == target.id }.items.size)
        assertTrue(tiers.templates.isEmpty())
    }

    @Test
    fun saving_copiesTheListUnderTheReadersOwnRoof_withTheAuthorAttached() = runBlocking {
        val tiers = FakeTierRepositoryForCommunity()
        val viewModel = viewModel(tiers = tiers)
        viewModel.state.first { it is CommunityListUiState.Success }

        // The view model clears `saving` before it calls back, so waiting on the
        // flag can win the race against the id it is waiting for.
        val savedId = CompletableDeferred<Long>()
        viewModel.saveToMyLists { savedId.complete(it) }
        assertEquals(7L, savedId.await())

        val template = tiers.templates.single()
        assertEquals("Every A24 film", template.title)
        assertEquals("Danylo K.", template.authorName)
        assertEquals(2, template.items.size)
        assertEquals(2, template.tiers.size)
    }

    @Test
    fun aListThatCannotBeOpened_saysSoRatherThanShowingAnEmptyBoard() = runBlocking {
        val viewModel = viewModel(community = FailingCommunityRepository())

        val state = viewModel.state.first { it is CommunityListUiState.Error }

        assertEquals(CommunityListUiState.Error, state)
    }

    private fun viewModel(
        community: CommunityRepository = FakeCommunityRepository(),
        tiers: FakeTierRepositoryForCommunity = FakeTierRepositoryForCommunity(),
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = FakeAppPreferences(),
        savedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
    )
}

private class FakeCommunityRepository : CommunityRepository {
    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
    ): Result<List<PublishedListSummary>> = Result.success(emptyList())
    override suspend fun open(id: String): Result<PublishedList> = Result.success(published)
    override suspend fun publish(list: TierList): Result<String> = Result.success("abc")
    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
}

private class FailingCommunityRepository : CommunityRepository {
    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
    ): Result<List<PublishedListSummary>> = Result.success(emptyList())
    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException("nope"))
    override suspend fun publish(list: TierList): Result<String> = Result.failure(IllegalStateException())
    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
}
