package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrivateRankingProgressTest {
    @get:Rule
    val main = MainDispatcherRule()
    private val made = mutableListOf<CommunityListViewModel>()

    @After
    fun clear() = made.forEach { it.viewModelScope.cancel() }

    @Test
    fun anAccountChangeBackDuringDraftLoadingRestoresTheGuestAndKeepsTheEditorWritable() = runBlocking {
        val storage = mutableMapOf<String?, com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft>()
        val reading = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val account = Account.SignedIn("b@example.com", null, uid = "b")
        val drafts = object : com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts {
            override suspend fun read(listId: String, owner: String?): com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft? {
                if (owner == account.recordKey) {
                    reading.complete(Unit)
                    release.await()
                }
                return storage[owner]
            }
            override suspend fun keep(listId: String, draft: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft, owner: String?) {
                storage[owner] = draft
            }
        }
        val accounts = FakeAccountRepository()
        val vm = viewModel(accounts = accounts, drafts = drafts)
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.show(Showing.Mine)
        vm.moveItem(1, 0, 0)
        accounts.accounts.value = account
        withTimeout(5_000) { reading.await() }
        accounts.accounts.value = Account.Guest
        release.complete(Unit)
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.let { state -> !state.arriving && !state.signedIn } == true }
        vm.moveItem(0, 0, 1)
        assertEquals(listOf(1, 0), storage[null]?.rows?.first())
        assertEquals(emptyList<Int>(), storage[null]?.pool)
    }

    @Test
    fun signingIntoAnAccountWithItsOwnDraft_restoresIt_andCannotUndoOrOverwriteItWithGuestWork() = runBlocking {
        val storage = mutableMapOf<Pair<String?, String>, com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft>()
        val drafts = object : com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts {
            override suspend fun read(listId: String, owner: String?) = storage[owner to listId]
            override suspend fun keep(listId: String, draft: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft, owner: String?) {
                storage[owner to listId] = draft
            }
        }
        val accounts = FakeAccountRepository()
        val vm = viewModel(community = TestCommunity(arrangement = listOf(0, null)), accounts = accounts, drafts = drafts)
        val first = vm.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val owner = Account.SignedIn("b@example.com", null, uid = "b")
        val bsDraft = com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft(
            first.version, first.mine.tiers.filterNot { it.isPool }.mapIndexed { at, _ -> if (at == 0) listOf(1) else emptyList() }, listOf(0),
        )
        storage[owner.recordKey to "abc"] = bsDraft
        vm.moveItem(1, 1, 0)
        val guest = (vm.state.value as CommunityListUiState.Success).mine
        accounts.accounts.value = owner
        val signedIn = vm.state.firstWithin {
            val state = it as? CommunityListUiState.Success
            state?.signedIn == true && !state.arriving && state.mine.tiers.first().items.map { card -> card.id } == listOf(1L)
        } as CommunityListUiState.Success
        vm.undoStartFromTheirs()
        assertEquals(signedIn.mine, (vm.state.value as CommunityListUiState.Success).mine)
        vm.rankings.returnAllToPool()
        assertEquals(emptyList<Int>(), storage[null to "abc"]!!.pool)
        assertEquals(listOf(0, 1), storage[owner.recordKey to "abc"]!!.pool)
        accounts.accounts.value = Account.Guest
        val guestAgain = vm.state.firstWithin { (it as? CommunityListUiState.Success)?.signedIn == false && !(it as CommunityListUiState.Success).arriving }
        assertEquals(guest, (guestAgain as CommunityListUiState.Success).mine)
    }

    @Test
    fun aConfirmedResetFinishesAfterBack_andReopeningWaitsForThePendingWrite() = runBlocking {
        var saved: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft? = null
        val lock = kotlinx.coroutines.sync.Mutex()
        val drafts = object : com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts {
            override suspend fun read(listId: String, owner: String?): com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft? {
                lock.lock()
                try {
                    return saved
                } finally {
                    lock.unlock()
                }
            }
            override suspend fun keep(listId: String, draft: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft, owner: String?) {
                lock.lock()
                try {
                    kotlinx.coroutines.delay(150)
                    saved = draft
                } finally {
                    lock.unlock()
                }
            }
        }
        val vm = viewModel(drafts = drafts)
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.show(Showing.Mine)
        vm.moveItem(0, 0, 0)
        vm.rankings.returnAllToPool()
        vm.viewModelScope.cancel()
        val reopened = viewModel(drafts = drafts)
        val state = reopened.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        assertEquals(listOf(1L, 0L), state.mine.tiers.first { it.isPool }.items.map { it.id })
        assertTrue(state.mine.tiers.filterNot { it.isPool }.all { it.items.isEmpty() })
    }

    @Test
    fun returningMineToPoolPreservesPoolFirstAndSurvivesReopeningWithoutChangingTheAuthor() = runBlocking {
        val kept = mutableMapOf<String, com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft>()
        val drafts = object : com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts {
            override suspend fun read(listId: String, owner: String?) = kept[listId]
            override suspend fun keep(listId: String, draft: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft, owner: String?) {
                kept[listId] = draft
            }
        }
        val vm = viewModel(drafts = drafts)
        val original = vm.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        vm.show(Showing.Mine)
        val item = original.mine.tiers.first { it.isPool }.items.first()
        val target = original.mine.tiers.first { !it.isPool }
        vm.moveItem(item.id, target.id, 0)
        vm.rankings.returnAllToPool()
        val reset = vm.state.value as CommunityListUiState.Success
        assertEquals(original.list, reset.list)
        assertTrue(reset.mine.tiers.filterNot { it.isPool }.all { it.items.isEmpty() })
        assertEquals(listOf(1L, 0L), reset.mine.tiers.first { it.isPool }.items.map { it.id })
        withTimeout(5_000) { while (kept["abc"]?.pool != listOf(1, 0)) kotlinx.coroutines.delay(10) }
        val reopened = viewModel(drafts = drafts)
        val restored = reopened.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        assertEquals(reset.mine, restored.mine)
        assertEquals(Showing.Mine, restored.showing)
    }

    private fun viewModel(
        community: CommunityRepository = TestCommunity(),
        accounts: FakeAccountRepository = FakeAccountRepository(),
        drafts: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts,
    ) = CommunityListViewModel(
        community, RecordingTierRepository(), FakeAppPreferences(), SavedStateHandle(mapOf("publishedId" to "abc")),
        accounts, FakeOpenedLists(), FollowAfterSignIn(accounts), drafts,
    ).also { made += it }
}
