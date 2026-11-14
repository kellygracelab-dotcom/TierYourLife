package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RankingAccountLeaseTest {
    @get:Rule val main = MainDispatcherRule()
    private val made = mutableListOf<CommunityListViewModel>()

    @After fun clear() = made.forEach { it.viewModelScope.cancel() }

    @Test fun delayedRereadAfterManualShowCannotPublishTheNextAccountsPrivateArrangement() = runBlocking {
        val fixture = LeaseFixture(holdRead = true)
        val vm = open(fixture)
        vm.showMyRanking()
        withTimeout(5_000) { fixture.readStarted.await() }
        switchTo(vm, fixture, fixture.b, card = 1)
        fixture.release.complete(Unit)
        withContext(Dispatchers.Main) { }
        assertTrue(fixture.postedOwners.isEmpty())
        assertTrue(fixture.tiers.rankings.isEmpty())
        assertEquals(listOf(1L), vm.loadedLease().mine.tiers.first().items.map { it.id })
        assertFalse(vm.loadedLease().rankings.any { it.mine })
    }

    @Test fun aLateSuccessfulPutCannotMarkTheNextAccountsCopyPublic() = runBlocking {
        val fixture = LeaseFixture(holdPost = true)
        val vm = open(fixture)
        vm.rankings.showMine()
        withTimeout(5_000) { fixture.postStarted.await() }
        assertEquals(listOf("a"), fixture.postedOwners)
        switchTo(vm, fixture, fixture.b, card = 1)
        fixture.release.complete(Unit)
        withContext(Dispatchers.Main) { }
        assertTrue(fixture.tiers.rankings.isEmpty())
        assertEquals(listOf(1L), vm.loadedLease().mine.tiers.first().items.map { it.id })
        assertFalse(vm.loadedLease().rankings.any { it.mine })
    }

    @Test fun switchingAwayAndBackDoesNotReviveTheOldConsentEvenWithTheSameUidAndSnapshot() = runBlocking {
        val fixture = LeaseFixture(holdRead = true)
        val vm = open(fixture)
        vm.rankings.showMine()
        withTimeout(5_000) { fixture.readStarted.await() }
        switchTo(vm, fixture, fixture.b, card = 1)
        switchTo(vm, fixture, fixture.a, card = 0)
        fixture.release.complete(Unit)
        withContext(Dispatchers.Main) { }
        assertTrue(fixture.postedOwners.isEmpty())
        assertTrue(fixture.tiers.rankings.isEmpty())
    }

    @Test fun anEditDuringRefreshRequiresANewExplicitPublicationAction() = runBlocking {
        val fixture = LeaseFixture(holdRead = true)
        val vm = open(fixture)
        vm.rankings.showMine()
        withTimeout(5_000) { fixture.readStarted.await() }
        vm.moveItem(1, 1, 0)
        fixture.release.complete(Unit)
        withContext(Dispatchers.Main) { }
        assertTrue(fixture.postedOwners.isEmpty())
        assertEquals(listOf(1L), vm.loadedLease().mine.tiers[1].items.map { it.id })
    }

    private suspend fun open(fixture: LeaseFixture): CommunityListViewModel {
        val vm = CommunityListViewModel(
            fixture, fixture.tiers, FakeAppPreferences(), SavedStateHandle(mapOf("publishedId" to "abc")),
            fixture.accounts, FakeOpenedLists(), FollowAfterSignIn(fixture.accounts), fixture.drafts,
        )
        made += vm
        vm.state.firstWithin { it is CommunityListUiState.Success }
        vm.show(Showing.Mine)
        vm.moveItem(0, 0, 0)
        return vm
    }

    private suspend fun switchTo(vm: CommunityListViewModel, fixture: LeaseFixture, account: Account.SignedIn, card: Long) {
        fixture.accounts.accounts.value = account
        vm.state.firstWithin {
            (it as? CommunityListUiState.Success)?.let { state ->
                !state.arriving && state.mine.tiers.first().items.map { item -> item.id } == listOf(card)
            } == true
        }
        withContext(Dispatchers.Main) { }
    }
}

private fun CommunityListViewModel.loadedLease() = state.value as CommunityListUiState.Success

private class LeaseFixture(private val holdRead: Boolean = false, private val holdPost: Boolean = false) : FakeCommunityRepository() {
    val a = Account.SignedIn("a@example.com", null, uid = "a")
    val b = Account.SignedIn("b@example.com", null, uid = "b")
    val accounts = FakeAccountRepository().apply { accounts.value = a }
    val tiers = RecordingTierRepository()
    val readStarted = CompletableDeferred<Unit>()
    val postStarted = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val postedOwners = mutableListOf<String?>()
    private var opens = 0
    private val saved = mutableMapOf<String?, RankingDraft>("b" to RankingDraft(0, listOf(listOf(1), emptyList()), listOf(0)))
    val drafts = object : RankingDrafts {
        override suspend fun read(listId: String, owner: String?) = saved[owner]
        override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) {
            saved[owner] = draft
        }
    }

    override suspend fun open(id: String) = withContext(NonCancellable) {
        opens++
        if (holdRead && opens > 1) {
            readStarted.complete(Unit)
            release.await()
        }
        Result.success(published)
    }

    override suspend fun showMyRankingVersioned(
        listId: String, arrangement: List<Int?>, rows: List<List<Int>>, tierLabels: List<String>, version: Long?, expectedOwnerUid: String?, board: TierList?,
    ) = withContext(NonCancellable) {
        postedOwners += expectedOwnerUid
        postStarted.complete(Unit)
        if (holdPost) release.await()
        Result.success(ShownRanking(Ranking("a", "A", null, arrangement, tierLabels, 0, mine = true, rows = rows), created = true))
    }
}
