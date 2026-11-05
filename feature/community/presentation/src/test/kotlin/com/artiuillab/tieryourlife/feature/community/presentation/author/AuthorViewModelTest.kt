package com.artiuillab.tieryourlife.feature.community.presentation.author

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.TooManyFollowed
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowRefusal
import com.artiuillab.tieryourlife.feature.community.presentation.follow.signedInFollows
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.OwnerMemory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AuthorViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    // Nobody is known to be the owner until the moderators list has answered them.
    @Test
    fun beforeTheOwnerIsKnown_noModeratorActionIsOffered() = runBlocking {
        val viewModel = authorFor(OwnerMemory())

        assertNull(viewModel.moderatorAction.value)
    }

    @Test
    fun theOwner_isOfferedToMakeAStranger_andToRemoveAModerator() = runBlocking {
        val owner = OwnerMemory().apply { learned(setOf("mod-1")) }

        val stranger = authorFor(owner, uid = "u1")
        val moderator = authorFor(owner, uid = "mod-1")

        assertEquals(ModeratorAction.Make, stranger.moderatorAction.firstWithin { it != null })
        assertEquals(ModeratorAction.Remove, moderator.moderatorAction.firstWithin { it != null })
    }

    @Test
    fun appointing_tellsTheServer_andTheMemory_soTheOfferTurnsIntoRemove() = runBlocking {
        val owner = OwnerMemory().apply { learned(emptySet()) }
        val community = AppointingFake()
        val viewModel = authorFor(owner, community = community)
        viewModel.moderatorAction.firstWithin { it == ModeratorAction.Make }

        viewModel.toggleModerator()

        assertEquals("u1" to "Ann", withTimeout(WAIT_MILLIS) { community.appointed.await() })
        assertEquals(ModeratorAction.Remove, viewModel.moderatorAction.firstWithin { it == ModeratorAction.Remove })
        assertEquals(setOf("u1"), owner.moderators.value)
    }

    @Test
    fun aGuestsFollow_asksForASignIn_andTheSignInFollows() = runBlocking {
        val accounts = FakeAccountRepository()
        val community = FollowingFake()
        val viewModel = authorFor(OwnerMemory(), community = community, afterSignIn = FollowAfterSignIn(accounts))
        viewModel.state.firstWithin { (it as? AuthorUiState.Ready)?.follow != null }

        viewModel.toggleFollow()

        withTimeout(WAIT_MILLIS) { viewModel.signInRequests.first() }
        assertEquals(false, (viewModel.state.value as AuthorUiState.Ready).follow?.following)
        assertFalse(community.followed.isCompleted)

        accounts.accounts.value = FakeAccountRepository.signedIn

        assertEquals("u1", withTimeout(WAIT_MILLIS) { community.followed.await() })
        viewModel.state.firstWithin { (it as? AuthorUiState.Ready)?.follow?.following == true }
        Unit
    }

    @Test
    fun aFollowOverTheLimit_saysSo_andGoesBack() = runBlocking {
        val viewModel = authorFor(OwnerMemory(), community = FollowingFake(refusal = TooManyFollowed()))
        viewModel.state.firstWithin { (it as? AuthorUiState.Ready)?.follow != null }

        viewModel.toggleFollow()

        assertEquals(FollowRefusal.TooMany, withTimeout(WAIT_MILLIS) { viewModel.followRefusals.first() })
        viewModel.state.firstWithin { (it as? AuthorUiState.Ready)?.follow?.following == false }
        Unit
    }

    @Test
    fun aFailedUnfollow_saysItCouldNotUnfollow() = runBlocking {
        val viewModel = authorFor(OwnerMemory(), community = FollowingFake(following = true, refusal = IOException("offline")))
        viewModel.state.firstWithin { (it as? AuthorUiState.Ready)?.follow?.following == true }

        viewModel.toggleFollow()

        assertEquals(FollowRefusal.Unfollow, withTimeout(WAIT_MILLIS) { viewModel.followRefusals.first() })
        viewModel.state.firstWithin { (it as? AuthorUiState.Ready)?.follow?.following == true }
        Unit
    }

    private fun authorFor(
        owner: OwnerMemory,
        uid: String = "u1",
        community: FakeCommunityRepository = FakeCommunityRepository(),
        afterSignIn: FollowAfterSignIn = signedInFollows(),
    ) = AuthorViewModel(
        community = community,
        preferences = FakeAppPreferences(),
        owner = owner,
        afterSignIn = afterSignIn,
        savedStateHandle = SavedStateHandle(mapOf("authorUid" to uid, "authorName" to "Ann")),
    )
}

private class AppointingFake : FakeCommunityRepository() {
    val appointed = CompletableDeferred<Pair<String, String?>>()

    override suspend fun appointModerator(uid: String, name: String?, photoUrl: String?): Result<Unit> {
        appointed.complete(uid to name)
        return Result.success(Unit)
    }
}

private class FollowingFake(private val following: Boolean = false, private val refusal: Exception? = null) : FakeCommunityRepository() {
    val followed = CompletableDeferred<String>()

    override suspend fun followState(authorUid: String): Result<FollowState> = Result.success(FollowState(following))

    override suspend fun follow(authorUid: String): Result<Unit> {
        followed.complete(authorUid)
        return refusal?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun unfollow(authorUid: String): Result<Unit> = refusal?.let { Result.failure(it) } ?: Result.success(Unit)
}

private const val WAIT_MILLIS = 5_000L
