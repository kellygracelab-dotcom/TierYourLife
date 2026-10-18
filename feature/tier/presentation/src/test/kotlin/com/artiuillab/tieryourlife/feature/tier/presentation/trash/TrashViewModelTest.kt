package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrashViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val published = TrashEntry.DeletedList(id = 1, title = "Films", itemCount = 3, deletedAtMillis = 10, publishedId = "pub-1")
    private val private = TrashEntry.DeletedList(id = 2, title = "Games", itemCount = 1, deletedAtMillis = 20)
    private val card = TrashEntry.DeletedItem(id = 9, title = "Alien", listTitle = "Films", wasInPool = false, deletedAtMillis = 30)

    @Test
    fun opening_showsWhatIsInTheTrash() = runBlocking {
        val viewModel = TrashViewModel(TrashRepository(listOf(published, card)), Community())

        val state = viewModel.state.firstWithin { it is TrashUiState.Success } as TrashUiState.Success

        assertEquals(listOf(published, card), state.entries)
    }

    @Test
    fun aTrashThatCannotBeRead_isAnErrorRatherThanEmpty() = runBlocking {
        val viewModel = TrashViewModel(TrashRepository(emptyList(), readFails = true), Community())

        assertEquals(TrashUiState.Error, viewModel.state.firstWithin { it != TrashUiState.Loading })
    }

    // Restored private: the snapshot it left in the community is taken down
    // and the board forgets it was ever published.
    @Test
    fun restoringAPublishedList_takesItsSnapshotDownAndRestoresItPrivate() = runBlocking {
        val repository = TrashRepository(listOf(published, private))
        val community = Community()
        val viewModel = TrashViewModel(repository, community)
        viewModel.state.firstWithin { it is TrashUiState.Success }

        viewModel.restoreList(published.id)
        viewModel.state.firstWithin { (it as? TrashUiState.Success)?.entries == listOf(private) }

        assertEquals(listOf("pub-1"), community.unpublished)
        assertEquals(published.id, repository.clearedPublishedId)
    }

    @Test
    fun emptyingTheTrash_takesDownEveryPublishedSnapshotInIt() = runBlocking {
        val repository = TrashRepository(listOf(published, private, card))
        val community = Community()
        val viewModel = TrashViewModel(repository, community)
        viewModel.state.firstWithin { it is TrashUiState.Success }

        viewModel.emptyTrash()
        viewModel.state.firstWithin { (it as? TrashUiState.Success)?.entries?.isEmpty() == true }

        assertEquals(listOf("pub-1"), community.unpublished)
    }

    @Test
    fun deletingACardForGood_leavesTheCommunityAlone() = runBlocking {
        val community = Community()
        val viewModel = TrashViewModel(TrashRepository(listOf(published, card)), community)
        viewModel.state.firstWithin { it is TrashUiState.Success }

        viewModel.removeItemPermanently(card.id)
        viewModel.state.firstWithin { (it as? TrashUiState.Success)?.entries == listOf(published) }

        assertTrue(community.unpublished.isEmpty())
    }

    @Test
    fun aFailedRestore_saysSoAndKeepsTheEntry() = runBlocking {
        val viewModel = TrashViewModel(TrashRepository(listOf(card), writesFail = true), Community())
        viewModel.state.firstWithin { it is TrashUiState.Success }

        viewModel.restoreItem(card.id)
        val message = withTimeout(TIMEOUT_MILLIS) { viewModel.userMessages.first() }

        assertEquals(UserMessage.ActionFailed, message)
        assertEquals(TrashUiState.Success(listOf(card)), viewModel.state.value)
    }

    private class Community : FakeCommunityRepository() {
        val unpublished = mutableListOf<String>()

        override suspend fun unpublish(publishedId: String): Result<Unit> {
            unpublished += publishedId
            return Result.success(Unit)
        }
    }

    private class TrashRepository(
        entries: List<TrashEntry>,
        private val readFails: Boolean = false,
        private val writesFail: Boolean = false,
        private val base: RecordingTierRepository = RecordingTierRepository(),
    ) : TierRepository by base {

        private var trash = entries
        val clearedPublishedId: Long? get() = base.clearedPublishedId

        override suspend fun getTrashEntries(): List<TrashEntry> {
            check(!readFails) { "disk" }
            return trash
        }

        override suspend fun restoreTierLists(ids: List<Long>) = remove { it is TrashEntry.DeletedList && it.id in ids }

        override suspend fun restoreTierItem(id: Long) = remove { it is TrashEntry.DeletedItem && it.id == id }

        override suspend fun deleteTierItemPermanently(id: Long) = remove { it is TrashEntry.DeletedItem && it.id == id }

        override suspend fun emptyTrash() = remove { true }

        private fun remove(which: (TrashEntry) -> Boolean) {
            check(!writesFail) { "disk" }
            trash = trash.filterNot(which)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
