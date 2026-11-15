package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdjustCoverViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val picture = "https://example.test/portrait.jpg"

    @Test
    fun opensOnTheFocusTheBoardHas() = runBlocking {
        val viewModel = adjusting(TierList(1, "Films", emptyList(), coverImageUrl = picture, coverFocus = CoverFocus(0.5f, 0.2f, 2f)))

        val state = viewModel.state.firstWithin { it.loaded }

        assertEquals(picture, state.picture)
        assertEquals(CoverFocus(0.5f, 0.2f, 2f), state.focus)
    }

    @Test
    fun done_keepsTheFocusAsFinelyAsTheSite() = runBlocking {
        val repository = RecordingTierRepository(boards = listOf(TierList(1, "Films", emptyList(), coverImageUrl = picture)))
        val viewModel = adjusting(repository)
        viewModel.state.firstWithin { it.loaded }

        viewModel.move(CoverFocus(0.123456f, 0.9f, 1.504f))
        viewModel.save()

        viewModel.state.firstWithin { it.saved }
        assertEquals(listOf(1L to CoverFocus(0.1235f, 0.9f, 1.5f)), repository.coverFocuses)
    }

    @Test
    fun reset_andDone_leaveTheCoverToItsDefault() = runBlocking {
        val repository = RecordingTierRepository(
            boards = listOf(TierList(1, "Films", emptyList(), coverImageUrl = picture, coverFocus = CoverFocus(0.2f, 0.2f, 3f))),
        )
        val viewModel = adjusting(repository)
        viewModel.state.firstWithin { it.loaded }

        viewModel.reset()
        assertEquals(null, viewModel.state.value.focus)
        viewModel.save()

        viewModel.state.firstWithin { it.saved }
        assertEquals(listOf(1L to null), repository.coverFocuses)
    }

    @Test
    fun aBoardWithoutASingleCover_hasNothingToAdjust() = runBlocking {
        val viewModel = adjusting(TierList(1, "Films", emptyList(), coverImageUrls = listOf("https://a/1.jpg", "https://a/2.jpg")))

        assertTrue(viewModel.state.firstWithin { it.loaded }.nothingToAdjust)
    }

    private fun adjusting(board: TierList) = adjusting(RecordingTierRepository(boards = listOf(board)))

    private fun adjusting(repository: RecordingTierRepository) =
        AdjustCoverViewModel(repository, SavedStateHandle(mapOf("tierListId" to 1L)))
}
