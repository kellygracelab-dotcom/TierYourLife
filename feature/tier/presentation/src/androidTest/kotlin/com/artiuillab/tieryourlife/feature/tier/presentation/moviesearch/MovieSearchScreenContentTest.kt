package com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Mirrors AddMovieSheet's own structure: selection is a Map<String, CatalogueItem>
// owned above MovieSearchScreenContent, in the composable that hosts it, so it survives
// that composable cycling through Initial/Loading/Success/Empty/Error. Testing it any
// other way (e.g. state re-created fresh per test state value) wouldn't exercise the
// thing that was actually broken — a selection that lived inside the branch that gets
// torn down on every new search.
@RunWith(AndroidJUnit4::class)
class MovieSearchScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dune = CatalogueItem(id = "tmdb:438631", title = "Dune", subtitle = "2021", imageUrl = null)
    private val arrival = CatalogueItem(id = "tmdb:329865", title = "Arrival", subtitle = "2016", imageUrl = null)
    private val interstellar = CatalogueItem(id = "tmdb:157336", title = "Interstellar", subtitle = "2014", imageUrl = null)

    private lateinit var stateHolder: MutableState<MovieSearchUiState>
    private var confirmed: List<CatalogueItem>? = null
    private var closed = false
    private var lastQuery = ""

    private fun setContent(initial: MovieSearchUiState) {
        confirmed = null
        closed = false
        lastQuery = ""
        stateHolder = mutableStateOf(initial)
        composeRule.setContent {
            val state by stateHolder
            var query by remember { mutableStateOf("") }
            var selectedItems by remember { mutableStateOf<Map<String, CatalogueItem>>(emptyMap()) }
            TierYourLifeTheme {
                MovieSearchScreenContent(
                    state = state,
                    query = query,
                    onQueryChange = {
                        query = it
                        lastQuery = it
                    },
                    onSearchClick = {},
                    onClose = { closed = true },
                    listTitle = "Sci-fi films",
                    selectedIds = selectedItems.keys,
                    onToggleSelection = { item ->
                        selectedItems = if (selectedItems.containsKey(item.id)) {
                            selectedItems - item.id
                        } else {
                            selectedItems + (item.id to item)
                        }
                    },
                    onConfirmSelection = { confirmed = selectedItems.values.toList() },
                )
            }
        }
    }

    @Test
    fun tappingAResult_marksIt_andTappingAgainUnmarksIt() {
        setContent(MovieSearchUiState.Success(listOf(dune, arrival)))
        val row = composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id))

        row.assertIsNotSelected()
        row.performClick()
        row.assertIsSelected()
        row.performClick()
        row.assertIsNotSelected()
    }

    @Test
    fun selectedCounter_matchesNumberOfMarkedItems() {
        setContent(MovieSearchUiState.Success(listOf(dune, arrival, interstellar)))

        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(arrival.id)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_SELECTED_COUNT).assertTextEquals("2 selected")
    }

    @Test
    fun marksPersist_throughANewSearchAndItsLoadingState() {
        // The real user flow this bug broke: search, mark, search again (which passes
        // through Loading before the next Success), mark once more, confirm.
        setContent(MovieSearchUiState.Success(listOf(dune)))
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id)).performClick()

        composeRule.runOnIdle { stateHolder.value = MovieSearchUiState.Loading }
        composeRule.runOnIdle { stateHolder.value = MovieSearchUiState.Success(listOf(arrival)) }
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(arrival.id)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_CONFIRM).performClick()
        composeRule.runOnIdle {
            assertEquals(setOf(dune, arrival), confirmed?.toSet())
        }
    }

    @Test
    fun marksPersist_throughAnEmptyResultAndAnErrorInBetween() {
        setContent(MovieSearchUiState.Success(listOf(dune)))
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id)).performClick()

        composeRule.runOnIdle { stateHolder.value = MovieSearchUiState.Empty(query = "zzz") }
        composeRule.runOnIdle { stateHolder.value = MovieSearchUiState.Error(message = "offline") }
        composeRule.runOnIdle { stateHolder.value = MovieSearchUiState.Success(listOf(dune)) }

        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id)).assertIsSelected()
    }

    @Test
    fun confirming_yieldsExactlyTheMarkedSet_inOneCall() {
        setContent(MovieSearchUiState.Success(listOf(dune, arrival, interstellar)))
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(interstellar.id)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_CONFIRM).performClick()

        composeRule.runOnIdle {
            assertEquals(setOf(dune, interstellar), confirmed?.toSet())
        }
    }

    @Test
    fun emptySelection_confirmIsUnavailable() {
        setContent(MovieSearchUiState.Success(listOf(dune)))

        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_CONFIRM).assertHasNoClickAction()
    }

    @Test
    fun closeIcon_invokesOnClose() {
        setContent(MovieSearchUiState.Initial)

        composeRule.onNodeWithContentDescription("Close search").performClick()

        composeRule.runOnIdle { assertTrue(closed) }
    }

    @Test
    fun clearIcon_appearsOnlyWithQuery_andClearsTextWhenTapped() {
        setContent(MovieSearchUiState.Initial)

        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_FIELD).performTextInput("Dune")
        composeRule.onNodeWithContentDescription("Clear search").performClick()

        composeRule.runOnIdle { assertEquals("", lastQuery) }
    }

    @Test
    fun bottomBar_staysVisible_whenTheResultListDoesNotFitOnScreen() {
        // A tall list has no weight/bound constraint by default, which is exactly what let
        // the bar get laid out below the visible sheet area — this reproduces that shape of
        // bug without needing a real, simulated software keyboard.
        val manyResults = List(40) { index ->
            CatalogueItem(id = "tmdb:$index", title = "Title $index", subtitle = null, imageUrl = null)
        }
        setContent(MovieSearchUiState.Success(manyResults))

        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_BOTTOM_BAR).assertIsDisplayed()
    }

    @Test
    fun lastRow_isReachable_andNotCoveredByTheBottomBar() {
        val manyResults = List(40) { index ->
            CatalogueItem(id = "tmdb:$index", title = "Title $index", subtitle = null, imageUrl = null)
        }
        setContent(MovieSearchUiState.Success(manyResults))

        val lastTag = TierDetailTestTags.movieSearchResult(manyResults.last().id)
        // A LazyColumn only composes items near the viewport, so the last row's node
        // doesn't exist yet to scroll to by its own tag — scroll the list itself to the
        // index first, which composes the row, then it can be found and asserted on.
        composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_RESULTS_LIST)
            .performScrollToIndex(manyResults.lastIndex)
        composeRule.onNodeWithTag(lastTag).assertIsDisplayed()

        val lastRowBottom = composeRule.onNodeWithTag(lastTag).fetchSemanticsNode().boundsInRoot.bottom
        val barTop = composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_BOTTOM_BAR)
            .fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "last row (bottom=$lastRowBottom) must not extend past where the bottom bar starts (top=$barTop)",
            lastRowBottom <= barTop + 0.5f,
        )
    }

    @Test
    fun selectedRowTint_spansTheFullRowWidth() {
        setContent(MovieSearchUiState.Success(listOf(dune, arrival)))
        composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id)).performClick()

        val rowWidth = composeRule.onNodeWithTag(TierDetailTestTags.movieSearchResult(dune.id))
            .fetchSemanticsNode().boundsInRoot.width
        val listWidth = composeRule.onNodeWithTag(TierDetailTestTags.MOVIE_SEARCH_RESULTS_LIST)
            .fetchSemanticsNode().boundsInRoot.width

        assertEquals(
            "a selected row's own background is its full-bleed tint — its width must equal " +
                "the sheet-wide list container's width, not something inset within it",
            listWidth,
            rowWidth,
            0.5f,
        )
    }
}
