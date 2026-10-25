package com.artiuillab.tieryourlife.feature.community.presentation.following

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val ann = FollowedAuthor(uid = "a", name = "Ann", photoUrl = null)
private val bea = FollowedAuthor(uid = "b", name = "Bea", photoUrl = "https://example.test/bea.jpg")

class FollowingViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun thePeopleFollowed_areListed() = runBlocking {
        val viewModel = FollowingViewModel(FollowingFake(listOf(ann, bea)))

        val ready = viewModel.state.firstWithin { it is FollowingUiState.Ready } as FollowingUiState.Ready

        assertEquals(listOf(ann, bea), ready.authors)
    }

    @Test
    fun unfollowing_removesThePersonAtOnce_andTellsTheServer() = runBlocking {
        val community = FollowingFake(listOf(ann, bea))
        val viewModel = FollowingViewModel(community)
        viewModel.state.firstWithin { it is FollowingUiState.Ready }

        viewModel.unfollow("a")

        assertEquals(listOf(bea), (viewModel.state.value as FollowingUiState.Ready).authors)
        assertEquals("a", withTimeout(WAIT_MILLIS) { community.unfollowAsked.await() })
    }

    // The button answers before the server does; a refusal has to undo that, and in the same place.
    @Test
    fun aRefusedUnfollow_putsThePersonBackWhereTheyWere() = runBlocking {
        val viewModel = FollowingViewModel(FollowingFake(listOf(ann, bea), refuses = true))
        viewModel.state.firstWithin { it is FollowingUiState.Ready }

        viewModel.unfollow("a")

        val back = viewModel.state.firstWithin { (it as? FollowingUiState.Ready)?.authors?.size == 2 } as FollowingUiState.Ready
        assertEquals(listOf(ann, bea), back.authors)
    }

    @Test
    fun whenTheListDoesNotArrive_theScreenSaysSo() = runBlocking {
        val viewModel = FollowingViewModel(FollowingFake(fails = true))

        assertEquals(FollowingUiState.Failed, viewModel.state.firstWithin { it is FollowingUiState.Failed })
    }
}

private class FollowingFake(
    private val people: List<FollowedAuthor> = emptyList(),
    private val refuses: Boolean = false,
    private val fails: Boolean = false,
) : FakeCommunityRepository() {
    val unfollowAsked = CompletableDeferred<String>()

    override suspend fun following(): Result<List<FollowedAuthor>> =
        if (fails) Result.failure(IllegalStateException("offline")) else Result.success(people)

    override suspend fun unfollow(authorUid: String): Result<Unit> {
        unfollowAsked.complete(authorUid)
        return if (refuses) Result.failure(IllegalStateException("offline")) else Result.success(Unit)
    }
}

private const val WAIT_MILLIS = 5_000L
