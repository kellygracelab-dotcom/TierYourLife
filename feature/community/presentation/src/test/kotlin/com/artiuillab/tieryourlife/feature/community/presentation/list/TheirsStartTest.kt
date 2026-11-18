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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** A move on someone else's ranking: Mine starts from it, and one's own version is asked about first. */
class TheirsStartTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    // A move on the author's arrangement used to be ignored. Now it starts the
    // reader's own version from what they were looking at, with that one move made.
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

    @Test
    fun aCardBackToThePoolOnTheirs_overAnArrangedMine_asksFirst() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine
        viewModel.show(Showing.Theirs)

        viewModel.moveItem(itemId = 0, toTierId = mine.tiers.first { it.isPool }.id, toPosition = 0)

        val asking = viewModel.state.value as CommunityListUiState.Success
        assertEquals(ReplaceAsk("Olena M."), asking.replaceAsk)
        assertEquals(mine, asking.mine)
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

    // Danylo 16.11, as on the web: one's own version is asked about first, every time.
    @Test
    fun aMoveOnTheirsOverAnArrangedMine_asksFirst_andChangesNothingYet() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine
        viewModel.show(Showing.Theirs)

        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)

        val asking = viewModel.state.value as CommunityListUiState.Success
        assertEquals(ReplaceAsk("Olena M."), asking.replaceAsk)
        assertEquals(mine, asking.mine)
        assertEquals(Showing.Theirs, asking.showing)
    }

    @Test
    fun cancel_leavesTheirsAsItWas_andTheNextMoveAsksAgain() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine
        viewModel.show(Showing.Theirs)
        val theirs = (viewModel.state.value as CommunityListUiState.Success).theirs

        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)
        viewModel.theirsStart.keep()

        val after = viewModel.state.value as CommunityListUiState.Success
        assertNull(after.replaceAsk)
        assertEquals(mine, after.mine)
        assertEquals(theirs, after.theirs)
        assertEquals(Showing.Theirs, after.showing)

        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)
        assertEquals(ReplaceAsk("Olena M."), (viewModel.state.value as CommunityListUiState.Success).replaceAsk)
    }

    @Test
    fun aMineWithNothingInItsTiers_isReplacedWithoutAsking() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = arranged(viewModel)
        viewModel.moveItem(itemId = 1, toTierId = loaded.mine.tiers.first { it.isPool }.id, toPosition = 0)
        viewModel.show(Showing.Theirs)

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertNull(after.replaceAsk)
        assertEquals(Showing.Mine, after.showing)
        assertEquals(ListNotice.StartedFromTheirs("Olena M.", replaced = false), withTimeout(5_000) { viewModel.listNotices.first() })
    }

    @Test
    fun replace_startsMineFromTheirs_saysItReplacedIt_andUndoBringsItBack() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine
        viewModel.show(Showing.Theirs)

        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)
        viewModel.theirsStart.replace()
        val replaced = viewModel.state.value as CommunityListUiState.Success
        assertNull(replaced.replaceAsk)
        assertEquals(Showing.Mine, replaced.showing)
        assertEquals(listOf("Hereditary"), replaced.mine.tiers.first { it.id == 1L }.items.map { it.title })
        val notice = withTimeout(5_000) { viewModel.listNotices.first() }
        viewModel.theirsStart.undo()

        assertEquals(ListNotice.StartedFromTheirs("Olena M.", replaced = true), notice)
        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(mine, after.mine)
        assertTrue(after.arranged)
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
    fun anAnonymousAuthorsArrangement_isStartedFromWithoutAName() = runBlocking {
        val community = TestCommunity(arrangement = listOf(0, null), summary = { it.copy(authorUid = "", authorName = "", anonymous = true) })
        val viewModel = viewModel(community = community)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        assertEquals(ListNotice.StartedFromTheirs(null, replaced = false), withTimeout(5_000) { viewModel.listNotices.first() })
    }

    @Test
    fun anExpiredUndoDoesNotEraseTheMoveAwaitingReplacement() = runBlocking {
        val vm = viewModel(TestCommunity(arrangement = listOf(0, null)))
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.moveItem(1, 1, 0)
        val old = vm.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        val firstMine = (vm.state.value as CommunityListUiState.Success).mine
        vm.show(Showing.Theirs)
        vm.moveItem(0, 1, 0)

        vm.theirsStart.forget(old.undoId)
        assertEquals(ReplaceAsk("Olena M."), (vm.state.value as CommunityListUiState.Success).replaceAsk)
        vm.theirsStart.replace()

        val after = vm.state.value as CommunityListUiState.Success
        assertNull(after.replaceAsk)
        assertEquals(Showing.Mine, after.showing)
        assertEquals(listOf(0L), after.mine.tiers.first { it.id == 1L }.items.map { it.id })
        val next = vm.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        vm.theirsStart.undo(next.undoId)
        assertEquals(firstMine, (vm.state.value as CommunityListUiState.Success).mine)
    }

    @Test
    fun anExpiredUndoWhileReplacementIsOpenStillAllowsCancelAndAnotherMove() = runBlocking {
        val vm = viewModel(TestCommunity(arrangement = listOf(0, null)))
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.moveItem(1, 1, 0)
        val old = vm.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        val firstMine = (vm.state.value as CommunityListUiState.Success).mine
        vm.show(Showing.Theirs)
        vm.moveItem(0, 1, 0)

        vm.theirsStart.forget(old.undoId)
        vm.theirsStart.keep()
        val kept = vm.state.value as CommunityListUiState.Success
        assertNull(kept.replaceAsk)
        assertEquals(firstMine, kept.mine)
        assertEquals(Showing.Theirs, kept.showing)
        vm.moveItem(0, 1, 0)
        assertEquals(ReplaceAsk("Olena M."), (vm.state.value as CommunityListUiState.Success).replaceAsk)
        vm.theirsStart.replace()
        assertEquals(listOf(0L), (vm.state.value as CommunityListUiState.Success).mine.tiers.first { it.id == 1L }.items.map { it.id })
    }

    @Test
    fun anOldActionOrSnackbarCompletionCannotConsumeTheNewUndo() = runBlocking {
        val vm = viewModel(TestCommunity(arrangement = listOf(0, null)))
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.moveItem(1, 1, 0)
        val old = vm.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
        val firstMine = (vm.state.value as CommunityListUiState.Success).mine
        vm.show(Showing.Theirs)
        vm.moveItem(0, 1, 0)
        vm.theirsStart.replace()
        val next = vm.listNotices.firstWithin { it is ListNotice.StartedFromTheirs } as ListNotice.StartedFromTheirs
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
