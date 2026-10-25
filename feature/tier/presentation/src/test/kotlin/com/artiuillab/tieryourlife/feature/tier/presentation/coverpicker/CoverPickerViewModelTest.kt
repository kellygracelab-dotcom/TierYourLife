package com.artiuillab.tieryourlife.feature.tier.presentation.coverpicker

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CoverPickerViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val web = List(7) { "https://example.test/$it.jpg" }
    private val local = "/data/user/0/app/files/tier_images/own.jpg"

    @Test
    fun theBoardsWebPictures_areTheCandidates_eachOnce() = runBlocking {
        val viewModel = pickerFor(board(pictures = listOf(web[0], web[1], web[0], local, null)))

        val state = viewModel.state.firstWithin { it.title == "Films" }

        assertEquals(listOf(web[0], web[1]), state.candidates)
        assertEquals(listOf(web[0], web[1], local), state.automatic)
        assertEquals(state.automatic, state.previewImages)
    }

    @Test
    fun tappedPictures_areChosenInThatOrder_andATapOnAChosenOneLetsItGo() = runBlocking {
        val viewModel = pickerFor(board(pictures = web.take(3)))
        viewModel.state.firstWithin { it.title == "Films" }

        viewModel.toggle(web[2])
        viewModel.toggle(web[0])
        viewModel.toggle(web[1])
        assertEquals(listOf(web[2], web[0], web[1]), viewModel.state.value.chosen)

        viewModel.toggle(web[0])
        assertEquals(listOf(web[2], web[1]), viewModel.state.value.chosen)
    }

    @Test
    fun aSeventhPicture_isNotTaken() = runBlocking {
        val viewModel = pickerFor(board(pictures = web))
        viewModel.state.firstWithin { it.title == "Films" }

        web.forEach(viewModel::toggle)

        assertEquals(web.take(6), viewModel.state.value.chosen)
        assertFalse(viewModel.state.value.canChooseMore)
    }

    @Test
    fun onePicture_isTheCoverItself_andASecondMakesACollage() = runBlocking {
        val viewModel = pickerFor(board(pictures = web.take(3)))
        viewModel.state.firstWithin { it.title == "Films" }

        viewModel.toggle(web[1])
        assertEquals(web[1], viewModel.state.value.coverImageUrl)

        viewModel.toggle(web[2])
        assertNull(viewModel.state.value.coverImageUrl)
        assertEquals(listOf(web[1], web[2]), viewModel.state.value.previewImages)
    }

    @Test
    fun reset_leavesTheCoverToTheCards() = runBlocking {
        val viewModel = pickerFor(board(pictures = web.take(3)))
        viewModel.state.firstWithin { it.title == "Films" }
        viewModel.toggle(web[0])
        viewModel.toggle(web[1])

        viewModel.reset()

        assertEquals(emptyList<String>(), viewModel.state.value.chosen)
        assertNull(viewModel.state.value.coverImageUrl)
        assertEquals(web.take(3), viewModel.state.value.previewImages)
    }

    @Test
    fun aGalleryPicture_isTheWholeCover_andAWebPictureAfterItStartsOver() = runBlocking {
        val viewModel = pickerFor(board(pictures = web.take(3)))
        viewModel.state.firstWithin { it.title == "Films" }
        viewModel.toggle(web[0])

        viewModel.pickFromGallery("content://media/42")
        assertEquals(listOf("content://media/42"), viewModel.state.value.chosen)

        viewModel.toggle(web[1])
        assertEquals(listOf(web[1]), viewModel.state.value.chosen)
    }

    @Test
    fun theStoredChoice_isWhatOpens() = runBlocking {
        val single = pickerFor(board(pictures = web.take(3), cover = web[2]))
        assertEquals(listOf(web[2]), single.state.firstWithin { it.title == "Films" }.chosen)

        val collage = pickerFor(board(pictures = web.take(3), collage = listOf(web[1], web[0])))
        assertEquals(listOf(web[1], web[0]), collage.state.firstWithin { it.title == "Films" }.chosen)
    }

    @Test
    fun done_savesTheChoice_andLeaves() = runBlocking {
        val repository = RecordingTierRepository(boards = listOf(board(pictures = web.take(3))))
        val viewModel = pickerFor(repository)
        viewModel.state.firstWithin { it.title == "Films" }
        viewModel.toggle(web[0])
        viewModel.toggle(web[2])

        viewModel.save()
        viewModel.state.firstWithin { it.saved }

        assertEquals(listOf(1L to listOf(web[0], web[2])), repository.covers)
    }

    private fun pickerFor(board: TierList) = pickerFor(RecordingTierRepository(boards = listOf(board)))

    private fun pickerFor(repository: RecordingTierRepository) =
        CoverPickerViewModel(repository, SavedStateHandle(mapOf("tierListId" to 1L)))

    private fun board(pictures: List<String?>, cover: String? = null, collage: List<String> = emptyList()) = TierList(
        id = 1,
        title = "Films",
        coverImageUrl = cover,
        coverImageUrls = collage,
        tiers = listOf(
            Tier(
                id = 1,
                label = "S",
                colorLight = "#B03A32",
                colorDark = "#F1948C",
                items = pictures.mapIndexed { index, url -> TierItem(id = index.toLong(), title = "Card $index", imageUrl = url) },
            ),
            Tier(id = 2, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
        ),
    )
}
