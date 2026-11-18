package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PictureLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1), hasMore = false),
            byScope = mapOf(PictureScope.Games to page(ids = listOf(2), hasMore = false)),
        )
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) {
            viewModel.start("anime")
            viewModel.search("naruto")
        }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }
        withContext(Dispatchers.Main) { viewModel.setScope(PictureScope.Games) }
        // Another film, not a second count: an answer equal to the first would
        // not be emitted again, and a collector that missed Loading would wait forever.
        viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && it.items.map { item -> item.id } == listOf("tmdb:2")
        }

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

    @Test
    fun theQuickCatalogues_showAtOnce_andTheSlowOnesAddTheirOwnBelow() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(1, 2, 3), hasMore = true).map { it.copy(notice = SearchNotice.WebDailyLimit) },
            quick = page(ids = listOf(2, 1), hasMore = false).map { it.copy(rest = true) },
        )
        val viewModel = viewModelWith(repository)
        repository.holdWhole = true

        withContext(Dispatchers.Main) { viewModel.search("dune") }
        val quick = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success
        assertEquals(listOf("tmdb:2", "tmdb:1"), quick.items.map { it.id })
        assertTrue(quick.loadingMore)

        repository.holdWhole = false
        val whole = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && !it.loadingMore
        } as CatalogueSearchUiState.Success
        assertEquals(listOf("tmdb:2", "tmdb:1", "tmdb:3"), whole.items.map { it.id })
        assertTrue(whole.canLoadMore)
        assertEquals(SearchNotice.WebDailyLimit, whole.notice)
        assertEquals(listOf(true, false), repository.quickAsked)
    }

    @Test
    fun aQuickAnswerWithNothing_waitsForTheWholeOne() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to page(ids = listOf(5), hasMore = false),
            quick = page(ids = emptyList(), hasMore = false).map { it.copy(rest = true) },
        )
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("dune messiah") }
        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success

        assertEquals(listOf("tmdb:5"), shown.items.map { it.id })
        assertFalse(shown.loadingMore)
    }

    @Test
    fun aSlowSearchThatFails_leavesTheQuickResults() = runBlocking {
        val repository = FakeCatalogueSearchRepository(
            1 to Result.failure(IOException("timed out")),
            quick = page(ids = listOf(1), hasMore = false).map { it.copy(rest = true) },
        )
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("dune") }
        val shown = viewModel.state.firstWithin {
            it is CatalogueSearchUiState.Success && !it.loadingMore
        } as CatalogueSearchUiState.Success

        assertEquals(listOf("tmdb:1"), shown.items.map { it.id })
        assertEquals(listOf(true, false), repository.quickAsked)
    }

    @Test
    fun anAddressInTheField_isNotSearchedFor_butLookedAtAsAPicture() = runBlocking {
        val repository = FakeCatalogueSearchRepository(1 to page(ids = listOf(1), hasMore = false))
        val found = LinkPicture.Found("/files/p_1", PictureAttribution("web"), null)
        val links = FakePictureLinks(found)
        val viewModel = viewModelWith(repository, links)

        withContext(Dispatchers.Main) { viewModel.search(" https://i.example.net/p/8812.jpg ") }
        val shown = viewModel.link.firstWithin { it?.answer != null }

        assertEquals(LinkUiState("https://i.example.net/p/8812.jpg", found), shown)
        assertEquals("i.example.net", shown?.host)
        assertEquals(listOf("https://i.example.net/p/8812.jpg"), links.asked)
        assertTrue(repository.requestedPages.isEmpty())
        assertEquals(CatalogueSearchUiState.Initial, viewModel.state.value)
    }

    @Test
    fun wordsAfterAnAddress_searchAgain_andTheAddressIsLetGo() = runBlocking {
        val repository = FakeCatalogueSearchRepository(1 to page(ids = listOf(1), hasMore = false))
        val viewModel = viewModelWith(repository, FakePictureLinks(LinkPicture.Refused(LinkRefusal.NotAPicture)))

        withContext(Dispatchers.Main) { viewModel.search("https://example.com/films/totoro") }
        viewModel.link.firstWithin { it?.answer != null }
        withContext(Dispatchers.Main) { viewModel.search("totoro") }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }

        assertEquals(null, viewModel.link.value)
        assertEquals(listOf(1), repository.requestedPages)
    }

    @Test
    fun afterSigningIn_theSameAddressIsAskedAgain() = runBlocking {
        val links = FakePictureLinks(LinkPicture.SignInNeeded)
        val viewModel = viewModelWith(FakeCatalogueSearchRepository(), links)
        withContext(Dispatchers.Main) { viewModel.search("https://x.org/a.jpg") }
        viewModel.link.firstWithin { it?.answer == LinkPicture.SignInNeeded }

        links.answer = LinkPicture.Found("/files/p_2", null, null)
        withContext(Dispatchers.Main) { viewModel.retryLink() }

        assertEquals("/files/p_2", (viewModel.link.firstWithin { it?.answer is LinkPicture.Found }?.answer as LinkPicture.Found).imageUrl)
        assertEquals(2, links.asked.size)
    }

    @Test
    fun nothingFoundWhileACatalogueIsBusy_waitsUntilItSaid_andAsksAgainByItself() = runBlocking {
        val soon = System.currentTimeMillis() + 300
        val repository = ScriptedSearch(listOf(Result.failure(SearchRefusal.Busy(soon)), page(ids = listOf(4), hasMore = false)))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("naruto") }
        val waiting = viewModel.state.firstWithin { it is CatalogueSearchUiState.Waiting } as CatalogueSearchUiState.Waiting
        assertEquals(soon, waiting.untilMillis)

        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success
        assertEquals(listOf("tmdb:4"), shown.items.map { it.id })
        assertEquals(listOf(true, true), repository.quickAsked)
    }

    @Test
    fun aBusyCatalogueWithATimeTooFarOff_isNotWaitedFor_andCanBeTriedAgain() = runBlocking {
        val later = System.currentTimeMillis() + BUSY_WAIT_MAX_MILLIS + 60_000
        val repository = ScriptedSearch(listOf(Result.failure(SearchRefusal.Busy(later))))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("naruto") }

        assertEquals(CatalogueSearchUiState.Busy(later), viewModel.state.firstWithin { it is CatalogueSearchUiState.Busy })
        assertEquals(1, repository.quickAsked.size)
    }

    @Test
    fun overTheSearchLimit_theSearchStops_andNothingElseIsAsked() = runBlocking {
        val repository = ScriptedSearch(listOf(Result.failure(SearchRefusal.Limit(3_600_000))))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("dune") }

        assertEquals(CatalogueSearchUiState.Limited(3_600_000), viewModel.state.firstWithin { it is CatalogueSearchUiState.Limited })
        delay(100)
        assertEquals(listOf(true), repository.quickAsked)
    }

    @Test
    fun aGapLeftByABusyCatalogue_isFilledOnceWhenItMayBeAsked_belowWhatIsThere() = runBlocking {
        val soon = System.currentTimeMillis() + 300
        val gap = page(ids = listOf(1), hasMore = false).map { it.copy(busy = true, retryAt = soon) }
        val repository = ScriptedSearch(listOf(gap, page(ids = listOf(2, 1), hasMore = false).map { it.copy(busy = true, retryAt = soon) }))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("naruto") }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success && it.items.size == 1 }

        val filled = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success && it.items.size == 2 } as CatalogueSearchUiState.Success
        assertEquals(listOf("tmdb:1", "tmdb:2"), filled.items.map { it.id })
        delay(600)
        assertEquals(listOf(true, false), repository.quickAsked)
    }

    @Test
    fun aWholeSearchThatFindsNothingWhileBusy_waitsToo() = runBlocking {
        val soon = System.currentTimeMillis() + 200
        val repository = ScriptedSearch(
            listOf(
            page(ids = emptyList(), hasMore = false).map { it.copy(rest = true) },
            Result.failure(SearchRefusal.Busy(soon)),
            page(ids = listOf(9), hasMore = false),
            ),
        )
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("dune") }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Waiting }

        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success
        assertEquals(listOf("tmdb:9"), shown.items.map { it.id })
        assertEquals(listOf(true, false, true), repository.quickAsked)
    }

    @Test
    fun theWaitRule_isTheSites() {
        assertEquals(1_000L + BUSY_FALLBACK_MILLIS, busyWaitUntil(null, tries = 1, now = 1_000))
        assertEquals(5_000L, busyWaitUntil(5_000, tries = 4, now = 1_000))
        assertEquals(null, busyWaitUntil(5_000, tries = BUSY_TRIES, now = 1_000))
        assertEquals(null, busyWaitUntil(1_000 + BUSY_WAIT_MAX_MILLIS + 1, tries = 1, now = 1_000))
    }

    @Test
    fun everySearch_carriesTheListsSectionAndTopic_andAChangedScopeKeepsThem() = runBlocking {
        val repository = FakeCatalogueSearchRepository(1 to page(ids = listOf(1), hasMore = true), 2 to page(ids = listOf(2), hasMore = false))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) {
            viewModel.start("food", "t-dishes")
            viewModel.search("sushi")
        }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }
        withContext(Dispatchers.Main) { viewModel.loadMore() }
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success && it.items.size == 2 }

        assertTrue(repository.kinds.isNotEmpty())
        assertTrue(repository.kinds.all { it == ListKind("food", "t-dishes") })
    }

    @Test
    fun resultsFoundUnderOtherWords_sayWhich_andKeepSayingItAsMoreArrive() = runBlocking {
        val first = page(ids = listOf(1), hasMore = true).map { it.copy(searchedAs = "Interstellar") }
        val repository = FakeCatalogueSearchRepository(1 to first, 2 to page(ids = listOf(2), hasMore = false))
        val viewModel = viewModelWith(repository)

        withContext(Dispatchers.Main) { viewModel.search("Intersteller") }
        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success
        assertEquals("Interstellar", shown.searchedAs)

        withContext(Dispatchers.Main) { viewModel.loadMore() }
        val more = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success && it.items.size == 2 } as CatalogueSearchUiState.Success
        assertEquals("Interstellar", more.searchedAs)
    }

    private fun viewModelWith(repository: CatalogueSearchRepository, links: PictureLinks = FakePictureLinks(LinkPicture.SignInNeeded)) =
        CatalogueSearchViewModel(repository, FakeAppPreferences(), links)

    private fun page(ids: List<Int>, hasMore: Boolean): Result<CatalogueSearchPage> = Result.success(
        CatalogueSearchPage(
            items = ids.map { id ->
                CatalogueItem(id = "tmdb:$id", title = "Film $id", subtitle = null, imageUrl = "https://p/$id.jpg")
            },
            hasMore = hasMore,
        ),
    )
}

/**
 * Without [quick], a quick search is answered like the whole one, with nothing left to add.
 * A scope in [byScope] gets its own answer whatever the page.
 */
private class FakeCatalogueSearchRepository(
    vararg pages: Pair<Int, Result<CatalogueSearchPage>>,
    private val quick: Result<CatalogueSearchPage>? = null,
    private val byScope: Map<PictureScope, Result<CatalogueSearchPage>> = emptyMap(),
) : CatalogueSearchRepository {

    private val pagesByNumber = pages.toMap()
    val requestedPages = mutableListOf<Int>()
    val asked = mutableListOf<Pair<PictureScope, Boolean>>()
    val quickAsked = mutableListOf<Boolean>()
    val kinds = mutableListOf<ListKind>()

    @Volatile
    var holdWhole = false

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope,
        everywhere: Boolean,
        quick: Boolean,
        kind: ListKind,
    ): Result<CatalogueSearchPage> {
        quickAsked += quick
        kinds += kind
        if (quick && this.quick != null) return this.quick
        while (!quick && holdWhole) delay(10)
        if (quick || this.quick == null) {
            requestedPages += page
            asked += scope to everywhere
        }
        return byScope[scope]
            ?: pagesByNumber[page]
            ?: Result.failure(IllegalStateException("page $page was not expected"))
    }
}

/** Answers in turn, whatever is asked; the last one again when the turns run out. */
private class ScriptedSearch(answers: List<Result<CatalogueSearchPage>>) : CatalogueSearchRepository {
    private val turns = ArrayDeque(answers)
    val quickAsked: MutableList<Boolean> = java.util.Collections.synchronizedList(mutableListOf())

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope,
        everywhere: Boolean,
        quick: Boolean,
        kind: ListKind,
    ): Result<CatalogueSearchPage> {
        quickAsked += quick
        return if (turns.size > 1) turns.removeFirst() else turns.first()
    }
}

private class FakePictureLinks(@Volatile var answer: LinkPicture) : PictureLinks {
    val asked = mutableListOf<String>()

    override suspend fun fetch(url: String): LinkPicture {
        asked += url
        return answer
    }
}
