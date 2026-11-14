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
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RankingChoiceJourneyTest {
    @get:Rule val main = MainDispatcherRule()
    private val models = mutableListOf<CommunityListViewModel>()

    @After fun clear() = models.forEach { it.viewModelScope.cancel() }

    @Test fun lookingAtTheAuthorsFinishedRankingNeverOffers_andNineteenIsNotTwenty_andDismissalSurvivesReopen() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        assertNull(vm.loaded().rankingOffer)
        vm.show(Showing.Mine)
        repeat(19) { vm.moveItem(it.toLong(), 0, it) }
        assertNull(vm.loaded().rankingOffer)
        vm.moveItem(19, 0, 19)
        assertEquals(20, vm.loaded().rankingOffer?.placed)
        assertEquals(0, fixture.shown)
        vm.choices.dismiss()
        val again = open(fixture)
        assertNull(again.loaded().rankingOffer)
        assertEquals(20, again.loaded().mine.tiers.first().items.size)
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

    @Test fun placingTheLastCardAfterAnEarlierManualSaveAlsoKeepsTheOfferMemoryOnTheSharedCopy() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        vm.show(Showing.Mine)
        repeat(3) { vm.moveItem(it.toLong(), 0, it) }
        vm.saveToMyLists()
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertNull(fixture.boards.saved?.rankingChoice)
        repeat(17) { vm.moveItem((it + 3).toLong(), 0, it + 3) }
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

    // Coordinator, parity with the website: a page opened over a copy already in Rankings knows it,
    // so its menu says Update and Remove, and Remove works before the Rankings are read.
    @Test fun aCopyAlreadyInRankings_opensKnowingIt_andComesOutFromTheMenu() = runBlocking {
        val fixture = ChoiceFixture(signedIn = true)
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = true, languageTag = "")
        until { fixture.boards.saved?.rankingOf != null }

        val reopened = open(fixture, RankingDrafts.None)
        assertTrue(reopened.loaded().rankingShown)
        assertTrue(reopened.loaded().rankings.isEmpty())

        reopened.rankings.withdrawMine()
        assertFalse((reopened.state.value as CommunityListUiState.Success).rankingShown)
        until { fixture.boards.saved?.rankingOf == null }
        assertEquals(1, fixture.withdrawn)
    }

    // The game's result is on the board: the offer comes straight away, however many cards it placed.
    @Test fun aGameEndingOnTheBoardOffersToShowIt_once() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        val played = vm.loaded().mine.let { mine ->
            val cards = mine.tiers.flatMap { it.items }
            mine.copy(tiers = mine.tiers.mapIndexed { i, tier -> if (i == 0) tier.copy(items = cards.take(5)) else if (tier.isPool) tier.copy(items = cards.drop(5)) else tier.copy(items = emptyList()) })
        }
        vm.putInMine(played)
        assertEquals(5, vm.loaded().rankingOffer?.placed)
        vm.choices.dismiss()
        vm.putInMine(played, undone = true)
        vm.putInMine(played)
        assertNull(vm.loaded().rankingOffer)
        assertEquals(0, fixture.shown)
    }

    // What is saved is what the reader sees as theirs, and once saved, the list is "Saved" and opens that one copy.
    @Test fun savingAfterArrangingKeepsTheReadersOwnArrangement_andTheListIsSavedFromThenOn() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.saveToMyLists()
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertEquals(7, fixture.boards.saved?.tiers?.first()?.items?.size)
        assertEquals(1, fixture.boards.created)
        val reopened = open(fixture, RankingDrafts.None)
        assertEquals(100L, reopened.loaded().savedId)
        reopened.saveToMyLists()
        assertEquals(1, fixture.boards.created)
        assertEquals(0, fixture.shown)
    }

    // Danylo's Eurovision twice in My lists: a copy saved as the author ranked it carried no choice and was not found again.
    @Test fun aCopySavedWithoutAChoice_isFoundAgain_soTheListOpensItInsteadOfSavingAnother() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        vm.saveToMyLists()
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertNull(fixture.boards.saved?.rankingChoice)
        assertEquals(20, fixture.boards.saved?.tiers?.first()?.items?.size)
        val reopened = open(fixture, RankingDrafts.None)
        assertEquals(100L, reopened.loaded().savedId)
        assertEquals(20, reopened.loaded().mine.tiers.first().items.size)
        reopened.saveToMyLists()
        assertEquals(1, fixture.boards.created)
    }

    // The author published again since: the copy of the older version is still this reader's one copy, left as it was.
    @Test fun aCopyOfAnOlderVersion_isStillTheSavedOne() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        vm.saveToMyLists()
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        fixture.boards.saved = requireNotNull(fixture.boards.saved).copy(originVersion = 1)
        val reopened = open(fixture, RankingDrafts.None)
        assertEquals(100L, reopened.loaded().savedId)
        reopened.moveItem(0, 0, 0)
        reopened.saveToMyLists()
        assertEquals(1, fixture.boards.created)
        assertEquals(1L, fixture.boards.saved?.originVersion)
    }

    @Test fun theCopyTakesTheListsCoverFromTheCommunity_notACollage() = runBlocking {
        val fixture = ChoiceFixture(cover = "https://tieryourlife.web.app/img/original/published/abc/cover")
        val vm = open(fixture)
        vm.saveToMyLists()
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        assertEquals("https://tieryourlife.web.app/img/original/published/abc/cover", fixture.boards.saved?.coverImageUrl)
        assertEquals(emptyList<String>(), fixture.boards.saved?.coverImageUrls)
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

    // SPEC list-flow Н2: a copy changed on its board still gives Mine its places when nothing is arranged on this phone.
    @Test fun aChangedCopy_givesMineItsPlaces_whenNothingIsArrangedHere() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = false, languageTag = "")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        fixture.boards.saved = changed(requireNotNull(fixture.boards.saved))

        val elsewhere = open(fixture, RankingDrafts.None).loaded()

        assertEquals((0L..6L).toList(), elsewhere.mine.tiers.first().items.map { it.id })
        assertEquals(13, elsewhere.mine.tiers.last().items.size)
        assertEquals(100L, elsewhere.savedId)
    }

    @Test fun aCopyThatArrivesAfterThePage_givesMineItsPlaces_butNeverOverwritesWhatIsArranged() = runBlocking {
        val fixture = ChoiceFixture()
        val vm = open(fixture)
        placeSeven(vm)
        vm.choices.choose(public = false, languageTag = "")
        vm.state.firstWithin { (it as? CommunityListUiState.Success)?.savedId != null && !it.saving }
        val copy = changed(requireNotNull(fixture.boards.saved))
        fixture.boards.saved = null
        val sync = SyncRuns()
        val late = open(fixture, RankingDrafts.None, sync)
        assertFalse(late.loaded().arranged)

        fixture.boards.saved = copy
        sync.finish()
        val shown = late.state.firstWithin { (it as CommunityListUiState.Success).arranged } as CommunityListUiState.Success
        assertEquals((0L..6L).toList(), shown.mine.tiers.first().items.map { it.id })

        late.moveItem(7, 1, 0)
        sync.finish()
        delay(200)
        assertEquals(listOf(7L), late.loaded().mine.tiers[1].items.map { it.id })
    }

    /** Its first tier renamed and a card of the reader's own added: no longer exactly the list. */
    private fun changed(copy: TierList) = copy.copy(
        tiers = copy.tiers.mapIndexed { at, tier ->
            if (at == 0) tier.copy(label = "Love", items = tier.items + TierItem(5000, "Mine only", null)) else tier
        },
    )

    private suspend fun open(fixture: ChoiceFixture, drafts: RankingDrafts = fixture.drafts, sync: BoardSync = BoardSync.None): CommunityListViewModel = CommunityListViewModel(
        fixture, fixture.boards, FakeAppPreferences(), SavedStateHandle(mapOf("publishedId" to "abc")),
        fixture.accounts, FakeOpenedLists(), FollowAfterSignIn(fixture.accounts), drafts, sync,
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

private class ChoiceFixture(signedIn: Boolean = false, translations: ListTranslations = ListTranslations(), cover: String? = null) : FakeCommunityRepository() {
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
        summary = published.summary.copy(itemCount = 20, updatedAtMillis = 123, coverImageUrl = cover),
        items = List(20) { TierItem(it.toLong(), "Card ${it / 2}", null, sourceIndex = it) },
        arrangement = List(20) { 0 },
        translations = translations,
    )

    @Volatile var shown = 0

    @Volatile var withdrawn = 0

    override suspend fun open(publishedId: String) = Result.success(source)
    override suspend fun withdrawRanking(listId: String): Result<Unit> {
        withdrawn++
        return Result.success(Unit)
    }
    override suspend fun showMyRanking(listId: String, arrangement: List<Int?>, rows: List<List<Int>>, tierLabels: List<String>): Result<ShownRanking> {
        shown++
        return Result.success(ShownRanking(Ranking("me", "Me", null, arrangement, tierLabels, 123, mine = true, rows = rows), created = true))
    }
}

private class ChoiceBoards : TierRepository by RecordingTierRepository() {
    @Volatile var saved: TierList? = null

    @Volatile var created = 0
    override suspend fun createRankingCopy(board: TierList, sourceId: String, sourceVersion: Long, choice: RankingChoice?): Long {
        created++
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

private class SyncRuns : BoardSync {
    private val runs = MutableSharedFlow<SyncReport>(extraBufferCapacity = 4)
    override val finished: Flow<SyncReport> = runs
    override suspend fun sync() = SyncReport(signedIn = false)

    /** Once the page listens: a run that ends before anyone listens is not told to anyone. */
    suspend fun finish() {
        withTimeout(5_000) { runs.subscriptionCount.first { it > 0 } }
        runs.emit(SyncReport(signedIn = true))
    }
}
