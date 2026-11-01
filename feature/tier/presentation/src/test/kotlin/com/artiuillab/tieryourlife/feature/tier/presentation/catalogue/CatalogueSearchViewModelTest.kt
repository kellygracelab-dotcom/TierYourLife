package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CatalogueSearchViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadingMore_putsTheNextPageUnderWhatIsAlreadyThere() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2), hasMore = true),
            2 to page(ids = listOf(3, 4), hasMore = false),
        )
        val viewModel = viewModelWith(repository)
        viewModel.search("marvel")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()
        val shown = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && !it.loadingMore
        } as CatalogueSearchUiState.Success

        assertEquals(listOf("tmdb:1", "tmdb:2", "tmdb:3", "tmdb:4"), shown.items.map { it.id })
        assertEquals(listOf(1, 2), repository.requestedPages)
        assertFalse(shown.canLoadMore)
    }

    @Test
    fun theLastPage_isNotFollowedByAnotherRequest() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2), hasMore = false),
        )
        val viewModel = viewModelWith(repository)
        viewModel.search("marvel")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()

        assertEquals(listOf(1), repository.requestedPages)
    }

    // Losing twenty films the reader may already have ticked because the
    // twenty-first did not arrive would be a worse answer than no more films.
    @Test
    fun aPageThatFails_leavesWhatIsAlreadyOnScreenAlone() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2), hasMore = true),
            2 to Result.failure(IOException("offline")),
        )
        val viewModel = viewModelWith(repository)
        viewModel.search("marvel")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()
        val shown = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && !it.loadingMore
        } as CatalogueSearchUiState.Success

        assertEquals(listOf("tmdb:1", "tmdb:2"), shown.items.map { it.id })
        assertTrue(shown.canLoadMore)
    }

    @Test
    fun aFilmAlreadyOnScreen_isNotListedTwiceWhenThePageRepeatsIt() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2), hasMore = true),
            2 to page(ids = listOf(2, 3), hasMore = false),
        )
        val viewModel = viewModelWith(repository)
        viewModel.search("marvel")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()
        val shown = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && !it.loadingMore
        } as CatalogueSearchUiState.Success

        assertEquals(listOf("tmdb:1", "tmdb:2", "tmdb:3"), shown.items.map { it.id })
    }

    @Test
    fun aNewSearch_startsFromTheFirstPageAgain() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2), hasMore = true),
            2 to page(ids = listOf(3, 4), hasMore = true),
        )
        val viewModel = viewModelWith(repository)
        viewModel.search("marvel")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }
        viewModel.loadMore()
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success && !it.loadingMore }

        viewModel.search("dune")
        val shown = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && it.items.size == 2
        } as CatalogueSearchUiState.Success

        assertEquals(listOf(1, 2, 1), repository.requestedPages)
        assertEquals(listOf("tmdb:1", "tmdb:2"), shown.items.map { it.id })
    }

    @Test
    fun aBoardInASection_searchesItsCatalogues_untilAnotherScopeIsPicked() = runBlocking {
        val repository = FakeCatalogueSearchRepository(1 to page(ids = listOf(1), hasMore = false))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) {
            viewModel.start("anime")
            viewModel.search("naruto")
        }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }
        withContext(Dispatchers.Main) { viewModel.setScope(PictureScope.Games) }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success && repository.asked.size == 2 }

        assertEquals(listOf(PictureScope.Anime to false, PictureScope.Games to false), repository.asked)
        assertEquals(PictureScope.Games, viewModel.scope.value)
    }

    @Test
    fun aSectionWithNoCatalogueOfItsOwn_searchesEverything() = runBlocking {
        val viewModel = viewModelWith(FakeCatalogueSearchRepository())

        withContext(Dispatchers.Main) { viewModel.start("board_games") }

        assertEquals(PictureScope.All, viewModel.scope.value)
    }

    @Test
    fun anEmptyScope_offersEverywhere_andEverywhereEmptyOffersNothingMore() = runBlocking {
        val repository = FakeCatalogueSearchRepository(1 to page(ids = emptyList(), hasMore = false))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) {
            viewModel.start("games")
            viewModel.search("borscht")
        }
        val scoped = viewModel.state.firstWithin { it is CatalogueSearchUiState.Empty } as CatalogueSearchUiState.Empty
        assertFalse(scoped.searchedEverywhere)

        withContext(Dispatchers.Main) { viewModel.searchEverywhere() }
        val everywhere = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Empty && it.searchedEverywhere
        } as CatalogueSearchUiState.Empty

        assertTrue(everywhere.searchedEverywhere)
        assertEquals(PictureScope.All to true, repository.asked.last())
    }

    @Test
    fun aNoticeFromTheServer_isShownWithTheResults() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1), hasMore = false).map { it.copy(notice = SearchNotice.WebDailyLimit) },
        )
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("puss in boots") }
        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success

        assertEquals(SearchNotice.WebDailyLimit, shown.notice)
    }

    private fun viewModelWith(repository: CatalogueSearchRepository) =
        CatalogueSearchViewModel(repository, FakeAppPreferences())

    private fun page(ids: List<Int>, hasMore: Boolean): Result<CatalogueSearchPage> = Result.success(
        CatalogueSearchPage(
            items = ids.map { id ->
                CatalogueItem(id = "tmdb:$id", title = "Film $id", subtitle = null, imageUrl = "https://p/$id.jpg")
            },
            hasMore = hasMore,
        ),
    )
}

private class FakeCatalogueSearchRepository(
    vararg pages: Pair<Int, Result<CatalogueSearchPage>>,
) : CatalogueSearchRepository {

    private val pagesByNumber = pages.toMap()
    val requestedPages = mutableListOf<Int>()
    val asked = mutableListOf<Pair<PictureScope, Boolean>>()

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope,
        everywhere: Boolean,
    ): Result<CatalogueSearchPage> {
        requestedPages += page
        asked += scope to everywhere
        return pagesByNumber[page] ?: Result.failure(IllegalStateException("page $page was not expected"))
    }
}
