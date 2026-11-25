package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
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

/**
 * The reader's version put in place in one step, never asked about first, always with Undo:
 * a move on someone else's ranking before there is a version, their ranking again, and a fresh start.
 */
class TheirsStartTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    // A move on the author's arrangement starts the reader's own version from
    // what they were looking at, with that one move made.
    @Test
    fun movingACardOnTheirs_startsMineFromTheirs_withThatMove_andSwitchesToMine() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        assertEquals(Showing.Theirs, loaded.showing)

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, after.showing)
        assertTrue(after.arranged)
        assertEquals(listOf("Hereditary"), after.mine.tiers.first { it.id == 0L }.items.map { it.title })
        assertEquals(listOf("Moonlight"), after.mine.tiers.first { it.id == 1L }.items.map { it.title })
        assertEquals(loaded.list, after.list)
        assertEquals(ListNotice.StartedFromTheirs("Olena M.", replaced = false), withTimeout(5_000) { viewModel.listNotices.first() })
    }

    // Danylo 16.11: back to the pool is a move too, the same as into a tier.
    @Test
    fun aCardBackToThePoolOnTheirs_startsMine_too() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val pool = loaded.mine.tiers.first { it.isPool }.id

        viewModel.moveItem(itemId = 0, toTierId = pool, toPosition = 0)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, after.showing)
        assertTrue(after.mine.tiers.filterNot { it.isPool }.all { it.items.isEmpty() })
        assertTrue("Hereditary" in after.mine.tiers.first { it.isPool }.items.map { it.title })
    }

    // Danylo 25.11: with a version of one's own, theirs is only to look at; nothing on it moves.
    @Test
    fun lookingAtTheirsOverMine_isReadOnly_andMineWaitsAsItWas() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine
        viewModel.show(Showing.Theirs)
        val looking = viewModel.state.value as CommunityListUiState.Success
        assertTrue(looking.peeking)
        assertTrue(looking.readOnly)

        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)

        assertEquals(looking, viewModel.state.value)
        viewModel.show(Showing.Mine)
        assertEquals(mine, (viewModel.state.value as CommunityListUiState.Success).mine)
    }

    @Test
    fun undo_afterStartingFromTheirs_putsMineBack_andTheirsOnScreen() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        viewModel.theirsStart.undo()

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Theirs, after.showing)
        assertEquals(loaded.mine, after.mine)
        assertFalse(after.arranged)
    }

    @Test
    fun aLaterMoveInMineInvalidatesTheEarlierStartUndoInsteadOfLosingTheNewMove() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)
        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)
        val newer = viewModel.state.value as CommunityListUiState.Success

        viewModel.theirsStart.undo()

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(newer.mine, after.mine)
        assertEquals(newer.showing, after.showing)
    }

    @Test
    fun backToTheirs_putsTheirRankingInMine_withoutAsking_andUndoBringsMineBack() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine

        viewModel.theirsStart.backToTheirs()

        val back = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, back.showing)
        assertEquals(listOf("Hereditary"), back.mine.tiers.first { it.id == 0L }.items.map { it.title })
        assertTrue(back.mine.tiers.first { it.id == 1L }.items.isEmpty())
        val notice = viewModel.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        assertEquals("Olena M.", notice.name)
        assertTrue(notice.replaced)

        viewModel.theirsStart.undo(notice.undoId)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(mine, after.mine)
        assertTrue(after.arranged)
        assertEquals(Showing.Mine, after.showing)
    }

    // From their ranking on screen, the way back puts it in place, and the reader is on their own version again.
    @Test
    fun backToTheirs_whileLookingAtIt_endsOnMine() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        arranged(viewModel)
        viewModel.show(Showing.Theirs)

        viewModel.theirsStart.backToTheirs()

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, after.showing)
        assertFalse(after.peeking)
        val notice = viewModel.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        viewModel.theirsStart.undo(notice.undoId)
        assertEquals(Showing.Theirs, (viewModel.state.value as CommunityListUiState.Success).showing)
    }

    @Test
    fun startFromScratch_sendsEveryCardToThePool_withoutAsking_andUndoBringsThemBack() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine

        viewModel.theirsStart.startFromScratch()

        val fresh = viewModel.state.value as CommunityListUiState.Success
        assertFalse(fresh.minePlaced)
        assertTrue(fresh.arranged)
        assertEquals(fresh.list, fresh.theirs)
        val notice = viewModel.listNotices.firstWithin { it is ListNotice.StartedFromScratch } as ListNotice.StartedFromScratch

        viewModel.theirsStart.undo(notice.undoId)

        assertEquals(mine, (viewModel.state.value as CommunityListUiState.Success).mine)
    }

    @Test
    fun startFromScratch_withNothingPlaced_changesNothing() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        viewModel.theirsStart.startFromScratch()

        assertEquals(loaded, viewModel.state.value)
    }

    // Theirs is whoever's placing is on screen: a chosen reader's is the one copied, under their words.
    @Test
    fun movingOnAChosenReadersRanking_startsMineFromTheirs() = runBlocking {
        val kyle = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)
        val viewModel = viewModel(community = TestCommunity(rankings = listOf(kyle), arrangement = listOf(0, null)))
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(kyle)

        viewModel.moveItem(itemId = 0, toTierId = 0, toPosition = 1)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, after.showing)
        assertEquals(listOf("Top", "Next"), after.mine.tiers.filterNot { it.isPool }.map { it.label })
        assertEquals(listOf("Moonlight", "Hereditary"), after.mine.tiers.first { it.id == 0L }.items.map { it.title })
        assertEquals("Kyle H.", (withTimeout(5_000) { viewModel.listNotices.first() } as ListNotice.StartedFromTheirs).name)
    }

    @Test
    fun backToAChosenReadersRanking_isTheirs_byName() = runBlocking {
        val kyle = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)
        val viewModel = viewModel(community = TestCommunity(rankings = listOf(kyle), arrangement = listOf(0, null)))
        arranged(viewModel)
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(kyle)
        assertTrue((viewModel.state.value as CommunityListUiState.Success).peeking)

        viewModel.theirsStart.backToTheirs()

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(listOf("Moonlight", "Hereditary").toSet(), after.mine.tiers.filterNot { it.isPool }.flatMap { it.items }.map { it.title }.toSet())
        val notice = viewModel.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        assertEquals("Kyle H.", notice.name)
    }

    @Test
    fun anAnonymousAuthorsArrangement_isStartedFromWithoutAName() = runBlocking {
        val community = TestCommunity(arrangement = listOf(0, null), summary = { it.copy(authorUid = "", authorName = "", anonymous = true) })
        val viewModel = viewModel(community = community)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        assertEquals(ListNotice.StartedFromTheirs(null, replaced = false), withTimeout(5_000) { viewModel.listNotices.first() })
    }

    @Test
    fun anOldActionOrSnackbarCompletionCannotConsumeTheNewUndo() = runBlocking {
        val vm = viewModel(TestCommunity(arrangement = listOf(0, null)))
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.moveItem(1, 1, 0)
        val old = vm.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        val firstMine = (vm.state.value as CommunityListUiState.Success).mine
        vm.theirsStart.startFromScratch()
        val next = vm.listNotices.firstWithin { it is ListNotice.StartedFromScratch } as ListNotice.StartedFromScratch
        val newer = vm.state.value

        vm.theirsStart.forget(old.undoId)
        vm.theirsStart.undo(old.undoId)
        assertEquals(newer, vm.state.value)
        vm.theirsStart.undo(next.undoId)
        assertEquals(firstMine, (vm.state.value as CommunityListUiState.Success).mine)
        val undone = vm.state.value
        vm.theirsStart.undo(next.undoId)
        assertEquals(undone, vm.state.value)
    }

    private fun viewModel(community: CommunityRepository) = CommunityListViewModel(
        community, RecordingTierRepository(), FakeAppPreferences(), SavedStateHandle(mapOf("publishedId" to "abc")),
        FakeAccountRepository(), FakeOpenedLists(), FollowAfterSignIn(FakeAccountRepository()), RankingDrafts.None,
    ).also { made += it }
}
