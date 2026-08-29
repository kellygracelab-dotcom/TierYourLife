package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

// Instrumented because viewModelScope needs Android's main dispatcher.
@RunWith(AndroidJUnit4::class)
class CatalogueSearchViewModelTest {

    @Test
    fun loadingMore_putsTheNextPageUnderWhatIsAlreadyThere() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2), hasMore = true),
            2 to page(ids = listOf(3, 4), hasMore = false),
        )
        val viewModel = viewModelWith(repository)
        viewModel.search("marvel")
        viewModel.state.first { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()
        val shown = viewModel.state.first {
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
        viewModel.state.first { it is CatalogueSearchUiState.Success }

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
        viewModel.state.first { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()
        val shown = viewModel.state.first {
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
        viewModel.state.first { it is CatalogueSearchUiState.Success }

        viewModel.loadMore()
        val shown = viewModel.state.first {
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
        viewModel.state.first { it is CatalogueSearchUiState.Success }
        viewModel.loadMore()
        viewModel.state.first { it is CatalogueSearchUiState.Success && !it.loadingMore }

        viewModel.search("dune")
        val shown = viewModel.state.first {
            it is CatalogueSearchUiState.Success && it.items.size == 2
        } as CatalogueSearchUiState.Success

        assertEquals(listOf(1, 2, 1), repository.requestedPages)
        assertEquals(listOf("tmdb:1", "tmdb:2"), shown.items.map { it.id })
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

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
    ): Result<CatalogueSearchPage> {
        requestedPages += page
        return pagesByNumber[page] ?: Result.failure(IllegalStateException("page $page was not expected"))
    }
}
