package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModerationViewModelTest {

    @Test
    fun theQueue_isWhatTheServerHandedBack() = runBlocking {
        val viewModel = ModerationViewModel(FakeModerationRepository(twoAboutOneList() + about("b")))

        val ready = viewModel.state.first { it is ModerationUiState.Ready } as ModerationUiState.Ready

        assertEquals(listOf("a", "b"), ready.reports.map { it.listId })
        assertEquals(2, ready.reports.first().reportCount)
    }

    // Everyone who is not the one person allowed to read these is turned away,
    // which is the same answer as the queue being unreachable.
    @Test
    fun beingTurnedAway_readsAsAFailure() = runBlocking {
        val viewModel = ModerationViewModel(FakeModerationRepository(emptyList(), readable = false))

        val reached = viewModel.state.first { it is ModerationUiState.Failed }

        assertEquals(ModerationUiState.Failed, reached)
    }

    @Test
    fun takingAListDown_clearsEveryComplaintAboutIt() = runBlocking {
        val repository = FakeModerationRepository(twoAboutOneList() + about("b"))
        val viewModel = ModerationViewModel(repository)
        viewModel.state.first { it is ModerationUiState.Ready }

        viewModel.takeDown("a")
        val left = viewModel.state.first {
            (it as? ModerationUiState.Ready)?.reports?.size == 1
        } as ModerationUiState.Ready

        assertEquals(listOf("b"), left.reports.map { it.listId })
        assertEquals(listOf("a"), repository.takenDown)
        assertTrue(repository.dismissed.isEmpty())
    }

    @Test
    fun leavingAListUp_clearsTheComplaintsWithoutRemovingIt() = runBlocking {
        val repository = FakeModerationRepository(twoAboutOneList())
        val viewModel = ModerationViewModel(repository)
        viewModel.state.first { it is ModerationUiState.Ready }

        viewModel.dismiss("a")
        viewModel.state.first { (it as? ModerationUiState.Ready)?.reports?.isEmpty() == true }

        assertEquals(listOf("a"), repository.dismissed)
        assertTrue(repository.takenDown.isEmpty())
    }

    // A queue that quietly loses entries is worse than one that will not budge.
    @Test
    fun anActionThatFails_leavesTheQueueWhereItWas() = runBlocking {
        val repository = FakeModerationRepository(twoAboutOneList(), actionsFail = true)
        val viewModel = ModerationViewModel(repository)
        viewModel.state.first { it is ModerationUiState.Ready }

        viewModel.takeDown("a")
        val still = viewModel.state.first {
            (it as? ModerationUiState.Ready)?.settling == null
        } as ModerationUiState.Ready

        assertEquals(listOf("a"), still.reports.map { it.listId })
        assertEquals(2, still.reports.single().reportCount)
    }

    // The server groups by list now, so two complaints about one board arrive
    // as one row carrying a count of two.
    private fun twoAboutOneList() = listOf(about("a", reportCount = 2))

    private fun about(listId: String, reportCount: Int = 1) = ModerationReport(
        listId = listId,
        listTitle = "A list",
        authorName = "Someone",
        reasons = List(reportCount) { ReportReason.Spam },
        notes = emptyList(),
        reportCount = reportCount,
        newestAtMillis = 0,
        hidden = false,
        reviewed = false,
    )
}

private class FakeModerationRepository(
    private val queue: List<ModerationReport>,
    private val readable: Boolean = true,
    private val actionsFail: Boolean = false,
) : CommunityRepository {
    val takenDown = mutableListOf<String>()
    val dismissed = mutableListOf<String>()

    override suspend fun reports(): Result<List<ModerationReport>> =
        if (readable) Result.success(queue) else Result.failure(IllegalStateException("not yours"))

    override suspend fun takeDown(publishedId: String): Result<Unit> {
        if (actionsFail) return Result.failure(IllegalStateException("offline"))
        takenDown += publishedId
        return Result.success(Unit)
    }

    override suspend fun dismissReports(publishedId: String): Result<Unit> {
        if (actionsFail) return Result.failure(IllegalStateException("offline"))
        dismissed += publishedId
        return Result.success(Unit)
    }

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
    ): Result<CommunityPage> = Result.success(CommunityPage(emptyList()))

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())

    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())
    override suspend fun publish(list: TierList): Result<Published> = Result.failure(IllegalStateException())
    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)
    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
}
