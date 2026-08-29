package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogueSearchScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dune = CatalogueItem(id = "tmdb:438631", title = "Dune", subtitle = "2021", imageUrl = null)
    private val arrival = CatalogueItem(id = "tmdb:329865", title = "Arrival", subtitle = "2016", imageUrl = null)
    private val interstellar = CatalogueItem(id = "tmdb:157336", title = "Interstellar", subtitle = "2014", imageUrl = null)

    private lateinit var stateHolder: MutableState<CatalogueSearchUiState>
    private var confirmed: List<CatalogueItem>? = null
    private var closed = false
    private var lastQuery = ""
    private var loadMoreCalls = 0

    private fun setContent(initial: CatalogueSearchUiState) {
        confirmed = null
        closed = false
        lastQuery = ""
        loadMoreCalls = 0
        stateHolder = mutableStateOf(initial)
        composeRule.setContent {
            val state by stateHolder
            var query by remember { mutableStateOf("") }
            var selectedItems by remember { mutableStateOf<Map<String, CatalogueItem>>(emptyMap()) }
            TierYourLifeTheme {
                CatalogueSearchScreenContent(
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
                    onLoadMore = { loadMoreCalls++ },
                )
            }
        }
    }

    private fun resultRows(count: Int) = List(count) { index ->
        CatalogueItem(id = "tmdb:$index", title = "Title $index", subtitle = null, imageUrl = null)
    }

    private fun topOf(id: String): Float =
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(id)).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun reachingTheEndOfTheResults_asksForTheNextPage() {
        val results = resultRows(40)
        setContent(CatalogueSearchUiState.Success(results, canLoadMore = true))
        composeRule.runOnIdle { assertEquals(0, loadMoreCalls) }

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_RESULTS_LIST)
            .performScrollToIndex(results.lastIndex)

        composeRule.runOnIdle { assertTrue("scrolling to the end should ask for more", loadMoreCalls > 0) }
    }

    // The reader ticks films as they read down the list, so a page arriving
    // underneath must not move or forget the rows they have already been over.
    @Test
    fun aPageArriving_leavesTheTicksAndTheOrderAboveItAlone() {
        val alreadyThere = listOf(dune, arrival, interstellar)
        setContent(CatalogueSearchUiState.Success(alreadyThere, canLoadMore = true))
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(arrival.id)).performClick()
        val orderBefore = alreadyThere.map { topOf(it.id) }

        val nextPage = CatalogueItem(id = "tmdb:335984", title = "Blade Runner 2049", subtitle = "2017", imageUrl = null)
        composeRule.runOnIdle {
            stateHolder.value = CatalogueSearchUiState.Success(alreadyThere + nextPage, canLoadMore = false)
        }

        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(arrival.id)).assertIsSelected()
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).assertIsNotSelected()
        assertEquals(orderBefore, alreadyThere.map { topOf(it.id) })
        assertTrue("the new film belongs below the ones already read", topOf(nextPage.id) > orderBefore.last())
    }

    @Test
    fun whileTheNextPageIsOnItsWay_theFootOfTheListSaysSo() {
        val results = resultRows(40)
        setContent(CatalogueSearchUiState.Success(results, canLoadMore = true, loadingMore = true))

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_RESULTS_LIST)
            .performScrollToIndex(results.size)

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_LOADING_MORE).assertIsDisplayed()
    }

    @Test
    fun tappingAResult_marksIt_andTappingAgainUnmarksIt() {
        setContent(CatalogueSearchUiState.Success(listOf(dune, arrival)))
        val row = composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id))

        row.assertIsNotSelected()
        row.performClick()
        row.assertIsSelected()
        row.performClick()
        row.assertIsNotSelected()
    }

    @Test
    fun selectedCounter_matchesNumberOfMarkedItems() {
        setContent(CatalogueSearchUiState.Success(listOf(dune, arrival, interstellar)))

        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).performClick()
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(arrival.id)).performClick()

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_SELECTED_COUNT).assertTextEquals("2 selected")
    }

    @Test
    fun marksPersist_throughANewSearchAndItsLoadingState() {
        setContent(CatalogueSearchUiState.Success(listOf(dune)))
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).performClick()

        composeRule.runOnIdle { stateHolder.value = CatalogueSearchUiState.Loading }
        composeRule.runOnIdle { stateHolder.value = CatalogueSearchUiState.Success(listOf(arrival)) }
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(arrival.id)).performClick()

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_CONFIRM).performClick()
        composeRule.runOnIdle {
            assertEquals(setOf(dune, arrival), confirmed?.toSet())
        }
    }

    @Test
    fun marksPersist_throughAnEmptyResultAndAnErrorInBetween() {
        setContent(CatalogueSearchUiState.Success(listOf(dune)))
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).performClick()

        composeRule.runOnIdle { stateHolder.value = CatalogueSearchUiState.Empty(query = "zzz") }
        composeRule.runOnIdle { stateHolder.value = CatalogueSearchUiState.Error }
        composeRule.runOnIdle { stateHolder.value = CatalogueSearchUiState.Success(listOf(dune)) }

        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).assertIsSelected()
    }

    @Test
    fun confirming_yieldsExactlyTheMarkedSet_inOneCall() {
        setContent(CatalogueSearchUiState.Success(listOf(dune, arrival, interstellar)))
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).performClick()
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(interstellar.id)).performClick()

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_CONFIRM).performClick()

        composeRule.runOnIdle {
            assertEquals(setOf(dune, interstellar), confirmed?.toSet())
        }
    }

    @Test
    fun emptySelection_confirmIsUnavailable() {
        setContent(CatalogueSearchUiState.Success(listOf(dune)))

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_CONFIRM).assertHasNoClickAction()
    }

    @Test
    fun closeIcon_invokesOnClose() {
        setContent(CatalogueSearchUiState.Initial)

        composeRule.onNodeWithContentDescription("Close search").performClick()

        composeRule.runOnIdle { assertTrue(closed) }
    }

    @Test
    fun clearIcon_appearsOnlyWithQuery_andClearsTextWhenTapped() {
        setContent(CatalogueSearchUiState.Initial)

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_FIELD).performTextInput("Dune")
        composeRule.onNodeWithContentDescription("Clear search").performClick()

        composeRule.runOnIdle { assertEquals("", lastQuery) }
    }

    @Test
    fun bottomBar_staysVisible_whenTheResultListDoesNotFitOnScreen() {
        val manyResults = List(40) { index ->
            CatalogueItem(id = "tmdb:$index", title = "Title $index", subtitle = null, imageUrl = null)
        }
        setContent(CatalogueSearchUiState.Success(manyResults))

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_BOTTOM_BAR).assertIsDisplayed()
    }

    @Test
    fun lastRow_isReachable_andNotCoveredByTheBottomBar() {
        val manyResults = List(40) { index ->
            CatalogueItem(id = "tmdb:$index", title = "Title $index", subtitle = null, imageUrl = null)
        }
        setContent(CatalogueSearchUiState.Success(manyResults))

        val lastTag = CatalogueSearchTestTags.itemSearchResult(manyResults.last().id)
        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_RESULTS_LIST)
            .performScrollToIndex(manyResults.lastIndex)
        composeRule.onNodeWithTag(lastTag).assertIsDisplayed()

        val lastRowBottom = composeRule.onNodeWithTag(lastTag).fetchSemanticsNode().boundsInRoot.bottom
        val barTop = composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_BOTTOM_BAR)
            .fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "last row (bottom=$lastRowBottom) must not extend past where the bottom bar starts (top=$barTop)",
            lastRowBottom <= barTop + 0.5f,
        )
    }

    @Test
    fun selectedRowTint_spansTheFullRowWidth() {
        setContent(CatalogueSearchUiState.Success(listOf(dune, arrival)))
        composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id)).performClick()

        val rowWidth = composeRule.onNodeWithTag(CatalogueSearchTestTags.itemSearchResult(dune.id))
            .fetchSemanticsNode().boundsInRoot.width
        val listWidth = composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_RESULTS_LIST)
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
