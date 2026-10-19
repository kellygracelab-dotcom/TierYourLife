package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val published = PublishedList(
    summary = PublishedListSummary(
        id = "abc",
        title = "Every A24 film",
        authorUid = "author-1",
        authorName = "Olena M.",
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

class CommunityListViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun opening_putsTheAuthorsCardsInThePool_andLeavesEveryTierEmpty() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        assertEquals("Olena M.", state.authorName)
        assertEquals(2, state.list.tiers.first { it.isPool }.items.size)
        assertTrue(state.list.tiers.filterNot { it.isPool }.all { it.items.isEmpty() })
    }

    // Ranking someone else's list writes nothing: backing out has to cost the
    // reader only the arrangement they were told was unsaved.
    @Test
    fun ranking_changesNothingInTheDatabase() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.show(Showing.Mine)
        val poolItem = loaded.mine.tiers.first { it.isPool }.items.first()
        val target = loaded.mine.tiers.first { !it.isPool }

        viewModel.moveItem(poolItem.id, target.id, 0)
        val ranked = viewModel.state.firstWithin {
            (it as? CommunityListUiState.Success)?.arranged == true
        } as CommunityListUiState.Success

        assertEquals(1, ranked.mine.tiers.first { it.id == target.id }.items.size)
        assertTrue(tiers.templates.isEmpty())
    }

    // The bug this replaces: a card dragged while the author's arrangement was
    // on screen landed in the author's board, which is not the one being
    // drawn -- so it sprang back to the pool on the next glance.
    @Test
    fun draggingWhileTheirArrangementIsShown_movesNothing() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.show(Showing.Theirs)
        val poolItem = loaded.mine.tiers.first { it.isPool }.items.first()
        val target = loaded.mine.tiers.first { !it.isPool }

        viewModel.moveItem(poolItem.id, target.id, 0)

        val after = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        assertEquals(false, after.arranged)
        assertEquals(loaded.list.tiers.map { tier -> tier.items.size }, after.list.tiers.map { it.items.size })
    }

    @Test
    fun draggingWhileYourOwnIsShown_movesTheCardAndLeavesTheirsAlone() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.show(Showing.Mine)
        val poolItem = loaded.mine.tiers.first { it.isPool }.items.first()
        val target = loaded.mine.tiers.first { !it.isPool }

        viewModel.moveItem(poolItem.id, target.id, 0)

        val after = viewModel.state.firstWithin {
            (it as? CommunityListUiState.Success)?.arranged == true
        } as CommunityListUiState.Success
        assertEquals(1, after.mine.tiers.first { it.id == target.id }.items.size)
        assertEquals(loaded.list.tiers.map { tier -> tier.items.size }, after.list.tiers.map { it.items.size })
    }

    // Keeping a list the way its author ranked it is a reason to save it, not
    // a mistake to correct.
    @Test
    fun saving_keepsWhicheverArrangementIsOnScreen() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.show(Showing.Theirs)

        val savedId = CompletableDeferred<Long>()
        viewModel.saveToMyLists { savedId.complete(it) }
        savedId.await()

        val theirs = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val template = tiers.templates.single()
        assertEquals(
            theirs.list.tiers.filterNot { it.isPool }.map { it.items.size },
            template.tiers.map { tier -> tier.items.size },
        )
        // Only what was left unranked goes to the pool. Every card used to,
        // which is how a saved copy came out with its tiers empty.
        assertEquals(theirs.list.tiers.first { it.isPool }.items.map { it.title }, template.items.map { it.title })
    }

    @Test
    fun saving_copiesTheListUnderTheReadersOwnRoof_withTheAuthorAttached() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        // The view model clears `saving` before it calls back, so waiting on the
        // flag can win the race against the id it is waiting for.
        val savedId = CompletableDeferred<Long>()
        viewModel.saveToMyLists { savedId.complete(it) }
        assertEquals(7L, savedId.await())

        val template = tiers.templates.single()
        assertEquals("Every A24 film", template.title)
        assertEquals("Olena M.", template.authorName)
        assertEquals(2, template.items.size)
        assertEquals(2, template.tiers.size)
    }

    @Test
    fun aListThatCannotBeOpened_saysSoRatherThanShowingAnEmptyBoard() = runBlocking {
        val viewModel = viewModel(community = FailingCommunityRepository())

        val state = viewModel.state.firstWithin { it is CommunityListUiState.Error }

        assertEquals(CommunityListUiState.Error, state)
    }

    private fun viewModel(
        community: CommunityRepository = TestCommunity(),
        tiers: RecordingTierRepository = RecordingTierRepository(),
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = FakeAppPreferences(),
        savedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
    )
}

private class TestCommunity : FakeCommunityRepository() {
    override suspend fun open(id: String): Result<PublishedList> = Result.success(published)
    override suspend fun publish(list: TierList): Result<Published> =
        Result.success(Published(id = "abc", fingerprint = "sent"))
}

private class FailingCommunityRepository : FakeCommunityRepository() {
    override suspend fun makeFace(pictureId: String): Result<String> = Result.failure(IllegalStateException())
}
