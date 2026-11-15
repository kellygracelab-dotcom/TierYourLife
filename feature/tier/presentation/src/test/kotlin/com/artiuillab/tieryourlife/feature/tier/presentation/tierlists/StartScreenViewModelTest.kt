package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.FakeTierRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Where the app opens: on the community's lists while someone has none of their own, on Your lists once they do. */
class StartScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val board = TierList(id = 1, title = "Films", tiers = emptyList())

    private fun lists(answer: suspend () -> List<TierListSummary>): TierRepository = object : TierRepository by FakeTierRepository(board) {
        override suspend fun getTierListSummaries(): List<TierListSummary> = answer()
    }

    private fun startsOnCommunity(repository: TierRepository): Boolean = runBlocking {
        StartScreenViewModel(repository).newcomer.firstWithin { it != null }!!
    }

    @Test
    fun withNoListsYet_itOpensOnTheCommunity() {
        assertEquals(true, startsOnCommunity(lists { emptyList() }))
    }

    @Test
    fun withAListOfTheirOwn_itOpensOnYourLists() {
        assertEquals(false, startsOnCommunity(FakeTierRepository(board)))
    }

    @Test
    fun whenTheListsCannotBeRead_itOpensOnYourListsAsBefore() {
        assertEquals(false, startsOnCommunity(lists { error("database closed") }))
    }
}
