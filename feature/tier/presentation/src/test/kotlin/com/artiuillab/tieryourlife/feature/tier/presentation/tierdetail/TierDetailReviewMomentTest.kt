package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.ReviewMoments
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The last card placed by hand finishes the list, a moment later; moves after that do not finish it again. */
class TierDetailReviewMomentTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Recorded : ReviewMoments {
        val finished = mutableListOf<Int>()

        override fun listFinished(cards: Int) {
            finished += cards
        }

        override val asks: Flow<Unit> = emptyFlow()
    }

    @Test
    fun theLastCardPlaced_finishesTheList_onlyOnce() = runBlocking {
        val moments = Recorded()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(board()),
            FakeCommunityRepositoryForDetail(),
            FakeAccountRepository(),
            SavedStateHandle(mapOf("tierListId" to 1L, "startInTitleEdit" to false)),
            reviewMoments = moments,
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        withContext(Dispatchers.Main) { viewModel.moveItem(103, toTierId = 10, toPosition = 0) }
        assertEquals("not before the pause", emptyList<Int>(), moments.finished)
        withTimeoutOrNull(3_000) { while (moments.finished.isEmpty()) delay(20) }
        assertEquals(listOf(5), moments.finished)

        withContext(Dispatchers.Main) { viewModel.moveItem(103, toTierId = 10, toPosition = 4) }
        delay(ReviewMoments.FINISHED_PAUSE_MS + 300)

        assertEquals(listOf(5), moments.finished)
    }

    private fun board(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(
                id = 10,
                label = "S",
                colorLight = "#000000",
                colorDark = "#000000",
                items = (100L..102L).map { TierItem(id = it, title = "Card $it", imageUrl = null) } +
                    TierItem(id = 104, title = "Card 104", imageUrl = null),
            ),
            Tier(
                id = 30,
                label = "Pool",
                colorLight = "#000000",
                colorDark = "#000000",
                items = listOf(TierItem(id = 103, title = "Card 103", imageUrl = null)),
                isPool = true,
            ),
        ),
    )
}
