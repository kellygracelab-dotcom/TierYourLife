package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Danylo saw his list's page blink before his board replaced it: a card of his own list goes to the board without the page. */
class ListOpenerTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val myBoard = TierList(id = 12, title = "Every A24 film", tiers = emptyList(), publishedId = "abc")
    private val opener = ListOpener(RecordingTierRepository(boards = listOf(myBoard)))

    @After
    fun clear() {
        opener.viewModelScope.cancel()
    }

    private sealed interface Went {
        data class Board(val id: Long) : Went
        data class Page(val route: CommunityRoute.CommunityList) : Went
    }

    private fun tap(route: CommunityRoute.CommunityList): Went = runBlocking {
        val went = CompletableDeferred<Went>()
        val pages = mutableListOf<CommunityRoute.CommunityList>()
        opener.open(
            route,
            toBoard = { went.complete(Went.Board(it)) },
            toList = {
                pages += it
                went.complete(Went.Page(it))
            },
        )
        withTimeout(5_000) { went.await() }.also { result ->
            if (result is Went.Board) assertEquals("The list's page was pushed as well as the board", emptyList<CommunityRoute.CommunityList>(), pages)
        }
    }

    @Test
    fun myOwnList_withItsBoardHere_goesToTheBoard_andNeverPushesThePage() {
        assertEquals(Went.Board(12), tap(CommunityRoute.CommunityList("abc", title = "Every A24 film", mine = true)))
    }

    @Test
    fun myOwnList_withNoBoardHere_opensThePage() {
        val route = CommunityRoute.CommunityList("gone", mine = true)

        assertEquals(Went.Page(route), tap(route))
    }

    @Test
    fun somebodyElsesList_opensThePage_evenWithABoardPublishedUnderItsId() {
        val route = CommunityRoute.CommunityList("abc", mine = false)

        assertEquals(Went.Page(route), tap(route))
    }

    @Test
    fun thePageAskedForFromTheBoard_opensThePage() {
        val route = CommunityRoute.CommunityList("abc", mine = true, fromBoard = true)

        assertEquals(Went.Page(route), tap(route))
    }
}
