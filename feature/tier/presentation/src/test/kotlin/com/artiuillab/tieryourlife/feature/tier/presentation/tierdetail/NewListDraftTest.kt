package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class NewListDraftTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun backFromANewListLeftAsItCame_removesIt_thenGoesBack() = runBlocking {
        val repository = FakeTierRepository(untitled())
        val viewModel = opened(repository, fresh = true)

        val left = CompletableDeferred<Unit>()
        viewModel.leave { left.complete(Unit) }
        withTimeout(5_000) { left.await() }

        assertEquals(listOf("discarded 1"), repository.drafts)
    }

    @Test
    fun theFirstRealChange_keepsTheNewList_andBackLeavesItBe() = runBlocking {
        val repository = FakeTierRepository(untitled())
        val viewModel = opened(repository, fresh = true)

        viewModel.renameTierList("Films")
        viewModel.screen.firstWithin { !it.untouchedDraft }
        val left = CompletableDeferred<Unit>()
        viewModel.leave { left.complete(Unit) }
        withTimeout(5_000) { left.await() }

        assertEquals(listOf("kept 1"), repository.drafts)
    }

    @Test
    fun aChangeMadeOnAnotherScreen_keepsTheNewListOnTheWayOut() = runBlocking {
        val repository = FakeTierRepository(untitled())
        val viewModel = opened(repository, fresh = true)

        repository.setCategory(1, "film-tv")
        val left = CompletableDeferred<Unit>()
        viewModel.leave { left.complete(Unit) }
        withTimeout(5_000) { left.await() }

        assertEquals(listOf("kept 1"), repository.drafts)
    }

    @Test
    fun aBoardOpenedFromTheList_isNeverADraft() = runBlocking {
        val repository = FakeTierRepository(untitled())
        val viewModel = opened(repository, fresh = false)

        assertFalse(viewModel.screen.value.untouchedDraft)
        var left = false
        viewModel.leave { left = true }

        assertEquals(true, left)
        assertEquals(emptyList<String>(), repository.drafts)
    }

    private suspend fun opened(repository: FakeTierRepository, fresh: Boolean): TierDetailViewModel {
        val viewModel = TierDetailViewModel(
            repository,
            FakeCommunityRepositoryForDetail(),
            FakeAccountRepository(),
            SavedStateHandle(mapOf("tierListId" to 1L, "fresh" to fresh)),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        return viewModel
    }

    private fun untitled() = TierList(
        id = 1,
        title = "Untitled",
        tiers = listOf(
            Tier(id = 10, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
            Tier(id = 11, label = "Unranked", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
        ),
    )
}
