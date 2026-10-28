package com.artiuillab.tieryourlife.feature.community.presentation.allsections

import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.text.Collator
import java.util.Locale

class AllSectionsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val cars = Section(id = "cars", name = "Cars", listCount = 40)
    private val books = Section(id = "books", listCount = 90)
    private val anime = Section(id = "anime", listCount = 10)
    private val other = Section(id = "other", listCount = 500)
    private val boats = Section(id = "boats", name = "Bâteaux", names = mapOf("uk" to "Човни"), listCount = 40)

    private val labels = mapOf("cars" to "Cars", "books" to "Books", "anime" to "Anime", "other" to "Other", "boats" to "Човни")
    private val byName = Collator.getInstance(Locale.ENGLISH)

    private fun ordered(sections: List<Section>, sort: SectionSort, query: String = "") =
        allSectionsInOrder(sections, sort, query, { labels.getValue(it.id) }, byName).map { it.id }

    @Test
    fun mostLists_firstByCount_tiesKeepTheServersOrder_otherLast() {
        assertEquals(listOf("books", "cars", "boats", "anime", "other"), ordered(listOf(other, cars, anime, boats, books), SectionSort.MostLists))
    }

    @Test
    fun aToZ_byTheNameOnScreen_otherLast() {
        assertEquals(listOf("anime", "books", "cars", "boats", "other"), ordered(listOf(other, cars, anime, boats, books), SectionSort.Name))
    }

    @Test
    fun aSearch_findsByTheNameOnScreen_orByTheNameAsTyped() {
        assertEquals(listOf("cars"), ordered(listOf(cars, books, boats), SectionSort.MostLists, query = " car "))
        assertEquals(listOf("boats"), ordered(listOf(cars, books, boats), SectionSort.MostLists, query = "човн"))
        assertEquals(listOf("boats"), ordered(listOf(cars, books, boats), SectionSort.MostLists, query = "bât"))
        assertTrue(ordered(listOf(cars, books), SectionSort.Name, query = "zzz").isEmpty())
    }

    @Test
    fun theSearchField_comesPastTwenty() {
        val twenty = (1..20).map { Section(id = "s$it") }

        assertFalse(AllSectionsUiState(sections = twenty).searchable)
        assertTrue(AllSectionsUiState(sections = twenty + cars).searchable)
    }

    private class Community(val followFails: Boolean = false) : FakeCommunityRepository() {
        var reads = 0

        override suspend fun sections(): Result<List<Section>> {
            reads++
            return Result.success(listOf(Section(id = "cars", name = "Cars"), Section(id = "kites", hidden = true)) + Section.BUILT_IN)
        }

        override suspend fun followSection(id: String): Result<Unit> =
            if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)
    }

    private val made = mutableListOf<AllSectionsViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(community: Community) = AllSectionsViewModel(sectionCatalogFor(community)).also { made += it }

    @Test
    fun opening_readsTheSections_andShowsOnlyThoseOrdinaryScreensDraw() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.open()
        val state = viewModel.state.firstWithin { it.countsKnown }

        assertEquals(1, community.reads)
        assertEquals(listOf("cars") + Section.BUILT_IN.map { it.id }, state.sections.map { it.id })
    }

    @Test
    fun theOrderAndTheSearch_areKeptInTheState() = runBlocking {
        val viewModel = viewModel(Community())

        viewModel.selectSort(SectionSort.Name)
        viewModel.updateQuery("car")

        val state = viewModel.state.firstWithin { it.query == "car" }
        assertEquals(SectionSort.Name, state.sort)
    }

    @Test
    fun followingFromALongPress_saysSo() = runBlocking {
        val viewModel = viewModel(Community())
        viewModel.open()
        viewModel.state.firstWithin { it.countsKnown }

        viewModel.follow("cars")

        assertEquals(CommunityNotice.NowFollowing("cars"), withTimeout(5_000) { viewModel.notices.first() })
        val followed = viewModel.state.firstWithin { s -> s.sections.first { it.id == "cars" }.following }
        assertEquals(1, followed.sections.first { it.id == "cars" }.followerCount)
    }

    @Test
    fun aRefusedFollow_saysSo_andPutsTheTileBack() = runBlocking {
        val viewModel = viewModel(Community(followFails = true))
        viewModel.open()
        viewModel.state.firstWithin { it.countsKnown }

        viewModel.follow("cars")

        assertEquals(CommunityNotice.FollowFailed, withTimeout(5_000) { viewModel.notices.first() })
        val back = viewModel.state.firstWithin { s -> !s.sections.first { it.id == "cars" }.following }
        assertEquals(0, back.sections.first { it.id == "cars" }.followerCount)
    }
}
