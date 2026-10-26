package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.Moderator
import com.artiuillab.tieryourlife.feature.community.domain.model.PendingTopic
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TopicQueueViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun theQueue_isWhatTheServerHandedBack() = runBlocking {
        val viewModel = TopicQueueViewModel(TopicsFake(topics = listOf(proposed("t1"), proposed("t2"))), OwnerMemory())

        val ready = viewModel.topics.firstWithin { it is TopicQueueUiState.Ready } as TopicQueueUiState.Ready

        assertEquals(listOf("t1", "t2"), ready.pending.map { it.topic.id })
    }

    @Test
    fun approvingATopic_namesTheSection_andTakesTheRowOut() = runBlocking {
        val repository = TopicsFake(topics = listOf(proposed("t1"), proposed("t2")))
        val viewModel = TopicQueueViewModel(repository, OwnerMemory())
        viewModel.topics.firstWithin { it is TopicQueueUiState.Ready }

        viewModel.approveTopic("t1", ListCategory.Games)
        val left = viewModel.topics.firstWithin { it.settled() && (it as TopicQueueUiState.Ready).pending.size == 1 }

        assertEquals(listOf("t2"), (left as TopicQueueUiState.Ready).pending.map { it.topic.id })
        assertEquals(listOf("t1" to ListCategory.Games), repository.approved)
    }

    // Renaming decides nothing: the row stays, under the new name, for the decision still to come.
    @Test
    fun renamingATopic_keepsItInTheQueue_underItsNewName() = runBlocking {
        val viewModel = TopicQueueViewModel(TopicsFake(topics = listOf(proposed("t1"))), OwnerMemory())
        viewModel.topics.firstWithin { it is TopicQueueUiState.Ready }

        viewModel.renameTopic("t1", "Pokemon games")
        val renamed = viewModel.topics.firstWithin {
            (it as? TopicQueueUiState.Ready)?.pending?.firstOrNull()?.topic?.name == "Pokemon games"
        }

        assertEquals(1, (renamed as TopicQueueUiState.Ready).pending.size)
        assertNull(renamed.settling)
    }

    @Test
    fun aDecisionThatFails_leavesTheRowWhereItWas() = runBlocking {
        val repository = TopicsFake(topics = listOf(proposed("t1")), actionsFail = true)
        val viewModel = TopicQueueViewModel(repository, OwnerMemory())
        viewModel.topics.firstWithin { it is TopicQueueUiState.Ready }

        viewModel.rejectTopic("t1")
        val after = viewModel.topics.firstWithin { it.settled() && repository.rejected.isNotEmpty() }

        assertEquals(listOf("t1"), (after as TopicQueueUiState.Ready).pending.map { it.topic.id })
    }

    // The moderators list answers the owner alone; reading it is how the app learns who it is talking to.
    @Test
    fun theOwner_seesTheModerators_andTheMemoryLearnsThem() = runBlocking {
        val owner = OwnerMemory()
        val viewModel = TopicQueueViewModel(TopicsFake(moderators = listOf(moderator("m1"))), owner)

        val ready = viewModel.moderators.firstWithin { it is ModeratorsUiState.Ready } as ModeratorsUiState.Ready

        assertEquals(listOf("m1"), ready.moderators.map { it.uid })
        assertEquals(setOf("m1"), owner.moderators.value)
    }

    @Test
    fun aModerator_whoIsNotTheOwner_getsNoModeratorsAtAll() = runBlocking {
        val owner = OwnerMemory()
        val viewModel = TopicQueueViewModel(TopicsFake(), owner)

        val reached = viewModel.moderators.firstWithin { it is ModeratorsUiState.Unavailable }

        assertEquals(ModeratorsUiState.Unavailable, reached)
        assertNull(owner.moderators.value)
    }

    @Test
    fun appointingAndRemoving_moveTheListAndTheMemoryTogether() = runBlocking {
        val owner = OwnerMemory()
        val repository = TopicsFake(moderators = listOf(moderator("m1")))
        val viewModel = TopicQueueViewModel(repository, owner)
        viewModel.moderators.firstWithin { it is ModeratorsUiState.Ready }

        viewModel.appointModerator(" m2 ", "Bea")
        viewModel.moderators.firstWithin { (it as? ModeratorsUiState.Ready)?.moderators?.size == 2 }
        viewModel.removeModerator("m1")
        val after = viewModel.moderators.firstWithin {
            (it as? ModeratorsUiState.Ready)?.let { ready -> !ready.busy && ready.moderators.size == 1 } == true
        }

        assertEquals(listOf("m2"), (after as ModeratorsUiState.Ready).moderators.map { it.uid })
        assertEquals(setOf("m2"), owner.moderators.value)
        assertEquals(listOf("m2" to "Bea"), repository.appointed)
        assertEquals(listOf("m1"), repository.removed)
    }

    private fun proposed(id: String) = PendingTopic(
        topic = Topic(id = id, name = "Topic $id", section = null, status = TopicStatus.Pending, createdBy = "u1"),
        proposerName = "Ann",
        requestedSection = ListCategory.Games,
    )

    private fun moderator(uid: String) = Moderator(uid = uid, name = "Mod $uid", photoUrl = null, appointedAtMillis = 0)
}

private fun TopicQueueUiState.settled(): Boolean = this is TopicQueueUiState.Ready && settling == null

private class TopicsFake(
    private val topics: List<PendingTopic> = emptyList(),
    /** Null for everyone but the owner. */
    private val moderators: List<Moderator>? = null,
    private val actionsFail: Boolean = false,
) : FakeCommunityRepository() {
    val approved = mutableListOf<Pair<String, ListCategory>>()
    val rejected = mutableListOf<String>()
    val appointed = mutableListOf<Pair<String, String?>>()
    val removed = mutableListOf<String>()

    override suspend fun pendingTopics(): Result<List<PendingTopic>> = Result.success(topics)

    override suspend fun approveTopic(id: String, section: ListCategory): Result<Topic> {
        if (actionsFail) return Result.failure(IllegalStateException("offline"))
        approved += id to section
        return Result.success(topics.first { it.topic.id == id }.topic.copy(section = section, status = TopicStatus.Approved))
    }

    override suspend fun renameTopic(id: String, name: String): Result<Topic> =
        Result.success(topics.first { it.topic.id == id }.topic.copy(name = name))

    override suspend fun rejectTopic(id: String): Result<Topic> {
        rejected += id
        if (actionsFail) return Result.failure(IllegalStateException("offline"))
        return Result.success(topics.first { it.topic.id == id }.topic.copy(status = TopicStatus.Rejected))
    }

    override suspend fun moderators(): Result<List<Moderator>> =
        moderators?.let { Result.success(it) } ?: Result.failure(IllegalStateException("not the owner"))

    override suspend fun appointModerator(uid: String, name: String?, photoUrl: String?): Result<Unit> {
        appointed += uid to name
        return Result.success(Unit)
    }

    override suspend fun removeModerator(uid: String): Result<Unit> {
        removed += uid
        return Result.success(Unit)
    }
}
