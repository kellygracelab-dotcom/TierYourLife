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
import com.artiuillab.tieryourlife.feature.community.domain.model.ListTranslations
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.lists.itemLayout
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RankingChoiceJourneyTest {
    @get:Rule val main = MainDispatcherRule()
    private val models = mutableListOf<CommunityListViewModel>()

    @After fun clear() = models.forEach { it.viewModelScope.cancel() }

    @Test fun lookingAtTheAuthorsFinishedRankingNeverOffers_andSixIsNotSeven_andDismissalSurvivesReopen() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        assertNull(vm.loaded().rankingOffer)
        vm.show(Showing.Mine)
        repeat(6) { vm.moveItem(it.toLong(), 0, it) }
        assertNull(vm.loaded().rankingOffer)
        vm.moveItem(6, 0, 6)
        assertEquals(7, vm.loaded().rankingOffer?.placed)
        assertEquals(0, fixture.shown)
        vm.choices.dismiss()
        val again = open(fixture)
        assertNull(again.loaded().rankingOffer)
        assertEquals(7, again.loaded().mine.tiers.first().items.size)
        assertEquals(0, fixture.shown)
    }

    @Test fun privateChoiceKeepsAnEditableLibraryCopy_andAnotherDeviceRestoresLaterMovesWithoutPromptOrPublication() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = false, languageTag = "")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertEquals(RankingChoice.Private, fixture.boards.saved?.rankingChoice)
        vm.moveItem(7, 1, 0)
        val anotherDevice = open(fixture, RankingDrafts.None)
        assertEquals(listOf(7L), anotherDevice.loaded().mine.tiers[1].items.map { it.id })
        assertNull(anotherDevice.loaded().rankingOffer)
        assertFalse(requireNotNull(fixture.boards.saved).isPublic)
        assertEquals(0, fixture.shown)
    }

    @Test fun completingSignInAfterTheGuestPublicButtonNeverPublishesWithoutAnotherExplicitAction() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = true, languageTag = "")
        assertEquals(ListNotice.SignInToFollow, nextNotice(vm))
        fixture.accounts.accounts.value = Account.SignedIn("me@example.com", null, uid = "me")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.let { state -> state.signedIn && !state.arriving } == true }
        assertEquals(0, fixture.shown)
        assertFalse(requireNotNull(fixture.boards.saved).isPublic)
        assertEquals(7, vm.loaded().mine.tiers.first().items.size)
    }

    @Test fun crossingTheThresholdAfterAnEarlierManualSaveAlsoKeepsTheOfferMemoryOnTheSharedCopy() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        vm.show(Showing.Mine)
        repeat(3) { vm.moveItem(it.toLong(), 0, it) }
        vm.saveToMyLists(Showing.Mine)
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertNull(fixture.boards.saved?.rankingChoice)
        repeat(4) { vm.moveItem((it + 3).toLong(), 0, it + 3) }
        assertEquals(RankingChoice.Offered, fixture.boards.saved?.rankingChoice)
        assertNull(open(fixture, RankingDrafts.None).loaded().rankingOffer)
        assertEquals(0, fixture.shown)
    }

    @Test fun explicitPublicChoicePublishesOnce_andWithdrawalKeepsBothPrivateCopyAndChoiceMemory() = runBlocking {
        val fixture = ChoiceFixture(signedIn = true)
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = true, languageTag = "")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.any { row -> row.mine } == true }
        until { fixture.boards.saved?.rankingOf != null }
        assertEquals(1, fixture.shown)
        assertEquals("abc", fixture.boards.saved?.rankingOf)
        vm.rankings.withdrawMine()
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.rankingCount == 0 }
        until { fixture.boards.saved?.rankingOf == null }
        assertNull(fixture.boards.saved?.rankingOf)
        val reopened = open(fixture, RankingDrafts.None)
        assertNull(reopened.loaded().rankingOffer)
        assertEquals(7, reopened.loaded().mine.tiers.first().items.size)
        assertEquals(RankingChoice.Ranking, fixture.boards.saved?.rankingChoice)
    }

    @Test fun savingTheAuthorsOrderingDoesNotReplaceTheReadersOwnPrivateProgress() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.saveToMyLists(Showing.Theirs)
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        val reopened = open(fixture)
        assertEquals(7, reopened.loaded().mine.tiers.first().items.size)
        assertEquals(20, fixture.boards.saved?.tiers?.first()?.items?.size)
        assertNull(fixture.boards.saved?.rankingChoice)
        assertEquals(0, fixture.shown)
    }

    @Test fun editedLibraryCopyIsNeverOverwrittenOrMistakenForTheUnchangedOriginal() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = false, languageTag = "")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        fixture.boards.saved = requireNotNull(fixture.boards.saved).copy(title = "My different criterion")
        vm.moveItem(7, 1, 0)
        assertEquals("My different criterion", fixture.boards.saved?.title)
        assertEquals(0, fixture.boards.saved?.tiers?.get(1)?.items?.size)
        assertEquals(1, open(fixture).loaded().mine.tiers[1].items.size)
    }

    @Test fun aWholeOfficialTranslationWithBaseFallbackRestoresTheSamePrivateCopyOnAnotherDevice() = runBlocking {
        val fixture = ChoiceFixture(translations = ListTranslations(mapOf("ru" to mapOf("Every A24 film" to "Все фильмы A24", "S" to "Лучшие", "Card 0" to "Карточка 0"))))
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = false, languageTag = "ru")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertEquals("Все фильмы A24", fixture.boards.saved?.title)
        val reopened = open(fixture, RankingDrafts.None).loaded()
        assertEquals(100L, reopened.savedId)
        assertEquals(7, reopened.mine.tiers.first().items.size)
        assertNull(reopened.rankingOffer)
        assertEquals(0, fixture.shown)
    }

    private suspend fun open(fixture: ChoiceFixture, drafts: RankingDrafts = fixture.drafts): CommunityListViewModel = CommunityListViewModel(
        fixture, fixture.boards, FakeAppPreferences(), SavedStateHandle(mapOf("publishedId" to "abc")),
        fixture.accounts, FakeOpenedLists(), FollowAfterSignIn(fixture.accounts), drafts,
    ).also {
        models += it
        it.state.firstWithin { state -> state is CommunityListUiState.Success }
    }

    private suspend fun until(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(10)
    }

    private fun placeSeven(vm: CommunityListViewModel) {
        vm.show(Showing.Mine)
        repeat(7) { vm.moveItem(it.toLong(), 0, it) }
    }
}

private fun CommunityListViewModel.loaded() = state.value as CommunityListUiState.Success

private class ChoiceFixture(signedIn: Boolean = false, translations: ListTranslations = ListTranslations()) : FakeCommunityRepository() {
    val boards = ChoiceBoards()
    val accounts = FakeAccountRepository().apply { if (signedIn) accounts.value = Account.SignedIn("me@example.com", null, uid = "me") }
    private val records = mutableMapOf<Pair<String?, String>, RankingDraft>()
    val drafts = object : RankingDrafts {
        override suspend fun read(listId: String, owner: String?) = records[owner to listId]
        override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) {
            records[owner to listId] = draft
        }
    }
    private val source = published.copy(
        summary = published.summary.copy(itemCount = 20, updatedAtMillis = 123),
        items = List(20) { TierItem(it.toLong(), "Card ${it / 2}", null, sourceIndex = it) },
        arrangement = List(20) { 0 },
        translations = translations,
    )

    @Volatile var shown = 0

    override suspend fun open(publishedId: String) = Result.success(source)
    override suspend fun showMyRanking(listId: String, arrangement: List<Int?>, rows: List<List<Int>>, tierLabels: List<String>): Result<ShownRanking> {
        shown++
        return Result.success(ShownRanking(Ranking("me", "Me", null, arrangement, tierLabels, 123, mine = true, rows = rows), created = true))
    }
}

private class ChoiceBoards : TierRepository by RecordingTierRepository() {
    @Volatile var saved: TierList? = null
    override suspend fun createRankingCopy(board: TierList, sourceId: String, sourceVersion: Long, choice: RankingChoice?): Long {
        saved = board.copy(
            id = 100, uid = "local-copy", takenFrom = sourceId, originVersion = sourceVersion, rankingChoice = choice,
            tiers = board.tiers.mapIndexed { row, tier -> tier.copy(id = 900L + row, items = tier.items.map { it.copy(id = 1000 + it.id) }) },
        )
        return 100
    }
    override suspend fun getTierListById(id: Long) = saved?.takeIf { it.id == id }
    override suspend fun getTierListByUid(uid: String) = saved?.takeIf { it.uid == uid }
    override suspend fun getAllTierLists() = listOfNotNull(saved)
    override suspend fun setRankingChoice(id: Long, choice: RankingChoice) {
        saved = saved?.copy(rankingChoice = choice)
    }
    override suspend fun setRankingOf(id: Long, listId: String?) {
        saved = saved?.copy(rankingOf = listId)
    }
    override suspend fun arrangeBoard(id: Long, expected: Map<Long, List<Long>>, target: Map<Long, List<Long>>): Boolean {
        val before = saved ?: return false
        if (before.itemLayout() != expected) return false
        val cards = before.tiers.flatMap { it.items }.associateBy { it.id }
        saved = before.copy(tiers = before.tiers.map { tier -> tier.copy(items = target.getValue(tier.id).map(cards::getValue)) })
        return true
    }
}
