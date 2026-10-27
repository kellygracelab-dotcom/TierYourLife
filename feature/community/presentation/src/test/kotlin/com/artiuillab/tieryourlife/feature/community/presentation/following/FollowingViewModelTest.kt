package com.artiuillab.tieryourlife.feature.community.presentation.following

import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val ann = FollowedAuthor(uid = "a", name = "Ann", photoUrl = null)
private val bea = FollowedAuthor(uid = "b", name = "Bea", photoUrl = "https://example.test/bea.jpg")
private val pokemon = Topic(id = "t1", name = "Pokémon", section = ListCategory.Games, status = TopicStatus.Approved)
private val cafes = Topic(id = "t2", name = "Kharkiv cafés", section = null, status = TopicStatus.Pending)

class FollowingViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<FollowingViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(fake: FollowingFake) = FollowingViewModel(fake).also { made += it }

    @Test
    fun sectionsTopicsAndPeople_areListed_withTheWaitingTopicsBeside() = runBlocking {
        val viewModel = viewModel(
            FollowingFake(people = listOf(ann, bea), topics = listOf(pokemon), sections = listOf(FollowedSection(ListCategory.Food, 4120)), mine = listOf(cafes)),
        )

        val ready = viewModel.state.firstWithin { it is FollowingUiState.Ready } as FollowingUiState.Ready

        assertEquals(listOf("a", "b"), ready.authors.map { it.uid })
        assertEquals(listOf("t1"), ready.topics.map { it.id })
        assertEquals(listOf(ListCategory.Food), ready.sections.map { it.section })
        assertEquals(listOf("t2"), ready.waiting.map { it.id })
    }

    @Test
    fun stoppingAPerson_keepsTheRow_turnsItsButton_andSaysSoWithUndo() = runBlocking {
        val fake = FollowingFake(people = listOf(ann, bea))
        val viewModel = viewModel(fake)
        viewModel.state.firstWithin { it is FollowingUiState.Ready }

        viewModel.toggle(FollowKeys.person("a"))

        val after = viewModel.state.firstWithin { (it as? FollowingUiState.Ready)?.stopped?.isNotEmpty() == true } as FollowingUiState.Ready
        assertEquals(listOf("a", "b"), after.authors.map { it.uid })
        assertEquals("a", withTimeout(5_000) { fake.unfollowAsked.await() })
        assertEquals(ManageNotice.Stopped("Ann", FollowKeys.person("a")), withTimeout(5_000) { viewModel.notices.first() })
    }

    @Test
    fun undo_followsAgain() = runBlocking {
        val fake = FollowingFake(people = listOf(ann))
        val viewModel = viewModel(fake)
        viewModel.state.firstWithin { it is FollowingUiState.Ready }
        viewModel.toggle(FollowKeys.person("a"))
        withTimeout(5_000) { viewModel.notices.first() }

        viewModel.toggle(FollowKeys.person("a"))

        assertEquals("a", withTimeout(5_000) { fake.followAsked.await() })
        viewModel.state.firstWithin { (it as? FollowingUiState.Ready)?.stopped?.isEmpty() == true }
        Unit
    }

    @Test
    fun aRefusal_turnsTheButtonBack_andSaysSo() = runBlocking {
        val viewModel = viewModel(FollowingFake(topics = listOf(pokemon), refuses = true))
        viewModel.state.firstWithin { it is FollowingUiState.Ready }

        viewModel.toggle(FollowKeys.topic("t1"))

        assertEquals(ManageNotice.Failed, withTimeout(5_000) { viewModel.notices.first() })
        viewModel.state.firstWithin { (it as? FollowingUiState.Ready)?.stopped?.isEmpty() == true }
        Unit
    }

    @Test
    fun stoppingASection_tellsTheServer() = runBlocking {
        val fake = FollowingFake(sections = listOf(FollowedSection(ListCategory.Food)))
        val viewModel = viewModel(fake)
        viewModel.state.firstWithin { it is FollowingUiState.Ready }

        viewModel.toggle(FollowKeys.section(ListCategory.Food))

        assertEquals(ListCategory.Food, withTimeout(5_000) { fake.sectionUnfollowed.await() })
    }

    @Test
    fun whenTheListDoesNotArrive_theScreenSaysSo() = runBlocking {
        val viewModel = viewModel(FollowingFake(fails = true))

        assertEquals(FollowingUiState.Failed, viewModel.state.firstWithin { it is FollowingUiState.Failed })
    }
}

private class FollowingFake(
    private val people: List<FollowedAuthor> = emptyList(),
    private val topics: List<Topic> = emptyList(),
    private val sections: List<FollowedSection> = emptyList(),
    private val mine: List<Topic> = emptyList(),
    private val refuses: Boolean = false,
    private val fails: Boolean = false,
) : FakeCommunityRepository() {
    val unfollowAsked = CompletableDeferred<String>()
    val followAsked = CompletableDeferred<String>()
    val sectionUnfollowed = CompletableDeferred<ListCategory>()

    override suspend fun following(): Result<Following> =
        if (fails) Result.failure(IllegalStateException("offline")) else Result.success(Following(people, topics, sections))

    override suspend fun myTopics(): Result<List<Topic>> = Result.success(mine)

    override suspend fun unfollow(authorUid: String): Result<Unit> {
        unfollowAsked.complete(authorUid)
        return answer()
    }

    override suspend fun follow(authorUid: String): Result<Unit> {
        followAsked.complete(authorUid)
        return answer()
    }

    override suspend fun unfollowTopic(id: String): Result<Unit> = answer()

    override suspend fun unfollowSection(section: ListCategory): Result<Unit> {
        sectionUnfollowed.complete(section)
        return answer()
    }

    private fun answer(): Result<Unit> = if (refuses) Result.failure(IllegalStateException("offline")) else Result.success(Unit)
}
