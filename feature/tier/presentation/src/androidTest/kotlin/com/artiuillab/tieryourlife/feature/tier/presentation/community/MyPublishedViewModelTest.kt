package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyPublishedViewModelTest {

    @Test
    fun theListComesFromTheServer_notFromThisPhone() = runBlocking {
        val tiers = FakeTierRepositoryForCommunity()
        val viewModel = MyPublishedViewModel(FakePublishedRepository(listOf(mine("a"), mine("b"))), tiers)

        val ready = viewModel.state.first { it is MyPublishedUiState.Ready } as MyPublishedUiState.Ready

        assertEquals(listOf("a", "b"), ready.lists.map { it.id })
    }

    @Test
    fun takingOneDown_removesItAndForgetsTheIdLocally() = runBlocking {
        val community = FakePublishedRepository(listOf(mine("a"), mine("b")))
        val tiers = FakeTierRepositoryForCommunity(publishedIdOfFirstList = "a")
        val viewModel = MyPublishedViewModel(community, tiers)
        viewModel.state.first { it is MyPublishedUiState.Ready }

        viewModel.takeDown("a")
        val left = viewModel.state.first {
            (it as? MyPublishedUiState.Ready)?.lists?.size == 1
        } as MyPublishedUiState.Ready

        assertEquals(listOf("b"), left.lists.map { it.id })
        assertEquals(listOf("a"), community.takenDown)
        assertEquals(7L, tiers.clearedPublishedId)
    }

    // A list published from a phone that is gone has no local copy to update.
    @Test
    fun takingDownSomethingThisPhoneNeverHad_stillWorks() = runBlocking {
        val community = FakePublishedRepository(listOf(mine("orphan")))
        val viewModel = MyPublishedViewModel(community, FakeTierRepositoryForCommunity())
        viewModel.state.first { it is MyPublishedUiState.Ready }

        viewModel.takeDown("orphan")
        val left = viewModel.state.first {
            (it as? MyPublishedUiState.Ready)?.lists?.isEmpty() == true
        } as MyPublishedUiState.Ready

        assertEquals(emptyList<String>(), left.lists.map { it.id })
        assertEquals(listOf("orphan"), community.takenDown)
    }

    @Test
    fun aTakeDownThatFails_leavesTheListWhereItWas() = runBlocking {
        val community = FakePublishedRepository(listOf(mine("a")), takeDownFails = true)
        val viewModel = MyPublishedViewModel(community, FakeTierRepositoryForCommunity())
        viewModel.state.first { it is MyPublishedUiState.Ready }

        viewModel.takeDown("a")
        val still = viewModel.state.first {
            (it as? MyPublishedUiState.Ready)?.removing == null
        } as MyPublishedUiState.Ready

        assertEquals(listOf("a"), still.lists.map { it.id })
    }

    private fun mine(id: String) = PublishedListSummary(
        id = id,
        title = "A list",
        authorUid = "me",
        authorName = "Olena",
        category = ListCategory.FilmTv,
        itemCount = 3,
        updatedAtMillis = 0,
    )
}

private class FakePublishedRepository(
    private val mine: List<PublishedListSummary>,
    private val takeDownFails: Boolean = false,
) : CommunityRepository {
    val takenDown = mutableListOf<String>()

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(mine)

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        if (takeDownFails) return Result.failure(IllegalStateException("offline"))
        takenDown += publishedId
        return Result.success(Unit)
    }

    override suspend fun makeFace(pictureId: String): Result<String> =
        Result.success("https://example.test/face.jpg")

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> = Result.success(CommunityPage(emptyList()))

    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())
    override suspend fun publish(list: TierList): Result<Published> = Result.failure(IllegalStateException())
    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)
    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun reports(): Result<List<ModerationReport>> = Result.failure(IllegalStateException())
    override suspend fun takeDown(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun follow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(emptyList())
    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)
}
