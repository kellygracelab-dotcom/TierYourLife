package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real Compose gestures over the same ViewModel/engine as the app, with synthetic local data only. */
class LargeGameJourneyTest {
    @get:Rule
    val compose = createComposeRule()
    private lateinit var vm: PlayViewModel
    private var board = wholeBoard()
    private val games = FakeSavedGames()

    @After
    fun finish() {
        if (::vm.isInitialized) vm.viewModelScope.cancel()
    }

    @Test
    fun ninetyOneCardsSampleSixteen_keepInS_thenApplyLeavesEveryCardOnce() {
        open()
        compose.runOnIdle { vm.picker.showCards() }
        compose.onNodeWithTag("play_sample_16").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(16, picking().count) }
        compose.onNodeWithTag("play_individual_cards").performScrollTo().performClick()
        compose.onNodeWithTag("play_individual_list").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(16, picking().selectedCards.map { it.item.id }.toSet().size)
            vm.picker.showGames()
            vm.picker.choose(GameKind.Hill)
        }
        compose.onNodeWithTag(PlayTestTags.START).performClick()
        repeat(15) { compose.onNodeWithTag(PlayTestTags.FIRST_CARD).performClick() }
        compose.onNodeWithTag(PlayTestTags.RESULT).assertIsDisplayed()
        compose.onNodeWithTag("play_unplayed_Keep").assertIsDisplayed()
        compose.runOnIdle {
            val result = (vm.state.value.stage as PlayStage.Finished).result
            assertEquals(75, result.unplayedCount)
            assertEquals(listOf(1L), result.tiers.map { it.tierId })
            assertEquals(91, board.tiers.first().items.size)
        }
        compose.onNodeWithTag(PlayTestTags.APPLY).performClick()
        compose.waitUntil(10_000) { board.tiers.first().items.size == 91 && games.games.isEmpty() }
        compose.runOnIdle {
            assertEquals((1L..91L).toSet(), board.tiers.flatMap { it.items }.map { it.id }.toSet())
            assertTrue(board.tiers.first { it.isPool }.items.isEmpty())
        }
    }

    @Test
    fun allNinetyOneCardsPlayAndFinish_withoutASelectionCeiling() {
        open()
        compose.runOnIdle {
            assertEquals(91, picking().count)
            vm.picker.choose(GameKind.Bracket)
        }
        compose.onNodeWithTag(PlayTestTags.START).performClick()
        repeat(90) { compose.onNodeWithTag(PlayTestTags.FIRST_CARD).performClick() }
        compose.onNodeWithTag(PlayTestTags.RESULT).assertIsDisplayed()
        compose.runOnIdle {
            val result = (vm.state.value.stage as PlayStage.Finished).result
            assertEquals(91, result.tiers.flatMap { it.cards }.size)
            assertEquals(0, result.unplayedCount)
        }
    }

    private fun picking() = (vm.state.value.stage as PlayStage.Picking).picker

    private fun open() {
        val repository = object : TierRepository by RecordingTierRepository() {
            override suspend fun getTierListById(id: Long) = board
            override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
                board = board.withItemMoved(itemId, toTierId, toPosition)
            }
            override suspend fun setPlay(id: Long, play: String?) {
                board = board.copy(play = play)
            }
            override suspend fun arrangeBoard(id: Long, expected: Map<Long, List<Long>>, target: Map<Long, List<Long>>): Boolean {
                val cards = board.tiers.flatMap { it.items }.associateBy { it.id }
                board = board.copy(tiers = board.tiers.map { it.copy(items = target.getValue(it.id).map(cards::getValue)) })
                return true
            }
        }
        compose.setContent {
            var open by androidx.compose.runtime.remember { mutableStateOf(true) }
            val viewModel = androidx.compose.runtime.remember { PlayViewModel(games, repository).also { vm = it } }
            TierYourLifeTheme {
                PlayLayer(PlayRequest.ownBoard(board), open, onArranged = { arranged, _ -> board = arranged }, onClose = { open = false }, viewModel = viewModel)
            }
        }
        compose.waitUntil(10_000) { vm.state.value.stage is PlayStage.Picking }
    }

    private fun wholeBoard() = TierList(
        1, "QA 91 cards",
        listOf(
            Tier(1, "S", "#B03A32", "#F1948C", (1L..91L).map { TierItem(it, "Card $it", null, uid = "card-$it") }, uid = "tier-s"),
            Tier(2, "Pool", "#DAD7E0", "#46464F", emptyList(), isPool = true, uid = "tier-pool"),
        ),
        uid = "qa-91",
    )
}
