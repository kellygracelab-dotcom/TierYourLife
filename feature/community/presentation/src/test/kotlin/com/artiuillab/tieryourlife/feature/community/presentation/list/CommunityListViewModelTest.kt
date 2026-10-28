package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingsPage
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

    @Test
    fun aGamesResult_becomesMine_onScreen_andStillUnsaved() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val played = loaded.mine.copy(
            tiers = loaded.mine.tiers.map { tier ->
                when {
                    tier.isPool -> tier.copy(items = emptyList())
                    tier.id == 0L -> tier.copy(items = published.items)
                    else -> tier
                }
            },
        )

        viewModel.putInMine(played)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(played, after.mine)
        assertEquals(Showing.Mine, after.showing)
        assertTrue(after.arranged)
        assertEquals(loaded.list, after.list)
        assertEquals(null, after.savedId)
    }

    // Keeping a list the way its author ranked it is a reason to save it, not
    // a mistake to correct.
    @Test
    fun savingTheirs_keepsTheAuthorsArrangement() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        // The switch shows the reader's own half; the choice at saving is what counts.
        viewModel.show(Showing.Mine)

        viewModel.saveToMyLists(Showing.Theirs)

        val theirs = viewModel.state.firstWithin { it.saved() } as CommunityListUiState.Success
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

        viewModel.saveToMyLists(Showing.Mine)
        val saved = viewModel.state.firstWithin { it.saved() } as CommunityListUiState.Success

        assertEquals(7L, saved.savedId)
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

    @Test
    fun whatTheCardShowed_isReadyBeforeTheListIs() {
        val viewModel = viewModel(
            handle = SavedStateHandle(
                mapOf(
                    "publishedId" to "abc",
                    "title" to "Every A24 film",
                    "authorName" to "Olena M.",
                    "itemCount" to 2,
                    "tierColors" to "#B03A32,#C06A25",
                ),
            ),
        )

        assertEquals(
            ListPreview(title = "Every A24 film", authorName = "Olena M.", itemCount = 2, tierColors = listOf("#B03A32", "#C06A25")),
            viewModel.preview,
        )
    }

    @Test
    fun aListOpenedByIdAlone_hasNothingToDrawAhead() {
        assertEquals(null, viewModel().preview)
    }

    // A reader's placing is the same cards under their own words; saving it keeps it, and the copy knows where it came from.
    @Test
    fun choosingAReadersRanking_showsTheirPlacing_andSavingTheirsKeepsIt() = runBlocking {
        val olena = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(community = TestCommunity(rankings = listOf(olena)), tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(olena)
        val shown = (viewModel.state.value as CommunityListUiState.Success).shown

        assertEquals(listOf("Top", "Next"), shown.tiers.filterNot { it.isPool }.map { it.label })
        assertEquals(listOf("Moonlight"), shown.tiers[0].items.map { it.title })
        assertEquals(listOf("Hereditary"), shown.tiers[1].items.map { it.title })

        viewModel.saveToMyLists(Showing.Theirs)
        viewModel.state.firstWithin { it.saved() }
        val kept = tiers.templates.single()
        assertEquals("abc", kept.takenFrom)
        assertEquals(listOf("Top", "Next"), kept.tiers.map { it.label })
    }

    @Test
    fun withdrawingMyRanking_takesItOutAtOnce_andPutsItBackIfRefused() = runBlocking {
        val mine = Ranking("me", "Me", null, arrangement = listOf(0, 0), tierLabels = emptyList(), updatedAtMillis = 0, mine = true)
        val community = TestCommunity(rankings = listOf(mine), refusesWithdrawal = true)
        val viewModel = viewModel(community = community)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(mine)

        viewModel.rankings.withdrawMine()
        val gone = viewModel.state.value as CommunityListUiState.Success
        assertEquals(0, gone.rankingCount)
        assertEquals(null, gone.chosenRanking)
        assertEquals(gone.list, gone.theirs)

        // Only now does the server refuse: the optimistic state above must not depend on who is faster.
        community.refusal.complete(Unit)
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankingCount == 1 }
        assertEquals(listOf("abc"), community.withdrawn)
    }

    // The chip says how many copies were saved, the number every feed card shows.
    @Test
    fun theChipCountsSaves_andSavingOneMoreCountsAtOnce() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(takeCount = 9))
        val opened = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        assertEquals(9, opened.takeCount)

        viewModel.saveToMyLists(Showing.Mine)

        val saved = viewModel.state.firstWithin { it.saved() } as CommunityListUiState.Success
        assertEquals(10, saved.takeCount)
    }

    private fun CommunityListUiState.saved() = (this as? CommunityListUiState.Success)?.savedId != null

    private fun viewModel(
        community: CommunityRepository = TestCommunity(),
        tiers: RecordingTierRepository = RecordingTierRepository(),
        handle: SavedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = FakeAppPreferences(),
        savedStateHandle = handle,
    )
}

private class TestCommunity(
    private val rankings: List<Ranking> = emptyList(),
    private val refusesWithdrawal: Boolean = false,
    private val takeCount: Int = 0,
) : FakeCommunityRepository() {
    val withdrawn = mutableListOf<String>()

    /** Held until the test lets the refusal through. */
    val refusal = CompletableDeferred<Unit>()

    override suspend fun open(id: String): Result<PublishedList> =
        Result.success(published.copy(summary = published.summary.copy(rankingCount = rankings.size, takeCount = takeCount)))

    override suspend fun publish(list: TierList): Result<Published> =
        Result.success(Published(id = "abc", fingerprint = "sent"))

    override suspend fun rankings(listId: String, after: String?): Result<RankingsPage> = Result.success(RankingsPage(rankings, null))

    override suspend fun withdrawRanking(listId: String): Result<Unit> {
        withdrawn += listId
        if (!refusesWithdrawal) return Result.success(Unit)
        refusal.await()
        return Result.failure(IllegalStateException("offline"))
    }
}

private class FailingCommunityRepository : FakeCommunityRepository() {
    override suspend fun makeFace(pictureId: String): Result<String> = Result.failure(IllegalStateException())
}
