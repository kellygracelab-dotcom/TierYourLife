package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The button answers before the server does, because a control that waits
 * for a round trip reads as broken; what matters is that it goes back when
 * the server refuses.
 */
@RunWith(AndroidJUnit4::class)
class FollowingAnAuthorTest {

    @Test
    fun theChipSaysNothing_untilTheServerHasAnswered() = runBlocking {
        val viewModel = viewModel(FollowRecorder(answers = false))

        val state = ready(viewModel)

        assertNull(state.follow)
    }

    @Test
    fun following_answersBeforeTheServerDoes() = runBlocking {
        val community = FollowRecorder()
        val viewModel = viewModel(community)
        ready(viewModel) { it.follow != null }

        viewModel.toggleFollow()
        val after = ready(viewModel) { it.follow?.following == true }

        assertTrue(after.follow!!.following)
        assertEquals("author-1", eventually("the follow to reach the server") { community.followed.firstOrNull() })
    }

    @Test
    fun aFollowTheServerRefuses_putsTheButtonBack() = runBlocking {
        val community = FollowRecorder(followFails = true)
        val viewModel = viewModel(community)
        ready(viewModel) { it.follow != null }

        viewModel.toggleFollow()
        val after = ready(viewModel) { it.follow?.following == false }

        assertFalse(after.follow!!.following)
    }

    @Test
    fun unfollowing_reachesTheServer() = runBlocking {
        val community = FollowRecorder(alreadyFollowing = true)
        val viewModel = viewModel(community)
        ready(viewModel) { it.follow != null }

        viewModel.toggleFollow()
        val after = ready(viewModel) { it.follow?.following == false }

        assertFalse(after.follow!!.following)
        assertEquals(
            "author-1",
            eventually("the unfollow to reach the server") { community.unfollowed.firstOrNull() },
        )
    }

    // The only thing the popular ordering counts.
    @Test
    fun takingAListToRankYourself_isCounted() = runBlocking {
        val community = FollowRecorder()
        val viewModel = viewModel(community)
        ready(viewModel)

        viewModel.saveToMyLists(Showing.Mine)

        assertEquals("abc", eventually("the take to be counted") { community.taken.firstOrNull() })
    }

    // Their copy exists either way, and a count that missed one take is not
    // worth an error on a screen.
    @Test
    fun aCountThatCannotBeSent_doesNotSpoilTheSave() = runBlocking {
        val community = FollowRecorder(takeFails = true)
        val tiers = RecordingTierRepository()
        val viewModel = viewModel(community, tiers)
        ready(viewModel)

        viewModel.saveToMyLists(Showing.Mine)

        val savedId = eventually("the board to be saved") { (viewModel.state.value as? CommunityListUiState.Success)?.savedId }
        assertTrue(savedId > 0)
    }

    /** The button answers before the server does on purpose, so a check on what reached the server has to wait for it. */
    private suspend fun <T : Any> eventually(what: String, get: () -> T?): T =
        withTimeoutOrNull(WAIT_MILLIS) {
            var seen = get()
            while (seen == null) {
                delay(POLL_MILLIS)
                seen = get()
            }
            seen
        } ?: error("Never saw $what")

    private suspend fun ready(
        viewModel: CommunityListViewModel,
        until: (CommunityListUiState.Success) -> Boolean = { true },
    ): CommunityListUiState.Success = withTimeoutOrNull(WAIT_MILLIS) {
        viewModel.state.first { it is CommunityListUiState.Success && until(it) }
            as CommunityListUiState.Success
    } ?: error("Waited for a state that never came. Last was ${viewModel.state.value}")

    private fun viewModel(
        community: CommunityRepository,
        tiers: RecordingTierRepository = RecordingTierRepository(),
    ) = CommunityListViewModel(
        community = community,
        tiers = tiers,
        preferences = FakeAppPreferences(),
        accounts = FakeAccountRepository(),
        savedStateHandle = SavedStateHandle(mapOf("publishedId" to "abc")),
        openedLists = FakeOpenedLists(),
    )
}

private const val WAIT_MILLIS = 5_000L
private const val POLL_MILLIS = 5L

private val theList = PublishedList(
    summary = PublishedListSummary(
        id = "abc",
        title = "Every A24 film",
        authorUid = "author-1",
        authorName = "Olena M.",
        category = ListCategory.FilmTv.id,
        itemCount = 1,
        updatedAtMillis = 0,
    ),
    tiers = listOf(Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList())),
    items = listOf(TierItem(0, "Hereditary", null)),
)

private class FollowRecorder(
    private val alreadyFollowing: Boolean = false,
    /** False for a server that never answers whether this author is followed. */
    private val answers: Boolean = true,
    private val followFails: Boolean = false,
    private val takeFails: Boolean = false,
) : FakeCommunityRepository() {
    val followed = mutableListOf<String>()
    val unfollowed = mutableListOf<String>()
    val taken = mutableListOf<String>()

    override suspend fun followState(authorUid: String): Result<FollowState> = if (answers) {
        Result.success(FollowState(following = alreadyFollowing))
    } else {
        Result.failure(IllegalStateException("offline"))
    }

    override suspend fun follow(authorUid: String): Result<Unit> = if (followFails) {
        Result.failure(IllegalStateException("offline"))
    } else {
        followed += authorUid
        Result.success(Unit)
    }

    override suspend fun unfollow(authorUid: String): Result<Unit> = if (followFails) {
        Result.failure(IllegalStateException("offline"))
    } else {
        unfollowed += authorUid
        Result.success(Unit)
    }

    override suspend fun noteTaken(publishedId: String): Result<Unit> = if (takeFails) {
        Result.failure(IllegalStateException("offline"))
    } else {
        taken += publishedId
        Result.success(Unit)
    }

    override suspend fun open(id: String): Result<PublishedList> = Result.success(theList)
}
