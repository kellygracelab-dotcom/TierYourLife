package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.usage.GameMode
import com.artiuillab.tieryourlife.core.usage.RecordingUsage
import com.artiuillab.tieryourlife.core.usage.UsageStep
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The statistics' games: started and played to the result, by mode; the funnel's first game and full list. */
class PlayUsageTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val unranked = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList())

    @Test
    fun aGamePlayedThrough_countsItsStartItsResultAndTheFunnel() = runBlocking {
        val usage = RecordingUsage()
        val viewModel = viewModel(usage)
        viewModel.open()
        viewModel.picker.start()
        while (viewModel.state.value.stage is PlayStage.Dueling) viewModel.duel.pick(first = true)
        viewModel.state.firstWithin { it.stage is PlayStage.Ready }

        viewModel.ready.dismiss()

        val mode = usage.counted.first().removePrefix("game.").substringBefore('.')
        assertEquals(listOf("game.$mode.start", "game.$mode.done"), usage.counted)
        assertEquals(listOf(UsageStep.FirstGame, UsageStep.FullList), usage.steps)
    }

    @Test
    fun aGameLeftHalfway_isAStartOnly() = runBlocking {
        val usage = RecordingUsage()
        val viewModel = viewModel(usage)
        viewModel.open()
        viewModel.picker.start()
        viewModel.duel.pick(first = true)

        viewModel.duel.quit()

        assertEquals(1, usage.counted.size)
        assertEquals(emptyList<UsageStep>(), usage.steps)
    }

    @Test
    fun theModes_areThePickersNames() {
        assertEquals(
            listOf(GameMode.King, GameMode.Bracket, GameMode.Losers, GameMode.Smart),
            listOf(GameKind.Hill, GameKind.Bracket, GameKind.Swiss, GameKind.Smart).map { it.mode },
        )
    }

    private fun viewModel(usage: RecordingUsage) =
        PlayViewModel(FakeSavedGames(), MovingTierRepository(unranked), usage = usage).apply {
            clock = { 1_800_000_000_000L }
            seeds = { 7 }
            offer(PlayRequest.ownBoard(unranked))
        }
}
