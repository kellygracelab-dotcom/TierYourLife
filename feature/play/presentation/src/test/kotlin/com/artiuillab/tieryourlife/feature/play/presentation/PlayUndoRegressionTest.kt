package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PlayUndoRegressionTest {
    @get:Rule val main = MainDispatcherRule()
    private val made = mutableListOf<PlayViewModel>()
    private val original = board(1L to emptyList(), 2L to emptyList(), POOL to listOf(1L, 2L, 3L, 4L))

    @After fun stop() = made.forEach { it.viewModelScope.cancel() }

    @Test fun aRefusedUndoReportsFailureWithoutAnnouncingAnArrangementOrChangingTheBoard() = runBlocking {
        val boards = UndoBoards(MovingTierRepository(original))
        val vm = finished(boards)
        val played = boards.inner.board.ids()
        boards.refuse = true

        vm.notices.undoArrange()

        assertEquals(PlayEvent.Said(PlayNotice.Failed), vm.events.firstWithin { it is PlayEvent.Said })
        assertNull(withTimeoutOrNull(150) { vm.events.first { it is PlayEvent.Arranged } })
        assertEquals(played, boards.inner.board.ids())
        boards.refuse = false
        vm.notices.undoArrange()
        assertTrue((vm.events.firstWithin { it is PlayEvent.Arranged } as PlayEvent.Arranged).undone)
        assertEquals(original.ids(), boards.inner.board.ids())
    }

    @Test fun aFailedCommitCannotSendThePretendRestoredBoardToTheScreen() = runBlocking {
        val boards = UndoBoards(MovingTierRepository(original))
        val vm = finished(boards)
        val played = boards.inner.board.ids()
        boards.fail = true

        vm.notices.undoArrange()

        assertEquals(PlayEvent.Said(PlayNotice.Failed), vm.events.firstWithin { it is PlayEvent.Said })
        assertNull(withTimeoutOrNull(150) { vm.events.first { it is PlayEvent.Arranged } })
        assertEquals(played, boards.inner.board.ids())
    }

    @Test fun rapidAndRepeatedUndoUsesOneTransactionUntilAnExplicitRedo() = runBlocking {
        val boards = UndoBoards(MovingTierRepository(original))
        val vm = finished(boards)
        val played = boards.inner.board.ids()
        boards.hold = true
        vm.notices.undoArrange()
        withTimeout(5_000) { boards.started.await() }
        vm.notices.undoArrange()
        boards.release.complete(Unit)
        vm.events.firstWithin { it is PlayEvent.Arranged }
        vm.events.firstWithin { it is PlayEvent.Said }
        withContext(Dispatchers.Main) { }
        assertEquals(1, boards.undoCommits)
        assertEquals(original.ids(), boards.inner.board.ids())

        vm.notices.undoArrange()
        withContext(Dispatchers.Main) { }
        assertEquals(1, boards.undoCommits)
        vm.notices.redoArrange()
        vm.events.firstWithin { it is PlayEvent.Arranged }
        assertEquals(2, boards.undoCommits)
        assertEquals(played, boards.inner.board.ids())
    }

    @Test fun aLaterMoveIsNotOverwrittenByTheOldGameUndo() = runBlocking {
        val boards = UndoBoards(MovingTierRepository(original))
        val vm = finished(boards)
        val id = boards.inner.board.tiers.first { !it.isPool && it.items.isNotEmpty() }.items.first().id
        boards.inner.board = boards.inner.board.withItemMoved(id, POOL, 0)
        val newer = boards.inner.board.ids()

        vm.notices.undoArrange()

        assertEquals(PlayEvent.Said(PlayNotice.Failed), vm.events.firstWithin { it is PlayEvent.Said })
        assertEquals(newer, boards.inner.board.ids())
        assertEquals(0, boards.undoCommits)
    }

    private suspend fun finished(boards: UndoBoards): PlayViewModel {
        val vm = PlayViewModel(FakeSavedGames(), boards).apply {
            seeds = { 7 }
            offer(PlayRequest.ownBoard(original))
        }
        made += vm
        vm.open()
        vm.picker.choose(GameKind.Hill)
        vm.picker.start()
        while (vm.state.value.stage is PlayStage.Dueling) vm.duel.pick(first = true)
        vm.state.firstWithin { it.stage is PlayStage.Ready }
        vm.events.firstWithin { it is PlayEvent.Arranged }
        vm.ready.dismiss()
        vm.events.firstWithin { it is PlayEvent.Said }
        boards.applying = false
        return vm
    }

    private class UndoBoards(val inner: MovingTierRepository) : TierRepository by inner {
        var applying = true
        var refuse = false
        var fail = false
        var hold = false
        var undoCommits = 0
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override suspend fun arrangeBoard(id: Long, expected: Map<Long, List<Long>>, target: Map<Long, List<Long>>): Boolean {
            if (!applying) {
                undoCommits++
                if (fail) throw IOException("Synthetic failed Undo commit")
                if (refuse) return false
                if (hold) {
                    started.complete(Unit)
                    release.await()
                }
            }
            return inner.arrangeBoard(id, expected, target)
        }
    }
}
