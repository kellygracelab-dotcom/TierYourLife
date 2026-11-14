package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * QA Н4: after a first game the offer to show it covered the snackbar's Undo,
 * which was gone once the offer closed. The page keeps its own Undo and Redo
 * for the game until the next change.
 */
class GameStepTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(): CommunityListViewModel {
        val accounts = FakeAccountRepository()
        return CommunityListViewModel(
            community = TestCommunity(),
            tiers = RecordingTierRepository(),
            preferences = FakeAppPreferences(),
            savedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
            accounts = accounts,
            openedLists = FakeOpenedLists(),
            afterSignIn = FollowAfterSignIn(accounts),
            drafts = RankingDrafts.None,
        ).also { made += it }
    }

    private suspend fun CommunityListViewModel.loaded() =
        state.firstWithin { it is CommunityListUiState.Success && !it.arriving } as CommunityListUiState.Success

    private val CommunityListViewModel.now get() = state.value as CommunityListUiState.Success

    private fun played(loaded: CommunityListUiState.Success): TierList = loaded.mine.copy(
        tiers = loaded.mine.tiers.map { tier ->
            when {
                tier.isPool -> tier.copy(items = emptyList())
                tier.id == loaded.mine.tiers.first { !it.isPool }.id -> tier.copy(items = loaded.list.tiers.flatMap { it.items })
                else -> tier.copy(items = emptyList())
            }
        },
    )

    @Test
    fun aGame_canBeTurnedOverOnThePage_beforeAndAfter() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.loaded()
        val played = played(loaded)

        viewModel.putInMine(played)
        assertEquals(GameStep.Undo, viewModel.now.gameStep)

        viewModel.game.turn()
        assertEquals(loaded.mine, viewModel.now.mine)
        assertEquals(loaded.showing, viewModel.now.showing)
        assertEquals(loaded.arranged, viewModel.now.arranged)
        assertEquals(GameStep.Redo, viewModel.now.gameStep)

        viewModel.game.turn()
        assertEquals(played, viewModel.now.mine)
        assertEquals(Showing.Mine, viewModel.now.showing)
        assertTrue(viewModel.now.arranged)
        assertEquals(GameStep.Undo, viewModel.now.gameStep)
    }

    @Test
    fun theNextMove_endsTheGamesUndo() = runBlocking {
        val viewModel = viewModel()
        val played = played(viewModel.loaded())
        viewModel.putInMine(played)

        val card = played.tiers.first { !it.isPool }.items.first()
        viewModel.moveItem(card.id, toTierId = played.tiers.first { it.isPool }.id, toPosition = 0)
        val moved = viewModel.now.mine

        assertNull(viewModel.now.gameStep)
        viewModel.game.turn()
        assertEquals(moved, viewModel.now.mine)
    }

    @Test
    fun theGamesOwnUndo_andThePagesButton_agree() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.loaded()
        val played = played(loaded)
        viewModel.putInMine(played)

        viewModel.putInMine(played, undone = true)
        assertEquals(loaded.mine, viewModel.now.mine)
        assertEquals(GameStep.Redo, viewModel.now.gameStep)

        // Already before the game: a second Undo from the game changes nothing.
        viewModel.putInMine(played, undone = true)
        assertEquals(loaded.mine, viewModel.now.mine)

        viewModel.game.turn()
        assertEquals(played, viewModel.now.mine)
    }
}
