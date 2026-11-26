package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Delete list from List settings: into the trash as from Your lists, and down from Community first. */
class DeleteListTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun aPrivateList_goesToTheTrash_andTheScreenLeaves() = runBlocking {
        val repository = FakeTierRepository(list())
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = viewModel(repository, community)

        val gone = CompletableDeferred<Unit>()
        viewModel.deleteList { gone.complete(Unit) }
        withTimeout(TIMEOUT) { gone.await() }

        assertEquals(listOf(1L), repository.deletedLists)
        assertTrue(community.unpublished.isEmpty())
    }

    @Test
    fun aPublishedList_comesDownFirst_thenGoesToTheTrash() = runBlocking {
        val repository = FakeTierRepository(list(), publishedId = "published-1")
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = viewModel(repository, community)

        val gone = CompletableDeferred<Unit>()
        viewModel.deleteList { gone.complete(Unit) }
        withTimeout(TIMEOUT) { gone.await() }

        assertEquals(listOf("published-1"), community.unpublished)
        assertEquals(1L to null, repository.publishedIds.last())
        assertEquals(listOf(1L), repository.deletedLists)
    }

    @Test
    fun aListThatCannotComeDown_staysHere_andSaysSo() = runBlocking {
        val repository = FakeTierRepository(list(), publishedId = "published-1")
        val community = FakeCommunityRepositoryForDetail(unpublishResult = Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel(repository, community)
        var left = false

        viewModel.deleteList { left = true }

        assertEquals(UserMessage.PublishedListStillPublic, withTimeout(TIMEOUT) { viewModel.userMessages.first() })
        assertTrue(repository.deletedLists.isEmpty())
        assertFalse(left)
    }

    private suspend fun viewModel(repository: FakeTierRepository, community: FakeCommunityRepositoryForDetail): TierDetailViewModel {
        val viewModel = TierDetailViewModel(
            repository,
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            SavedStateHandle(mapOf("tierListId" to 1L, "startInTitleEdit" to false)),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        return viewModel
    }

    private fun list(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
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

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
