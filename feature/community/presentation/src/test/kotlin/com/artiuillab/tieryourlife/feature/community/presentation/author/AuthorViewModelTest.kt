package com.artiuillab.tieryourlife.feature.community.presentation.author

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.moderation.OwnerMemory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

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

    private fun authorFor(
        owner: OwnerMemory,
        uid: String = "u1",
        community: FakeCommunityRepository = FakeCommunityRepository(),
    ) = AuthorViewModel(
        community = community,
        preferences = FakeAppPreferences(),
        owner = owner,
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

private const val WAIT_MILLIS = 5_000L
