package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.TargetIsStaff
import com.artiuillab.tieryourlife.feature.community.domain.model.TooManyFollowed
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CommunityListViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val made = mutableListOf<CommunityListViewModel>()

    /** Each view model's coroutines end with its test, the account's watch among them. */
    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    @Test
    fun opening_putsTheAuthorsCardsInThePool_andLeavesEveryTierEmpty() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        assertEquals("Olena M.", state.authorName)
        assertEquals(2, state.list.tiers.first { it.isPool }.items.size)
        assertTrue(state.list.tiers.filterNot { it.isPool }.all { it.items.isEmpty() })
    }

    // Ranking someone else's list writes nothing: backing out has to cost the
    // reader only the arrangement they were told was unsaved.
    @Test
    fun ranking_changesNothingInTheDatabase() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.show(Showing.Mine)
        val poolItem = loaded.mine.tiers.first { it.isPool }.items.first()
        val target = loaded.mine.tiers.first { !it.isPool }

        viewModel.moveItem(poolItem.id, target.id, 0)
        val ranked = viewModel.state.firstWithin {
            (it as? CommunityListUiState.Success)?.arranged == true
        } as CommunityListUiState.Success

        assertEquals(1, ranked.mine.tiers.first { it.id == target.id }.items.size)
        assertTrue(tiers.templates.isEmpty())
    }

    // A move on the author's arrangement used to be ignored. Now it starts the
    // reader's own version from what they were looking at, with that one move made.
    @Test
    fun movingACardOnTheirs_startsMineFromTheirs_withThatMove_andSwitchesToMine() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        assertEquals(Showing.Theirs, loaded.showing)

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, after.showing)
        assertTrue(after.arranged)
        assertEquals(listOf("Hereditary"), after.mine.tiers.first { it.id == 0L }.items.map { it.title })
        assertEquals(listOf("Moonlight"), after.mine.tiers.first { it.id == 1L }.items.map { it.title })
        assertEquals(loaded.list, after.list)
        assertEquals(ListNotice.StartedFromTheirs("Olena M.", replaced = false), withTimeout(5_000) { viewModel.listNotices.first() })
    }

    @Test
    fun undo_afterStartingFromTheirs_putsMineBack_andTheirsOnScreen() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        viewModel.undoStartFromTheirs()

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Theirs, after.showing)
        assertEquals(loaded.mine, after.mine)
        assertFalse(after.arranged)
    }

    @Test
    fun startingFromTheirsOverAnArrangedMine_saysItReplacedIt_andUndoBringsItBack() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null)))
        val mine = arranged(viewModel).mine
        viewModel.show(Showing.Theirs)

        viewModel.moveItem(itemId = 0, toTierId = 1, toPosition = 0)
        val notice = withTimeout(5_000) { viewModel.listNotices.first() }
        viewModel.undoStartFromTheirs()

        assertEquals(ListNotice.StartedFromTheirs("Olena M.", replaced = true), notice)
        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(mine, after.mine)
        assertTrue(after.arranged)
    }

    // Theirs is whoever's placing is on screen: a chosen reader's is the one copied, under their words.
    @Test
    fun movingOnAChosenReadersRanking_startsMineFromTheirs() = runBlocking {
        val kyle = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)
        val viewModel = viewModel(community = TestCommunity(rankings = listOf(kyle), arrangement = listOf(0, null)))
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(kyle)

        viewModel.moveItem(itemId = 0, toTierId = 0, toPosition = 1)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(Showing.Mine, after.showing)
        assertEquals(listOf("Top", "Next"), after.mine.tiers.filterNot { it.isPool }.map { it.label })
        assertEquals(listOf("Moonlight", "Hereditary"), after.mine.tiers.first { it.id == 0L }.items.map { it.title })
        assertEquals("Kyle H.", (withTimeout(5_000) { viewModel.listNotices.first() } as ListNotice.StartedFromTheirs).name)
    }

    // A template, every card left in the pool: no empty half to switch to, nothing to choose on saving.
    @Test
    fun aListWhoseAuthorPlacedNothing_opensOnMine_withOneHalf() = runBlocking {
        val community = TestCommunity(arrangement = listOf(null, null), summary = { it.copy(authorUid = "", authorName = "", anonymous = true) })
        val viewModel = viewModel(community = community)

        val state = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        assertEquals(Showing.Mine, state.showing)
        assertFalse(state.knowsTheirs)
        assertFalse(state.twoHalves)
    }

    @Test
    fun aReadersRankingChosenOnATemplate_isTheOtherHalf_untilTheAuthorsEmptyOneIsChosenBack() = runBlocking {
        val kyle = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)
        val viewModel = viewModel(community = TestCommunity(rankings = listOf(kyle), arrangement = listOf(null, null)))
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }

        viewModel.rankings.choose(kyle)
        val chosen = viewModel.state.value as CommunityListUiState.Success
        viewModel.rankings.choose(null)
        val back = viewModel.state.value as CommunityListUiState.Success

        assertTrue(chosen.twoHalves)
        assertEquals(Showing.Theirs, chosen.showing)
        assertFalse(back.twoHalves)
        assertEquals(Showing.Mine, back.showing)
    }

    @Test
    fun anAnonymousAuthorsArrangement_isStartedFromWithoutAName() = runBlocking {
        val community = TestCommunity(arrangement = listOf(0, null), summary = { it.copy(authorUid = "", authorName = "", anonymous = true) })
        val viewModel = viewModel(community = community)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        assertEquals(ListNotice.StartedFromTheirs(null, replaced = false), withTimeout(5_000) { viewModel.listNotices.first() })
    }

    @Test
    fun onMyOwnList_aMoveChangesNothing() = runBlocking {
        val viewModel = viewModel(community = TestCommunity(arrangement = listOf(0, null), summary = { it.copy(mine = true) }))
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        viewModel.moveItem(itemId = 1, toTierId = 1, toPosition = 0)

        assertEquals(loaded.copy(ownBoardLookedUp = true), viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.ownBoardLookedUp == true })
    }

    @Test
    fun draggingWhileYourOwnIsShown_movesTheCardAndLeavesTheirsAlone() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        viewModel.show(Showing.Mine)
        val poolItem = loaded.mine.tiers.first { it.isPool }.items.first()
        val target = loaded.mine.tiers.first { !it.isPool }

        viewModel.moveItem(poolItem.id, target.id, 0)

        val after = viewModel.state.firstWithin {
            (it as? CommunityListUiState.Success)?.arranged == true
        } as CommunityListUiState.Success
        assertEquals(1, after.mine.tiers.first { it.id == target.id }.items.size)
        assertEquals(loaded.list.tiers.map { tier -> tier.items.size }, after.list.tiers.map { it.items.size })
    }

    @Test
    fun aGamesResult_becomesMine_onScreen_andStillUnsaved() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val played = loaded.mine.copy(
            tiers = loaded.mine.tiers.map { tier ->
                when {
                    tier.isPool -> tier.copy(items = emptyList())
                    tier.id == 0L -> tier.copy(items = published.items)
                    else -> tier
                }
            },
        )

        viewModel.putInMine(played)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(played, after.mine)
        assertEquals(Showing.Mine, after.showing)
        assertTrue(after.arranged)
        assertEquals(loaded.list, after.list)
        assertEquals(null, after.savedId)
    }

    // A24: the undo of a result put in Mine left Mine on screen and marked as arranged.
    @Test
    fun undoingAGamesResult_putsBackWhatWasOnScreen() = runBlocking {
        val viewModel = viewModel()
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
        val played = loaded.mine.copy(
            tiers = loaded.mine.tiers.map { tier ->
                when {
                    tier.isPool -> tier.copy(items = emptyList())
                    tier.id == 0L -> tier.copy(items = published.items)
                    else -> tier
                }
            },
        )
        viewModel.putInMine(played)

        viewModel.putInMine(loaded.mine, undone = true)

        val after = viewModel.state.value as CommunityListUiState.Success
        assertEquals(loaded.mine, after.mine)
        assertEquals(loaded.showing, after.showing)
        assertEquals(loaded.arranged, after.arranged)
    }

    // Keeping a list the way its author ranked it is a reason to save it, not
    // a mistake to correct.
    @Test
    fun savingTheirs_keepsTheAuthorsArrangement() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        // The switch shows the reader's own half; the choice at saving is what counts.
        viewModel.show(Showing.Mine)

        viewModel.saveToMyLists(Showing.Theirs)

        val theirs = viewModel.state.firstWithin { it.saved() } as CommunityListUiState.Success
        val template = tiers.templates.single()
        assertEquals(
            theirs.list.tiers.filterNot { it.isPool }.map { it.items.size },
            template.tiers.map { tier -> tier.items.size },
        )
        // Only what was left unranked goes to the pool. Every card used to,
        // which is how a saved copy came out with its tiers empty.
        assertEquals(theirs.list.tiers.first { it.isPool }.items.map { it.title }, template.items.map { it.title })
    }

    @Test
    fun saving_copiesTheListUnderTheReadersOwnRoof_withTheAuthorAttached() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.saveToMyLists(Showing.Mine)
        val saved = viewModel.state.firstWithin { it.saved() } as CommunityListUiState.Success

        assertEquals(7L, saved.savedId)
        val template = tiers.templates.single()
        assertEquals("Every A24 film", template.title)
        assertEquals("Olena M.", template.authorName)
        assertEquals(2, template.items.size)
        assertEquals(2, template.tiers.size)
        assertEquals(listOf(null, "© Jane Doe, CC BY"), template.items.map { it.credit })
    }

    @Test
    fun aListThatCannotBeOpened_saysSoRatherThanShowingAnEmptyBoard() = runBlocking {
        val viewModel = viewModel(community = FailingCommunityRepository())

        val state = viewModel.state.firstWithin { it is CommunityListUiState.Error }

        assertEquals(CommunityListUiState.Error, state)
    }

    @Test
    fun whatTheCardShowed_isReadyBeforeTheListIs() {
        val viewModel = viewModel(
            handle = SavedStateHandle(
                mapOf(
                    "publishedId" to "abc",
                    "title" to "Every A24 film",
                    "authorName" to "Olena M.",
                    "itemCount" to 2,
                    "tierColors" to "#B03A32,#C06A25",
                ),
            ),
        )

        assertEquals(
            ListPreview(title = "Every A24 film", authorName = "Olena M.", itemCount = 2, tierColors = listOf("#B03A32", "#C06A25")),
            viewModel.preview,
        )
    }

    @Test
    fun aListOpenedByIdAlone_hasNothingToDrawAhead() {
        assertEquals(null, viewModel().preview)
    }

    // A reader's placing is the same cards under their own words; saving it keeps it, and the copy knows where it came from.
    @Test
    fun choosingAReadersRanking_showsTheirPlacing_andSavingTheirsKeepsIt() = runBlocking {
        val olena = Ranking("r1", "Kyle H.", null, arrangement = listOf(1, 0), tierLabels = listOf("Top", "Next"), updatedAtMillis = 0)
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(community = TestCommunity(rankings = listOf(olena)), tiers = tiers)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(olena)
        val shown = (viewModel.state.value as CommunityListUiState.Success).shown

        assertEquals(listOf("Top", "Next"), shown.tiers.filterNot { it.isPool }.map { it.label })
        assertEquals(listOf("Moonlight"), shown.tiers[0].items.map { it.title })
        assertEquals(listOf("Hereditary"), shown.tiers[1].items.map { it.title })

        viewModel.saveToMyLists(Showing.Theirs)
        viewModel.state.firstWithin { it.saved() }
        val kept = tiers.templates.single()
        assertEquals("abc", kept.takenFrom)
        assertEquals(listOf("Top", "Next"), kept.tiers.map { it.label })
    }

    @Test
    fun withdrawingMyRanking_takesItOutAtOnce_andPutsItBackIfRefused() = runBlocking {
        val mine = Ranking("me", "Me", null, arrangement = listOf(0, 0), tierLabels = emptyList(), updatedAtMillis = 0, mine = true)
        val community = TestCommunity(rankings = listOf(mine), refusesWithdrawal = true)
        val viewModel = viewModel(community = community)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }
        viewModel.rankings.choose(mine)

        viewModel.rankings.withdrawMine()
        val gone = viewModel.state.value as CommunityListUiState.Success
        assertEquals(0, gone.rankingCount)
        assertEquals(null, gone.chosenRanking)
        assertEquals(gone.list, gone.theirs)

        // Only now does the server refuse: the optimistic state above must not depend on who is faster.
        community.refusal.complete(Unit)
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankingCount == 1 }
        assertEquals(listOf("abc"), community.withdrawn)
    }

    private fun CommunityListUiState.saved() = (this as? CommunityListUiState.Success)?.savedId != null

    private val myBoard = TierList(id = 12, title = "Every A24 film", tiers = emptyList(), publishedId = "abc")

    private fun route(mine: Boolean = false, fromBoard: Boolean = false) =
        SavedStateHandle(mapOf("publishedId" to "abc", "mine" to mine, "fromBoard" to fromBoard))

    // Danylo tapped his own list and got a page of it with a button to the board: the board is where it changes, so it opens.
    @Test
    fun aCardThatSaysTheListIsMine_opensMyBoard_withoutAskingTheServer() = runBlocking {
        val community = ownCommunity()
        val viewModel = viewModel(community = community, tiers = RecordingTierRepository(boards = listOf(myBoard)), handle = route(mine = true))

        assertEquals(ListNotice.OpenBoard(12), nextNotice(viewModel))
        assertEquals(0, community.opens)
    }

    // Opened by id alone, it is only known to be theirs once the server says so: until then the page waits, and it is never drawn.
    @Test
    fun myOwnAnonymousList_openedByIdAlone_opensMyBoard_withoutEverDrawingThePage() = runBlocking {
        val community = TestCommunity(summary = { it.copy(authorUid = "", authorName = "", anonymous = true, mine = true) })
        val viewModel = viewModel(community = community, tiers = RecordingTierRepository(boards = listOf(myBoard)))

        assertEquals(ListNotice.OpenBoard(12), nextNotice(viewModel))
        assertEquals(1, community.opens)
        assertEquals(CommunityListUiState.Loading, viewModel.state.value)
        assertEquals(0, community.followStateAsked)
    }

    @Test
    fun myOwnList_keptFromALastVisit_opensMyBoard_withoutDrawingTheKeptCopy() = runBlocking {
        val kept = published.copy(summary = published.summary.copy(mine = true))
        val community = ownCommunity()
        val viewModel = viewModel(community = community, tiers = RecordingTierRepository(boards = listOf(myBoard)), openedLists = FakeOpenedLists(kept))

        assertEquals(ListNotice.OpenBoard(12), nextNotice(viewModel))
        assertEquals(0, community.opens)
        assertEquals(CommunityListUiState.Loading, viewModel.state.value)
    }

    @Test
    fun myOwnList_askedForFromItsBoard_staysAsThePage() = runBlocking {
        val viewModel = viewModel(community = ownCommunity(), tiers = RecordingTierRepository(boards = listOf(myBoard)), handle = route(mine = true, fromBoard = true))

        val state = lookedUp(viewModel)

        assertTrue(state.own)
        assertTrue(state.fromBoard)
        assertEquals(12L, state.ownBoardId)
        assertEquals(null, state.ownBoardAction)
        assertEquals(null, withTimeoutOrNull(300) { viewModel.listNotices.first() })
    }

    @Test
    fun somebodyElsesList_neverOpensABoard() = runBlocking {
        val viewModel = viewModel(tiers = RecordingTierRepository(boards = listOf(myBoard)))

        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow != null }

        assertEquals(null, withTimeoutOrNull(300) { viewModel.listNotices.first() })
    }

    @Test
    fun myOwnList_offersNothingToSave_hideOrReport() = runBlocking {
        val tiers = RecordingTierRepository()
        val preferences = FakeAppPreferences()
        val community = TestCommunity(summary = { it.copy(mine = true) })
        val viewModel = viewModel(community = community, tiers = tiers, preferences = preferences)
        val state = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        viewModel.saveToMyLists(Showing.Theirs)
        viewModel.report(ReportReason.Spam, null)

        assertTrue(state.own)
        assertEquals(null, state.ownBoardId)
        assertTrue(tiers.templates.isEmpty())
        assertTrue(preferences.hiddenListIds().isEmpty())
        assertTrue(community.reported.isEmpty())
    }

    @Test
    fun aModeratorReportingAListByStaff_isToldOnlyTheOwnerCan() = runBlocking {
        val preferences = FakeAppPreferences()
        val viewModel = viewModel(community = TestCommunity(reportRefusal = TargetIsStaff()), preferences = preferences)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.report(ReportReason.Spam, null)

        assertEquals(UserMessage.TargetIsStaff, withTimeout(5_000) { viewModel.userMessages.first() })
        assertTrue("back in the feed", preferences.hiddenListIds().isEmpty())
    }

    private fun ownCommunity(anonymous: Boolean = false, unpublishFails: Boolean = false) = TestCommunity(
        arrangement = listOf(0, null),
        unpublishFails = unpublishFails,
        summary = { if (anonymous) it.copy(authorUid = "", authorName = "", anonymous = true, mine = true) else it.copy(mine = true) },
    )

    private val trashed = TrashEntry.DeletedList(id = 5, title = "Every A24 film", itemCount = 2, deletedAtMillis = 0, publishedId = "abc")

    @Test
    fun myOwnList_withItsBoardInTheTrash_offersToRestoreIt_andTheRestoredBoardOpens() = runBlocking {
        val tiers = RecordingTierRepository(trash = listOf(trashed))
        val viewModel = viewModel(community = ownCommunity(), tiers = tiers, handle = route(mine = true))
        assertEquals(OwnBoardAction.Restore, lookedUp(viewModel).ownBoardAction)

        viewModel.ownList.restore()

        assertEquals(ListNotice.OpenBoard(5), nextNotice(viewModel))
        assertEquals(listOf(5L), tiers.restored)
    }

    // Owned, not taken: no takenFrom, still out as this list so Update sends to the same link, and as nameless as it went out.
    @Test
    fun myOwnList_withNoBoardAnywhere_getsItBackAsMyOwnBoard_andOpensIt() = runBlocking {
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(community = ownCommunity(anonymous = true), tiers = tiers)
        assertEquals(OwnBoardAction.GetBack, lookedUp(viewModel).ownBoardAction)

        viewModel.ownList.getBack()

        assertEquals(ListNotice.OpenBoard(7), nextNotice(viewModel))
        val template = tiers.templates.single()
        assertEquals(null, template.takenFrom)
        assertEquals("", template.authorName)
        assertEquals(listOf("Hereditary"), template.tiers.first().items.map { it.title })
        assertEquals(listOf("Moonlight"), template.items.map { it.title })
        assertEquals(listOf(7L to "abc"), tiers.publishedIds)
        assertEquals(listOf(7L to true), tiers.anonymity)
        assertEquals(listOf(7L to ListCategory.FilmTv.id), tiers.categories)
    }

    @Test
    fun takingMyListDown_unpublishesIt_forgetsItOnTheTrashedBoard_andLeaves() = runBlocking {
        val tiers = RecordingTierRepository(trash = listOf(trashed))
        val community = ownCommunity()
        val viewModel = viewModel(community = community, tiers = tiers)
        lookedUp(viewModel)

        viewModel.ownList.takeDown()

        assertEquals(ListNotice.TakenDown, nextNotice(viewModel))
        assertEquals(listOf("abc"), community.unpublished)
        assertEquals(5L, tiers.clearedPublishedId)
    }

    @Test
    fun aTakeDownTheServerRefuses_saysSo_andTheBoardKeepsItsId() = runBlocking {
        val tiers = RecordingTierRepository(trash = listOf(trashed))
        val viewModel = viewModel(community = ownCommunity(unpublishFails = true), tiers = tiers)
        lookedUp(viewModel)

        viewModel.ownList.takeDown()

        assertEquals(UserMessage.ActionFailed, withTimeout(5_000) { viewModel.userMessages.first() })
        assertEquals(null, tiers.clearedPublishedId)
    }

    @Test
    fun somebodyElsesList_isTheirs_andTheirAuthorCanBeFollowed() = runBlocking {
        val community = TestCommunity()
        val viewModel = viewModel(community = community)

        val state = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow != null } as CommunityListUiState.Success

        assertFalse(state.own)
        assertEquals(1, community.followStateAsked)
    }

    // The open answer was read before the follow landed; the server followed, so the button must not go back.
    @Test
    fun aFollowTappedWhileTheListIsOnItsWay_isNotUndoneByTheAnswer() = runBlocking {
        val community = TestCommunity(holdOpen = true, summary = { it.copy(viewerFollowsAuthor = false, rankingCount = 5) })
        val kept = published.copy(summary = published.summary.copy(viewerFollowsAuthor = false))
        val viewModel = viewModel(community = community, accounts = signedIn, openedLists = FakeOpenedLists(kept))
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow != null }

        viewModel.toggleFollow()
        community.openHeld.complete(Unit)
        val state = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankingCount == 5 } as CommunityListUiState.Success

        assertEquals(true, state.follow?.following)
    }

    // Rows name cards by their place in the list; a kept copy the author has changed since would place them under the wrong items.
    @Test
    fun showingMyRanking_fromAKeptCopyTheAuthorChanged_sendsNothing() = runBlocking {
        val community = TestCommunity().apply { offline = true }
        val kept = published.copy(items = published.items + TierItem(2, "Pearl", null), summary = published.summary.copy(itemCount = 3))
        val viewModel = viewModel(community = community, accounts = signedIn, openedLists = FakeOpenedLists(kept))
        arranged(viewModel)

        community.offline = false
        viewModel.showMyRanking()

        assertEquals(UserMessage.ActionFailed, withTimeout(5_000) { viewModel.userMessages.first() })
        assertTrue(community.shownRankings.isEmpty())
    }

    @Test
    fun showingMyRanking_fromAKeptCopyStillTheSame_readsItAgainAndSends() = runBlocking {
        val community = TestCommunity().apply { offline = true }
        val viewModel = viewModel(community = community, accounts = signedIn, openedLists = FakeOpenedLists(published))
        arranged(viewModel)

        community.offline = false
        viewModel.showMyRanking()

        assertEquals(RankingNotice.Shown, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertEquals(listOf(listOf(listOf(1), emptyList())), community.shownRows)
    }

    @Test
    fun aGuestsFollow_opensTheSignIn_andTheSignInFollows() = runBlocking {
        val accounts = FakeAccountRepository()
        val viewModel = viewModel(accounts = accounts)
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow != null }

        viewModel.toggleFollow()

        assertEquals(ListNotice.SignInToFollow, nextNotice(viewModel))
        assertEquals(false, (viewModel.state.value as CommunityListUiState.Success).follow?.following)
        accounts.accounts.value = FakeAccountRepository.signedIn
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow?.following == true }
        Unit
    }

    @Test
    fun aRefusedFollow_saysWhy() = runBlocking {
        val community = object : FakeCommunityRepository() {
            override suspend fun open(id: String): Result<PublishedList> = Result.success(published)

            override suspend fun follow(authorUid: String): Result<Unit> = Result.failure(TooManyFollowed())
        }
        val viewModel = viewModel(community = community, accounts = signedIn)
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.follow != null }

        viewModel.toggleFollow()

        assertEquals(ListNotice.FollowRefused(FollowRefusal.TooMany), nextNotice(viewModel))
    }

    @Test
    fun theListsVersion_isWhenItWasLastPublished() = runBlocking {
        val community = TestCommunity(summary = { it.copy(updatedAtMillis = 1_234L) })
        val viewModel = viewModel(community = community)

        val state = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        assertEquals(1_234L, state.version)
    }

    private fun viewModel(
        community: CommunityRepository = TestCommunity(),
        tiers: RecordingTierRepository = RecordingTierRepository(),
        handle: SavedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
        preferences: FakeAppPreferences = FakeAppPreferences(),
        accounts: FakeAccountRepository = FakeAccountRepository(),
        openedLists: FakeOpenedLists = FakeOpenedLists(),
        drafts: com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts = com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts.None,
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = preferences,
        savedStateHandle = handle,
        accounts = accounts,
        openedLists = openedLists,
        afterSignIn = FollowAfterSignIn(accounts),
        drafts = drafts,
    ).also { made += it }

    private val signedIn = FakeAccountRepository(Account.SignedIn(email = "ann@example.com", photoUrl = null))

    @Test
    fun showingMyRanking_sendsMineInTheListsOrder_andPutsMeFirstAsYou() = runBlocking {
        val community = TestCommunity()
        val viewModel = viewModel(community = community, accounts = signedIn)
        assertEquals(0, arranged(viewModel).rankingCount)

        viewModel.showMyRanking()

        assertEquals(RankingNotice.Shown, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertEquals(listOf(listOf(null, 0) to listOf("S", "A")), community.shownRankings)
        assertEquals(listOf(listOf(listOf(1), emptyList())), community.shownRows)
        val state = viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true } as CommunityListUiState.Success
        assertTrue(state.rankings.first().mine)
        assertEquals(1, state.rankingCount)
        assertFalse(state.sendingRanking)
    }

    @Test
    fun showingItAgain_changesTheOneShown_withoutCountingItTwice() = runBlocking {
        val community = TestCommunity(rankings = listOf(myOldRanking), shownIsNew = false)
        val viewModel = viewModel(community = community, accounts = signedIn)
        arranged(viewModel)
        viewModel.rankings.load()
        viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.rankings?.isNotEmpty() == true }

        viewModel.showMyRanking()

        withTimeout(5_000) { viewModel.rankingNotices.first() }
        val state = viewModel.state.value as CommunityListUiState.Success
        assertEquals(1, state.rankings.size)
        assertEquals(listOf(null, 0), state.rankings.single().arrangement)
        assertEquals(1, state.rankingCount)
    }

    @Test
    fun nothingArranged_nothingToShow() = runBlocking {
        val community = TestCommunity()
        val viewModel = viewModel(community = community, accounts = signedIn)
        val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success

        viewModel.showMyRanking()

        assertFalse(loaded.canShowRanking)
        assertTrue(community.shownRankings.isEmpty())
    }

    @Test
    fun aGuest_isAskedToSignIn_andNothingIsSent() = runBlocking {
        val community = TestCommunity()
        val viewModel = viewModel(community = community)
        assertFalse(arranged(viewModel).signedIn)

        viewModel.showMyRanking()

        assertEquals(RankingNotice.SignInFirst, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertTrue(community.shownRankings.isEmpty())
    }

    @Test
    fun signingInWhileTheListIsOpen_letsTheRankingBeShown() = runBlocking {
        val accounts = FakeAccountRepository()
        val viewModel = viewModel(accounts = accounts)
        arranged(viewModel)

        accounts.accounts.value = Account.SignedIn(email = "ann@example.com", photoUrl = null)

        assertTrue((viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.signedIn == true } as CommunityListUiState.Success).canShowRanking)
    }

    @Test
    fun myOwnList_isNeverSentAsMyRanking() = runBlocking {
        val community = TestCommunity(summary = { it.copy(mine = true) })
        val viewModel = viewModel(community = community, accounts = signedIn)
        viewModel.state.firstWithin { it is CommunityListUiState.Success }

        viewModel.putInMine(TierList(id = 0, title = "x", tiers = emptyList()))
        viewModel.showMyRanking()

        assertFalse((viewModel.state.value as CommunityListUiState.Success).canShowRanking)
        assertTrue(community.shownRankings.isEmpty())
    }

    @Test
    fun aSessionTheServerNoLongerKnows_isAskedToSignInAgain() = runBlocking {
        val community = TestCommunity(showRefusal = RankingRefusal.NotSignedIn)
        val viewModel = viewModel(community = community, accounts = signedIn)
        arranged(viewModel)

        viewModel.showMyRanking()

        assertEquals(RankingNotice.SignInFirst, withTimeout(5_000) { viewModel.rankingNotices.first() })
        assertFalse((viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.sendingRanking == false } as CommunityListUiState.Success).sendingRanking)
    }

    @Test
    fun theLimitsOnPicturesFromOtherSites_areSaidAsThemselves() = runBlocking {
        listOf(
            RankingRefusal.WebPicturesToday to RankingNotice.WebPicturesToday,
            RankingRefusal.TooManyWebPictures to RankingNotice.TooManyWebPictures,
        ).forEach { (refusal, notice) ->
            val viewModel = viewModel(community = TestCommunity(showRefusal = refusal), accounts = signedIn)
            arranged(viewModel)

            viewModel.showMyRanking()

            assertEquals(notice, withTimeout(5_000) { viewModel.rankingNotices.first() })
        }
    }

    @Test
    fun anyOtherRefusal_saysItFailed_andShowsNothing() = runBlocking {
        val community = TestCommunity(showRefusal = RankingRefusal.Failed)
        val viewModel = viewModel(community = community, accounts = signedIn)
        arranged(viewModel)

        viewModel.showMyRanking()

        assertEquals(UserMessage.ActionFailed, withTimeout(5_000) { viewModel.userMessages.first() })
        assertTrue((viewModel.state.value as CommunityListUiState.Success).rankings.isEmpty())
    }
}

private class FailingCommunityRepository : FakeCommunityRepository() {
    override suspend fun makeFace(pictureId: String): Result<String> = Result.failure(IllegalStateException())
}
