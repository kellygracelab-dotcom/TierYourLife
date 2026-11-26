package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.repository.Carried
import com.artiuillab.tieryourlife.feature.community.domain.repository.CarryRequest
import com.artiuillab.tieryourlife.feature.community.domain.repository.Carrying
import com.artiuillab.tieryourlife.feature.community.domain.repository.PublishCarrier
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Designer, approved by Danylo 26.11.2026: Update goes on in the background; the board stays open, and says how it went. */
class BackgroundUpdateTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun update_isHandedOn_andTheSlotSaysUpdatingUntilItIsOut() = runBlocking<Unit> {
        val h = harness()

        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }
        assertEquals("Films", h.carrier.sent.single().board.title)

        val said = async { h.viewModel.userMessages.first() }
        h.carrier.end(Carried.Out(BOARD, h.carrier.sent.single().board, Published("p-1", "sent")))

        assertEquals(UserMessage.Published(updated = true), said.await())
        h.viewModel.screen.firstWithin { !it.publish.publishing }
    }

    @Test
    fun withNoConnection_theSlotSaysItWaitsForOne() = runBlocking<Unit> {
        val h = harness()
        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }

        h.carrier.going.update { it + (BOARD to Carrying(update = true, offline = true)) }

        h.viewModel.screen.firstWithin { it.publish.waitingForConnection && it.publish.publishing }
    }

    @Test
    fun overAMinute_saysOnceThatOneCanKeepGoing() = runBlocking<Unit> {
        val h = harness()
        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }
        val said = async { h.viewModel.userMessages.first() }

        h.carrier.going.update { it + (BOARD to Carrying(update = true, slow = true)) }

        assertEquals(UserMessage.StillUpdating, said.await())
    }

    @Test
    fun anUpdateTurnedDown_saysTheChangesAreSafe_andOffersToTryAgain() = runBlocking<Unit> {
        val h = harness()
        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }
        val said = async { h.viewModel.userMessages.first() }

        h.carrier.end(Carried.Refused(BOARD, update = true, reason = PublishError.Unknown))

        assertEquals(UserMessage.CouldNotUpdate, said.await())
        h.viewModel.screen.firstWithin { !it.publish.publishing }
        assertEquals(null, h.viewModel.screen.value.publish.error)
    }

    @Test
    fun aRefusalWithAReason_isSaidAsBefore() = runBlocking<Unit> {
        val h = harness()
        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }

        h.carrier.end(Carried.Refused(BOARD, update = true, reason = PublishError.TooLarge))

        h.viewModel.screen.firstWithin { it.publish.error == PublishError.TooLarge && !it.publish.publishing }
    }

    @Test
    fun takeItDown_duringAnUpdate_stopsItFirst() = runBlocking<Unit> {
        val h = harness()
        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }

        h.viewModel.setPublic(false)
        h.viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.publishedId == null }

        assertEquals(listOf(BOARD), h.carrier.stopped)
    }

    @Test
    fun aSecondTap_whileOneIsOnItsWay_sendsNothingMore() = runBlocking<Unit> {
        val h = harness()
        h.viewModel.updatePublished()
        h.viewModel.screen.firstWithin { it.publish.publishing }

        h.viewModel.updatePublished()

        assertEquals(1, h.carrier.sent.size)
        assertFalse(h.viewModel.screen.value.publish.error != null)
        assertTrue(h.viewModel.screen.value.publish.publishing)
    }

    private class Harness(val viewModel: TierDetailViewModel, val carrier: Carrier)

    private suspend fun harness(): Harness {
        val carrier = Carrier()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(films(), publishedId = "p-1"),
            FakeCommunityRepositoryForDetail(),
            FakeAccountRepository(FakeAccountRepository.signedIn),
            SavedStateHandle(mapOf("tierListId" to BOARD, "startInTitleEdit" to false)),
            publishCarrier = carrier,
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        return Harness(viewModel, carrier)
    }

    /** Holds each trip until the test ends it. */
    private class Carrier : PublishCarrier {
        override val going = MutableStateFlow(emptyMap<Long, Carrying>())
        override val ended = MutableSharedFlow<Carried>(extraBufferCapacity = 8)
        val sent = mutableListOf<CarryRequest>()
        val stopped = mutableListOf<Long>()

        override fun send(request: CarryRequest) {
            if (request.board.id in going.value) return
            sent += request
            going.update { it + (request.board.id to Carrying(update = request.board.publishedId != null)) }
        }

        override suspend fun stop(boardId: Long) {
            stopped += boardId
            going.update { it - boardId }
        }

        suspend fun end(event: Carried) {
            ended.emit(event)
            going.update { it - event.boardId }
        }
    }

    private companion object {
        const val BOARD = 1L

        fun films() = TierList(
            id = BOARD,
            title = "Films",
            category = "films",
            tiers = listOf(
                Tier(id = 10, label = "S", colorLight = "#000000", colorDark = "#000000", items = listOf(TierItem(id = 100, title = "Alien", imageUrl = null))),
                Tier(id = 20, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
            ),
        )
    }
}
