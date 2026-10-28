package com.artiuillab.tieryourlife.feature.community.presentation.published

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyPublishedViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun theListComesFromTheServer_notFromThisPhone() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = MyPublishedViewModel(FakePublishedRepository(listOf(mine("a"), mine("b"))), tiers)

        val ready = viewModel.state.firstWithin { it is MyPublishedUiState.Ready } as MyPublishedUiState.Ready

        assertEquals(listOf("a", "b"), ready.lists.map { it.id })
    }

    @Test
    fun takingOneDown_removesItAndForgetsTheIdLocally() = runBlocking {
        val community = FakePublishedRepository(listOf(mine("a"), mine("b")))
        val tiers = RecordingTierRepository(publishedIdOfFirstList = "a")
        val viewModel = MyPublishedViewModel(community, tiers)
        viewModel.state.firstWithin { it is MyPublishedUiState.Ready }

        viewModel.takeDown("a")
        val left = viewModel.state.firstWithin {
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
        val viewModel = MyPublishedViewModel(community, RecordingTierRepository())
        viewModel.state.firstWithin { it is MyPublishedUiState.Ready }

        viewModel.takeDown("orphan")
        val left = viewModel.state.firstWithin {
            (it as? MyPublishedUiState.Ready)?.lists?.isEmpty() == true
        } as MyPublishedUiState.Ready

        assertEquals(emptyList<String>(), left.lists.map { it.id })
        assertEquals(listOf("orphan"), community.takenDown)
    }

    @Test
    fun aTakeDownThatFails_leavesTheListWhereItWas() = runBlocking {
        val community = FakePublishedRepository(listOf(mine("a")), takeDownFails = true)
        val viewModel = MyPublishedViewModel(community, RecordingTierRepository())
        viewModel.state.firstWithin { it is MyPublishedUiState.Ready }

        viewModel.takeDown("a")
        val still = viewModel.state.firstWithin {
            (it as? MyPublishedUiState.Ready)?.removing == null
        } as MyPublishedUiState.Ready

        assertEquals(listOf("a"), still.lists.map { it.id })
    }

    @Test
    fun aTakeDownThatFails_saysSo() = runBlocking {
        val community = FakePublishedRepository(listOf(mine("a")), takeDownFails = true)
        val viewModel = MyPublishedViewModel(community, RecordingTierRepository())
        viewModel.state.firstWithin { it is MyPublishedUiState.Ready }

        viewModel.takeDown("a")

        assertEquals(UserMessage.ActionFailed, withTimeout(WAIT_MILLIS) { viewModel.userMessages.first() })
    }

    // Rebuilding the state from scratch after a take-down dropped what the
    // other rows knew, and every Update button went with it.
    @Test
    fun takingOneDown_leavesTheOthersAbleToUpdate() = runBlocking {
        val tiers = object : TierRepository by RecordingTierRepository() {
            override suspend fun publishedStanding() = PublishedStanding(canUpdate = setOf("a", "b"), knownBehind = setOf("b"))
        }
        val viewModel = MyPublishedViewModel(FakePublishedRepository(listOf(mine("a"), mine("b"))), tiers)
        viewModel.state.firstWithin { it is MyPublishedUiState.Ready }

        viewModel.takeDown("a")
        val left = viewModel.state.firstWithin {
            (it as? MyPublishedUiState.Ready)?.lists?.size == 1
        } as MyPublishedUiState.Ready

        assertEquals(setOf("b"), left.canUpdate)
        assertEquals(setOf("b"), left.behind)
    }

    private fun mine(id: String) = PublishedListSummary(
        id = id,
        title = "A list",
        authorUid = "me",
        authorName = "Olena",
        category = ListCategory.FilmTv.id,
        itemCount = 3,
        updatedAtMillis = 0,
    )
}

private class FakePublishedRepository(
    private val mine: List<PublishedListSummary>,
    private val takeDownFails: Boolean = false,
) : FakeCommunityRepository() {
    val takenDown = mutableListOf<String>()

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(mine)

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        if (takeDownFails) return Result.failure(IllegalStateException("offline"))
        takenDown += publishedId
        return Result.success(Unit)
    }
}

private const val WAIT_MILLIS = 5_000L
