package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.summary
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    // QA 484 N3: a copy of an editorial list said "by Author"; the website names nobody there.
    @Test
    fun aCopyThatNamesNobody_learnsWhetherItsOriginalStandsWithoutAnAuthor() = runBlocking {
        val summary = PublishedListSummary("source", "Animals", "someone", "", category = "animals", itemCount = 1, updatedAtMillis = 1)
        for ((original, editorial) in listOf(summary.copy(anonymous = true) to true, summary to false)) {
            val community = FakeCommunityRepositoryForDetail(original = PublishedList(original, tiers = emptyList(), items = emptyList()))
            val copy = listWithOneCard().copy(takenFrom = "source", authorName = "")
            val viewModel = TierDetailViewModel(FakeTierRepository(copy), community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())

            assertEquals(editorial, viewModel.screen.firstWithin { it.originalEditorial != null }.originalEditorial)
            assertEquals(listOf("source"), community.opened)
        }
        val named = FakeCommunityRepositoryForDetail(original = PublishedList(summary, tiers = emptyList(), items = emptyList()))
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(takenFrom = "source", authorName = "Olena")),
            named,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        assertEquals(emptyList<String>(), named.opened)
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
