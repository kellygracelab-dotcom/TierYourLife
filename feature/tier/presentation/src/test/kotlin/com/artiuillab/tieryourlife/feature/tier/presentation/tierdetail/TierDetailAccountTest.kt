package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** A board kept in step with the account, and whose copy it is. */
class TierDetailAccountTest {

    @get:Rule
    val main = MainDispatcherRule()

    // QA 484 N2: what the website did since showed only the next time the board was opened.
    @Test
    fun anOpenedBoard_runsASync_andShowsWhatARunBrought() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard())
        val sync = RecordingBoardSync()
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle(), sync)
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        sync.runs.firstWithin { it == 1 }
        sync.listening.firstWithin { it > 0 }

        repository.arrive { it.copy(title = "Sci-fi films, from the website") }
        sync.finish(SyncReport(signedIn = true, carried = 0))
        sync.finish(SyncReport(signedIn = true, carried = 1))

        val shown = viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.title == "Sci-fi films, from the website" }
        assertTrue(shown is TierDetailUiState.Success)
    }

    private fun savedStateHandle(): SavedStateHandle = SavedStateHandle(mapOf("tierListId" to 1L, "startInTitleEdit" to false))

    private fun listWithOneCard(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        category = ListCategory.FilmTv.id,
        tiers = listOf(
            Tier(
                id = 10,
                label = "Pool",
                colorLight = "#000000",
                colorDark = "#000000",
                items = listOf(TierItem(id = 100, title = "Arrival", imageUrl = null)),
                isPool = true,
            ),
        ),
    )
}

private class RecordingBoardSync : BoardSync {
    val runs = MutableStateFlow(0)
    private val reports = MutableSharedFlow<SyncReport>(extraBufferCapacity = 4)
    override val finished: Flow<SyncReport> = reports
    val listening = reports.subscriptionCount

    override suspend fun sync(): SyncReport {
        runs.update { it + 1 }
        return SyncReport(signedIn = true)
    }

    fun finish(report: SyncReport) {
        reports.tryEmit(report)
    }
}
