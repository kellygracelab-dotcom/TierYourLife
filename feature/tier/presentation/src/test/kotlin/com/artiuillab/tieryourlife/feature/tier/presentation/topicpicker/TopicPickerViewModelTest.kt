package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

private val oscars = Topic(id = "t1", name = "Oscars", section = ListCategory.FilmTv, status = TopicStatus.Approved, listCount = 41)
private val snubs = Topic(id = "t2", name = "Oscar snubs", section = ListCategory.FilmTv, status = TopicStatus.Approved, listCount = 6)
private val pixar = Topic(id = "t3", name = "Pixar", section = ListCategory.FilmTv, status = TopicStatus.Approved, listCount = 12)

private fun pending(n: Int) = Topic(id = "p$n", name = "Waiting $n", section = null, status = TopicStatus.Pending, createdBy = "me")

class TopicPickerViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun theBoardsOwnTopic_isWhatOpens() = runBlocking {
        val known = pickerFor(board(topicId = "t1", topicName = "Oscars"))
        assertEquals(TopicChoice.Known::class, known.state.firstWithin { it.title == "Films" }.chosen::class)

        val drafted = pickerFor(board(newTopicName = "Oscars 2025"))
        assertEquals(TopicChoice.New("Oscars 2025"), drafted.state.firstWithin { it.title == "Films" }.chosen)
    }

    @Test
    fun typing_asksTheServer_andTheAnswerIsTheList() = runBlocking {
        val community = TopicsFake(found = listOf(oscars, snubs, pixar))
        val viewModel = pickerFor(board(), community)
        viewModel.state.firstWithin { it.title == "Films" && it.results.size == 3 }

        viewModel.onQueryChange("Osc")

        val state = viewModel.state.firstWithin { it.results.size == 2 && !it.searching }
        assertEquals(listOf("Oscars", "Oscar snubs"), state.results.map { it.name })
        assertEquals("Osc", community.queries.last())
    }

    @Test
    fun aNewName_isOffered_untilItMatchesAKnownTopic() = runBlocking {
        val viewModel = pickerFor(board(), TopicsFake(found = listOf(oscars)))
        viewModel.state.firstWithin { it.title == "Films" && it.results.isNotEmpty() }

        viewModel.onQueryChange("Oscars 2025")
        assertEquals("Oscars 2025", viewModel.state.firstWithin { it.query == "Oscars 2025" }.createName)

        viewModel.onQueryChange("oscars")
        assertNull(viewModel.state.firstWithin { it.query == "oscars" }.createName)
    }

    @Test
    fun creating_namesTheTopic_whileFewerThanThreeWait() = runBlocking {
        val viewModel = pickerFor(board(), TopicsFake(mine = listOf(pending(1), pending(2))))
        viewModel.state.firstWithin { it.title == "Films" && it.mine.size == 2 }

        viewModel.onQueryChange("Kharkiv restaurants")
        viewModel.state.firstWithin { it.query.isNotEmpty() }
        viewModel.createNew()

        assertEquals(TopicChoice.New("Kharkiv restaurants"), viewModel.state.value.chosen)
    }

    @Test
    fun aFourthWaitingTopic_isNotOffered() = runBlocking {
        val viewModel = pickerFor(board(), TopicsFake(mine = listOf(pending(1), pending(2), pending(3))))
        val state = viewModel.state.firstWithin { it.title == "Films" && it.mine.size == 3 }
        assertFalse(state.canCreate)

        viewModel.onQueryChange("One more")
        viewModel.state.firstWithin { it.query.isNotEmpty() }
        viewModel.createNew()

        assertEquals(TopicChoice.None, viewModel.state.value.chosen)
    }

    // A name the server already knows is that topic, not a twin of it.
    @Test
    fun creatingAKnownName_choosesTheKnownTopicInstead() = runBlocking {
        val viewModel = pickerFor(board(), TopicsFake(found = listOf(oscars)))
        viewModel.state.firstWithin { it.title == "Films" && it.results.isNotEmpty() }

        viewModel.onQueryChange("  oscars ")
        viewModel.state.firstWithin { it.query.isNotEmpty() }
        viewModel.createNew()

        assertEquals(TopicChoice.Known(oscars), viewModel.state.value.chosen)
    }

    @Test
    fun theTitle_pointsAtATopicItContains() = runBlocking {
        val viewModel = pickerFor(board(title = "Oscar snubs of the decade"), TopicsFake(found = listOf(oscars, snubs)))

        val state = viewModel.state.firstWithin { it.suggestion != null }

        assertEquals("Oscar snubs", state.suggestion?.name)
    }

    @Test
    fun done_storesTheChoice_andLeaves() = runBlocking {
        val repository = RecordingTierRepository(boards = listOf(board()))
        val viewModel = pickerFor(repository, TopicsFake(found = listOf(oscars)))
        viewModel.state.firstWithin { it.title == "Films" && it.results.isNotEmpty() }
        viewModel.choose(oscars)

        viewModel.save()
        viewModel.state.firstWithin { it.saved }

        assertEquals(listOf(listOf(1L, "t1", "Oscars", null)), repository.topics)
    }

    @Test
    fun aNewName_isStoredAsADraft() = runBlocking {
        val repository = RecordingTierRepository(boards = listOf(board()))
        val viewModel = pickerFor(repository, TopicsFake())
        viewModel.state.firstWithin { it.title == "Films" }
        viewModel.onQueryChange("Oscars 2025")
        viewModel.state.firstWithin { it.query.isNotEmpty() }
        viewModel.createNew()

        viewModel.save()
        viewModel.state.firstWithin { it.saved }

        assertEquals(listOf(listOf(1L, null, null, "Oscars 2025")), repository.topics)
    }

    private fun board(
        title: String = "Films",
        topicId: String? = null,
        topicName: String? = null,
        newTopicName: String? = null,
    ) = TierList(
        id = 1,
        title = title,
        tiers = emptyList(),
        category = ListCategory.FilmTv,
        topicId = topicId,
        topicName = topicName,
        newTopicName = newTopicName,
    )

    private fun pickerFor(board: TierList, community: FakeCommunityRepository = TopicsFake()) =
        pickerFor(RecordingTierRepository(boards = listOf(board)), community)

    private fun pickerFor(repository: RecordingTierRepository, community: FakeCommunityRepository) = TopicPickerViewModel(
        repository = repository,
        community = community,
        savedStateHandle = SavedStateHandle(mapOf("tierListId" to 1L)),
    )
}

private class TopicsFake(
    private val found: List<Topic> = emptyList(),
    private val mine: List<Topic> = emptyList(),
) : FakeCommunityRepository() {
    val queries = mutableListOf<String?>()

    override suspend fun topics(query: String?, section: ListCategory?): Result<List<Topic>> {
        queries += query
        val wanted = query?.lowercase()
        return Result.success(if (wanted == null) found else found.filter { it.name.lowercase().startsWith(wanted) })
    }

    override suspend fun myTopics(): Result<List<Topic>> = Result.success(mine)
}
