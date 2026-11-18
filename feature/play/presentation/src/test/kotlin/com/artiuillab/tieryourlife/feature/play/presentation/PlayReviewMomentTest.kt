package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.settings.ReviewMoments
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** A game played through counts as a finished list once its window is closed; a game left halfway does not. */
class PlayReviewMomentTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val unranked = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList())

    private class Recorded : ReviewMoments {
        val finished = mutableListOf<Int>()

        override fun listFinished(cards: Int) {
            finished += cards
        }

        override val asks: Flow<Unit> = emptyFlow()
    }

    @Test
    fun aFinishedGame_countsWhenItsWindowCloses() = runBlocking {
        val moments = Recorded()
        val viewModel = viewModel(moments)
        viewModel.open()
        viewModel.picker.start()
        while (viewModel.state.value.stage is PlayStage.Dueling) viewModel.duel.pick(first = true)
        viewModel.state.firstWithin { it.stage is PlayStage.Ready }
        assertEquals(emptyList<Int>(), moments.finished)

        viewModel.ready.dismiss()

        assertEquals(listOf(6), moments.finished)
    }

    @Test
    fun aGameLeftHalfway_doesNot() = runBlocking {
        val moments = Recorded()
        val viewModel = viewModel(moments)
        viewModel.open()
        viewModel.picker.start()
        viewModel.duel.pick(first = true)

        viewModel.duel.quit()

        assertEquals(emptyList<Int>(), moments.finished)
    }

    private fun viewModel(moments: ReviewMoments) =
        PlayViewModel(FakeSavedGames(), MovingTierRepository(unranked), reviewMoments = moments).apply {
            clock = { 1_800_000_000_000L }
            seeds = { 7 }
            offer(PlayRequest.ownBoard(unranked))
        }
}
