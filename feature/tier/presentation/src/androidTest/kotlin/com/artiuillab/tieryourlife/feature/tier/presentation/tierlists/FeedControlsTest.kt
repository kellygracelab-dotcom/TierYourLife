package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The feed's two axes: whose lists, and in what order. They are separate
 * controls because they are separate questions, and the whole point of that is
 * that changing one must not quietly change another.
 */
@RunWith(AndroidJUnit4::class)
class FeedControlsTest {

    @Test
    fun everyoneOpens_onWhatPeopleHaveTaken() = runBlocking {
        val community = RecordingCommunity()
        val viewModel = started(community)

        val shown = ready(viewModel)

        assertEquals(FeedSource.Everyone, shown.communitySource)
        assertEquals(FeedSort.Popular, shown.communitySort)
        assertEquals(FeedSort.Popular, eventually("a feed request") { community.asked.lastOrNull() }.sort)
    }

    // Somebody who followed these people has already vouched for them, and
    // popularity here would bury a good list by an author with twelve
    // followers for ever.
    @Test
    fun followingOpens_onTheNewest() = runBlocking {
        val community = RecordingCommunity()
        val viewModel = started(community)
        ready(viewModel)

        viewModel.selectCommunitySource(FeedSource.Following)
        val shown = ready(viewModel) { it.communitySource == FeedSource.Following }

        assertEquals(FeedSort.Recent, shown.communitySort)
        val sent = eventually("a request for the people followed") {
            community.asked.lastOrNull()?.takeIf { it.following }
        }
        assertEquals(FeedSort.Recent, sent.sort)
    }

    @Test
    fun eachSource_remembersItsOwnOrder() = runBlocking {
        val community = RecordingCommunity()
        val viewModel = started(community)
        ready(viewModel)

        viewModel.selectCommunitySort(FeedSort.Recent)
        ready(viewModel) { it.communitySort == FeedSort.Recent }
        viewModel.selectCommunitySource(FeedSource.Following)
        ready(viewModel) { it.communitySource == FeedSource.Following }
        viewModel.selectCommunitySource(FeedSource.Everyone)
        val back = ready(viewModel) { it.communitySource == FeedSource.Everyone }

        assertEquals(FeedSort.Recent, back.communitySort)
    }

    // The category answers a third question and keeps its own row. Switching
    // where the lists come from is no reason to widen what they are about.
    @Test
    fun switchingSource_keepsTheCategory() = runBlocking {
        val community = RecordingCommunity()
        val viewModel = started(community)
        ready(viewModel)

        viewModel.selectCommunityCategory(ListCategory.Games)
        ready(viewModel) { it.communityCategory == ListCategory.Games }
        viewModel.selectCommunitySource(FeedSource.Following)
        val shown = ready(viewModel) { it.communitySource == FeedSource.Following }

        assertEquals(ListCategory.Games, shown.communityCategory)
        val sent = eventually("a request narrowed to games") {
            community.asked.lastOrNull()?.takeIf { it.following }
        }
        assertEquals(ListCategory.Games, sent.category)
    }

    @Test
    fun followingNobody_offersPeopleRatherThanSayingThereIsNothing() = runBlocking {
        val community = RecordingCommunity(followsNobody = true, suggestions = listOf(author("a"), author("b")))
        val viewModel = started(community)
        ready(viewModel)

        viewModel.selectCommunitySource(FeedSource.Following)
        val offered = feed(viewModel) { it is CommunityFeed.FollowingNobody && !it.loading }

        assertEquals(listOf("a", "b"), (offered as CommunityFeed.FollowingNobody).authors.map { it.uid })
    }

    // A list that removes what you just touched makes the next tap land on
    // somebody else.
    @Test
    fun followingSomebodyOffered_leavesThemOnScreen() = runBlocking {
        val community = RecordingCommunity(followsNobody = true, suggestions = listOf(author("a")))
        val viewModel = started(community)
        ready(viewModel)
        viewModel.selectCommunitySource(FeedSource.Following)
        feed(viewModel) { it is CommunityFeed.FollowingNobody && !it.loading }

        viewModel.followSuggested("a")
        val after = feed(viewModel) {
            (it as? CommunityFeed.FollowingNobody)?.followed?.contains("a") == true
        } as CommunityFeed.FollowingNobody

        assertEquals(listOf("a"), after.authors.map { it.uid })
        assertEquals("a", eventually("the follow to reach the server") { community.followed.firstOrNull() })
    }

    @Test
    fun aFollowThatFails_putsTheButtonBack() = runBlocking {
        val community = RecordingCommunity(
            followsNobody = true,
            suggestions = listOf(author("a")),
            followFails = true,
        )
        val viewModel = started(community)
        ready(viewModel)
        viewModel.selectCommunitySource(FeedSource.Following)
        feed(viewModel) { it is CommunityFeed.FollowingNobody && !it.loading }

        viewModel.followSuggested("a")
        val after = feed(viewModel) {
            (it as? CommunityFeed.FollowingNobody)?.followed?.isEmpty() == true
        } as CommunityFeed.FollowingNobody

        assertFalse("a" in after.followed)
    }

    /**
     * Waits for something a coroutine will do, rather than assuming it already
     * has. The screen answers before the server does on purpose, so a check on
     * what reached the server has to wait for it -- on a machine where the
     * launch does not happen to run inline, it has not.
     */
    private suspend fun <T : Any> eventually(what: String, get: () -> T?): T =
        withTimeoutOrNull(WAIT_MILLIS) {
            var seen = get()
            while (seen == null) {
                delay(POLL_MILLIS)
                seen = get()
            }
            seen
        } ?: error("Never saw $what")

    /**
     * Bounded, so a state that never arrives fails the test with the state it
     * got stuck on instead of hanging the whole run.
     */
    private suspend fun ready(
        viewModel: TierListsViewModel,
        until: (TierListsUiState.Success) -> Boolean = { true },
    ): TierListsUiState.Success = withTimeoutOrNull(WAIT_MILLIS) {
        viewModel.state.first { it is TierListsUiState.Success && until(it) } as TierListsUiState.Success
    } ?: error("Waited for a state that never came. Last was ${viewModel.state.value}")

    private suspend fun feed(
        viewModel: TierListsViewModel,
        until: (CommunityFeed) -> Boolean,
    ): CommunityFeed = ready(viewModel) { until(it.community) }.community

    /** Nothing loads on construction; the screen asks, so the test does too. */
    private fun started(community: CommunityRepository) = viewModel(community).also {
        it.loadTierLists()
        it.loadCommunityFeed()
    }

    private fun viewModel(community: CommunityRepository) = TierListsViewModel(
        FakeTierRepository(emptyList()),
        community,
        FakeAppPreferences(),
        GuestAccount,
        NoSync,
        NoRestore,
    )

    private fun author(uid: String) =
        SuggestedAuthor(uid = uid, name = uid.uppercase(), photoUrl = null, takeCount = 3)
}

/** What the feed was actually asked for, which is the thing under test. */
internal data class Asked(
    val category: ListCategory?,
    val sort: FeedSort,
    val following: Boolean,
)

internal class RecordingCommunity(
    private val followsNobody: Boolean = false,
    private val suggestions: List<SuggestedAuthor> = emptyList(),
    private val followFails: Boolean = false,
) : CommunityRepository {
    val asked = mutableListOf<Asked>()
    val followed = mutableListOf<String>()

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> {
        asked += Asked(category, sort, following)
        return Result.success(
            CommunityPage(lists = emptyList(), followingNobody = following && followsNobody),
        )
    }

    override suspend fun follow(authorUid: String): Result<Unit> = if (followFails) {
        Result.failure(IllegalStateException("offline"))
    } else {
        followed += authorUid
        Result.success(Unit)
    }

    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(suggestions)
    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())
    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())
    override suspend fun publish(list: TierList): Result<Published> = Result.failure(IllegalStateException())
    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun makeFace(pictureId: String): Result<String> = Result.success("https://example.test/f.jpg")
    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)
    override suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit> =
        Result.success(Unit)

    override suspend fun reports(): Result<List<ModerationReport>> = Result.failure(IllegalStateException())
    override suspend fun takeDown(publishedId: String, ban: BanLength?): Result<Unit> = Result.success(Unit)
    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)
}

private object GuestAccount : AccountRepository {
    override val account: Flow<Account> = flowOf(Account.Guest)
    override suspend fun signInWithGoogle(idToken: String): SignInOutcome = SignInOutcome.Success
    override suspend fun setDisplayName(name: String): Boolean = true
    override suspend fun setPhotoUrl(photoUrl: String?): Boolean = true
    override suspend fun signOut() = Unit
}

private object NoSync : BoardSync {
    override suspend fun sync(): SyncReport = SyncReport(signedIn = false)
}

private object NoRestore : PictureRestore {
    override val restoring = MutableStateFlow(PictureRestore.Progress.Idle)
}

private const val WAIT_MILLIS = 5_000L
private const val POLL_MILLIS = 5L
