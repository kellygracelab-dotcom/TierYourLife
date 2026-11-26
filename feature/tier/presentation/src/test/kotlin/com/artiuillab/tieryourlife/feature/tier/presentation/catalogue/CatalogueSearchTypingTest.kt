package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PictureLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.Collections

/**
 * Typing into the search, as Backend's logs of 24.11 showed it: a request for
 * every pause ("Те", "Теория", "Теория большого"), and "Теория" twice at once.
 */
class CatalogueSearchTypingTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun keysTypedWithinTheWait_askOnlyForTheLastWords() = runBlocking {
        val search = HeldSearch()
        val viewModel = viewModelWith(search)

        viewModel.onQueryChange("Те")
        viewModel.onQueryChange("Тео")
        viewModel.onQueryChange("Теория")
        assertTrue("asked before the wait was over", search.asked.isEmpty())

        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success

        assertEquals(listOf("Теория"), search.asked.distinct())
        assertEquals("Теория", shown.items.single().title)
    }

    @Test
    fun newWords_cancelTheSearchOnItsWay_andOnlyTheirAnswerIsShown() = runBlocking {
        val search = HeldSearch(holding = setOf("Теор"))
        val viewModel = viewModelWith(search)
        viewModel.onQueryChange("Теор")
        search.started("Теор")

        viewModel.onQueryChange("Теория")
        val shown = viewModel.state.firstWithin { it is CatalogueSearchUiState.Success } as CatalogueSearchUiState.Success

        assertEquals(listOf("Теор"), search.cancelled.toList())
        assertEquals("Теория", shown.items.single().title)
    }

    @Test
    fun aSpaceAfterTheWords_doesNotAskForThemAgain() = runBlocking {
        val search = HeldSearch(holding = setOf("Теория"))
        val viewModel = viewModelWith(search)
        viewModel.onQueryChange("Теория")
        search.started("Теория")

        viewModel.onQueryChange("Теория ")
        search.release("Теория")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Success }
        viewModel.onQueryChange("Теория")
        delay(SETTLE_MILLIS)

        assertEquals(1, search.quickAsked.count { it == "Теория" })
        assertTrue(search.cancelled.isEmpty())
    }

    @Test
    fun theSameWordsAfterAFailure_areAskedAgain() = runBlocking {
        val search = HeldSearch(failing = setOf("Теория"))
        val viewModel = viewModelWith(search)
        viewModel.onQueryChange("Теория")
        viewModel.state.firstWithin { it is CatalogueSearchUiState.Error }

        viewModel.onQueryChange("Теория ")
        // Waited for rather than slept on: a busy CI machine takes longer than the debounce.
        withTimeout(5_000) { while (search.quickAsked.count { it == "Теория" } < 2) delay(20) }
        delay(SETTLE_MILLIS)

        assertEquals(2, search.quickAsked.count { it == "Теория" })
    }

    private fun viewModelWith(repository: CatalogueSearchRepository) =
        CatalogueSearchViewModel(repository, FakeAppPreferences(), NoLinks)
}

private const val SETTLE_MILLIS = 600L

/** Answers each search with one card named after its words; [holding] words wait to be released or cancelled. */
private class HeldSearch(
    private val holding: Set<String> = emptySet(),
    private val failing: Set<String> = emptySet(),
) : CatalogueSearchRepository {
    val asked: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val quickAsked: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val cancelled: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val gates = holding.associateWith { CompletableDeferred<Unit>() }
    private val begun = holding.associateWith { CompletableDeferred<Unit>() }

    suspend fun started(query: String) = withTimeout(5_000) { begun.getValue(query).await() }

    fun release(query: String) = gates.getValue(query).complete(Unit)

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope,
        everywhere: Boolean,
        quick: Boolean,
        kind: ListKind,
    ): Result<CatalogueSearchPage> {
        asked += query
        if (quick) quickAsked += query
        begun[query]?.complete(Unit)
        try {
            gates[query]?.await()
        } catch (stopped: CancellationException) {
            cancelled += query
            throw stopped
        }
        if (query in failing) return Result.failure(IOException("offline"))
        val card = CatalogueItem(id = "tmdb:$query", title = query, subtitle = null, imageUrl = "https://p/$query.jpg")
        return Result.success(CatalogueSearchPage(items = listOf(card), hasMore = false))
    }
}

private object NoLinks : PictureLinks {
    override suspend fun fetch(url: String): LinkPicture = LinkPicture.Refused(LinkRefusal.NotAPicture)
}
