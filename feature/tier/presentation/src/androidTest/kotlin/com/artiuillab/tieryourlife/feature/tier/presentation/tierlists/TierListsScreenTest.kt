package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.OnResumeEffect
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierListsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successState_displaysTitleSummaryAndTierLists() {
        val lists = initialLists()
        setScreen(successState(lists))

        composeRule.onNodeWithText(string(R.string.tier_lists_title)).assertIsDisplayed()
        composeRule.onNodeWithText(summary(listCount = 2, rankedCount = 19)).assertIsDisplayed()
        composeRule.onNodeWithText("Sci-fi films").assertIsDisplayed()
        composeRule.onNodeWithText("Every A24 film").assertIsDisplayed()
        composeRule.onNodeWithText(cardCounts(ranked = 7, pool = 6)).assertIsDisplayed()
        composeRule.onNodeWithText(cardCounts(ranked = 12, pool = 4)).assertIsDisplayed()
    }

    @Test
    fun settingsButton_clickInvokesCallback() {
        var calls = 0
        setScreen(successState(emptyList()), onSettingsClick = { calls++ })

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_lists_content_description_settings),
        ).performClick()

        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun tierListCard_clickPassesIdToCallback() {
        var clickedId: Long? = null
        setScreen(
            successState(listOf(tierList(7L, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 1)))),
            onTierListClick = { clickedId = it },
        )

        composeRule.onNodeWithTag("tier_list_card_7").performClick()

        composeRule.runOnIdle { assertEquals(7L, clickedId) }
    }

    @Test
    fun loadingState_displaysProgressIndicator() {
        setScreen(TierListsUiState.Loading)

        composeRule.onNodeWithTag(TierListsTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        setScreen(TierListsUiState.Error)

        composeRule.onNodeWithText(string(R.string.tier_lists_load_error)).assertIsDisplayed()
    }

    @Test
    fun emptySuccessState_hidesSummaryLine_andShowsTheEmptyState() {
        setScreen(successState(emptyList()))

        composeRule.onNodeWithText(string(R.string.tier_lists_title)).assertIsDisplayed()
        composeRule.onNodeWithText(summary(listCount = 0, rankedCount = 0)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_empty_title)).assertIsDisplayed()
    }

    @Test
    fun search_filtersByTitle_caseInsensitiveAndAnywhereInTheName() {
        val lists = listOf(
            tierList(1L, "Pizza in Lisbon", intArrayOf(0, 0, 0, 0, 0, 0)),
            tierList(2L, "Sushi tour", intArrayOf(0, 0, 0, 0, 0, 0)),
        )
        setLiveScreen(lists)

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_lists_content_description_search),
        ).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.SEARCH_FIELD).performTextInput("piz")

        composeRule.onNodeWithText("Pizza in Lisbon").assertIsDisplayed()
        composeRule.onNodeWithText("Sushi tour").assertDoesNotExist()
        composeRule.onNodeWithText(plural(R.plurals.search_results_count, 1)).assertIsDisplayed()
    }

    @Test
    fun search_withNoMatches_showsTheNoResultsState() {
        val lists = listOf(tierList(1L, "Pizza in Lisbon", intArrayOf(0, 0, 0, 0, 0, 0)))
        setLiveScreen(lists)

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_lists_content_description_search),
        ).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.SEARCH_FIELD).performTextInput("sushi")

        composeRule.onNodeWithTag(TierListsTestTags.SEARCH_NO_RESULTS).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.search_no_results_title, "sushi")).assertIsDisplayed()
    }

    @Test
    fun search_closeButton_returnsToBrowsingAndRestoresTheFab() {
        val lists = listOf(tierList(1L, "Pizza in Lisbon", intArrayOf(0, 0, 0, 0, 0, 0)))
        setLiveScreen(lists)

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_lists_content_description_search),
        ).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.SEARCH_CLOSE).performClick()

        composeRule.onNodeWithTag(TierListsTestTags.FAB).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.tier_lists_title)).assertIsDisplayed()
    }

    @Test
    fun longPress_entersSelectionMode_showingTheContextualBarAndHidingTheFab() {
        val lists = listOf(tierList(7L, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 1)))
        setLiveScreen(lists)

        composeRule.onNodeWithTag("tier_list_card_7").performTouchInput { longClick() }

        composeRule.onNodeWithTag(TierListsTestTags.SELECTION_BAR).assertIsDisplayed()
        composeRule.onNodeWithText(plural(R.plurals.selection_count, 1)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.FAB).assertDoesNotExist()
    }

    @Test
    fun deselectingTheLastCard_exitsSelectionMode() {
        val lists = listOf(tierList(7L, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 1)))
        setLiveScreen(lists)

        composeRule.onNodeWithTag("tier_list_card_7").performTouchInput { longClick() }
        composeRule.onNodeWithTag(TierListsTestTags.SELECTION_BAR).assertIsDisplayed()

        composeRule.onNodeWithTag("tier_list_card_7").performClick()

        composeRule.onNodeWithTag(TierListsTestTags.SELECTION_BAR).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.FAB).assertIsDisplayed()
    }

    @Test
    fun enteringSelectionMode_showsACheckboxOnEveryCard_checkedOnlyOnTheSelectedOne() {
        val lists = listOf(
            tierList(1L, "First", intArrayOf(0, 0, 0, 0, 0, 0)),
            tierList(2L, "Second", intArrayOf(0, 0, 0, 0, 0, 0)),
        )
        setLiveScreen(lists)

        composeRule.onNodeWithTag("tier_list_card_1").performTouchInput { longClick() }

        composeRule.onNodeWithTag("tier_list_card_1").assertIsOn()
        composeRule.onNodeWithTag("tier_list_card_2").assertIsOff()
    }

    @Test
    fun selectionMode_keepsTheHeadingAndSummaryInPlace() {
        val lists = listOf(tierList(7L, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 1)))
        setLiveScreen(lists)

        composeRule.onNodeWithTag("tier_list_card_7").performTouchInput { longClick() }

        composeRule.onNodeWithTag(TierListsTestTags.SELECTION_BAR).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.tier_lists_title)).assertIsDisplayed()
        composeRule.onNodeWithText(summary(listCount = 1, rankedCount = 1)).assertIsDisplayed()
    }

    @Test
    fun selectionMode_tappingAnotherCard_addsItRatherThanOpeningIt() {
        var openedId: Long? = null
        val lists = listOf(
            tierList(1L, "First", intArrayOf(0, 0, 0, 0, 0, 0)),
            tierList(2L, "Second", intArrayOf(0, 0, 0, 0, 0, 0)),
        )
        setLiveScreen(lists, onTierListClick = { openedId = it })

        composeRule.onNodeWithTag("tier_list_card_1").performTouchInput { longClick() }
        composeRule.onNodeWithTag("tier_list_card_2").performClick()

        composeRule.onNodeWithText(plural(R.plurals.selection_count, 2)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(null, openedId) }
    }

    @Test
    fun delete_removesTheCardsAndShowsTheUndoSnackbar_andUndoRestoresThem() {
        val lists = listOf(
            tierList(1L, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 1)),
            tierList(2L, "Every A24 film", intArrayOf(1, 0, 0, 0, 0, 1)),
        )
        setLiveScreen(lists)

        composeRule.onNodeWithTag("tier_list_card_1").performTouchInput { longClick() }
        composeRule.onNodeWithTag(TierListsTestTags.SELECTION_DELETE).performClick()

        composeRule.onNodeWithTag("tier_list_card_1").assertDoesNotExist()
        composeRule.onNodeWithTag("tier_list_card_2").assertIsDisplayed()
        composeRule.onNodeWithText(plural(R.plurals.snack_lists_deleted, 1)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.SELECTION_BAR).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.action_undo)).performClick()

        composeRule.onNodeWithTag("tier_list_card_1").assertIsDisplayed()
    }

    @Test
    fun onResumeEffect_firesOnResumeButNotOnPlainRecomposition() {
        var resumeCount = 0
        lateinit var lifecycleOwner: ManualLifecycleOwner
        var recomposeTrigger by mutableIntStateOf(0)

        composeRule.setContent {
            val owner = remember {
                ManualLifecycleOwner().apply {
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                }
            }
            lifecycleOwner = owner
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                recomposeTrigger
                OnResumeEffect(onResume = { resumeCount++ })
            }
        }

        composeRule.runOnIdle { assertEquals(1, resumeCount) }

        recomposeTrigger++
        composeRule.runOnIdle { assertEquals(1, resumeCount) }

        composeRule.runOnIdle {
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeRule.runOnIdle { assertEquals(2, resumeCount) }
    }

    @Test
    fun emptyState_offersStartingPointsRatherThanJustSayingItIsEmpty() {
        setScreen(TierListsUiState.Success(emptyList(), 0, 0))

        composeRule.onNodeWithTag(TierListsTestTags.EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.suggestion(0)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.suggestion(4)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyState_aSuggestionCreatesTheBoardUnderItsOwnName() {
        var created: String? = null
        setScreen(TierListsUiState.Success(emptyList(), 0, 0), onCreateNamedList = { created = it })

        composeRule.onNodeWithTag(TierListsTestTags.suggestion(0)).performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(string(R.string.home_suggestion_films), created) }
    }

    @Test
    fun withLists_theSuggestionsAreGone() {
        setScreen(TierListsUiState.Success(listOf(tierList(1, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 0))), 1, 1))

        composeRule.onNodeWithTag(TierListsTestTags.EMPTY_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.suggestion(0)).assertDoesNotExist()
    }

    // Searching narrows which boards are on screen. It does not change what a
    // board looks like -- and having them turn from pictures into rows made
    // the results read as somebody else's list rather than a subset of yours.
    @Test
    fun searching_keepsTheBoardsAsPicturesWhenThatIsHowTheyWereShown() {
        setScreen(
            TierListsUiState.Success(
                lists = listOf(tierList(1, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 0))),
                totalListCount = 1,
                rankedCount = 1,
                mode = HomeMode.Searching("sci"),
                asPictures = true,
            ),
        )

        composeRule.onNodeWithTag(TierListsTestTags.tile(1)).assertIsDisplayed()
    }

    @Test
    fun searching_keepsTheBoardsAsRowsWhenThatIsHowTheyWereShown() {
        setScreen(
            TierListsUiState.Success(
                lists = listOf(tierList(1, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 0))),
                totalListCount = 1,
                rankedCount = 1,
                mode = HomeMode.Searching("sci"),
                asPictures = false,
            ),
        )

        composeRule.onNodeWithTag(TierListsTestTags.tile(1)).assertDoesNotExist()
        composeRule.onNodeWithText("Sci-fi films").assertIsDisplayed()
    }

    private fun setScreen(
        state: TierListsUiState,
        onTierListClick: (Long) -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onCreateNamedList: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(
                    state = state,
                    onTierListClick = onTierListClick,
                    onSettingsClick = onSettingsClick,
                    onCreateNamedList = onCreateNamedList,
                )
            }
        }
    }

    private fun setLiveScreen(
        initialLists: List<TierList>,
        onTierListClick: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            var lists by remember { mutableStateOf(initialLists) }
            var mode by remember { mutableStateOf<HomeMode>(HomeMode.Browsing) }
            val query = (mode as? HomeMode.Searching)?.query
            val visibleLists = if (query != null) {
                lists.filter { it.title.contains(query, ignoreCase = true) }
            } else {
                lists
            }
            val rankedCount = lists.sumOf { list -> list.tiers.filterNot { it.isPool }.sumOf { it.items.size } }

            TierYourLifeTheme {
                TierListsScreenContent(
                    state = TierListsUiState.Success(visibleLists, lists.size, rankedCount, mode),
                    onTierListClick = onTierListClick,
                    onSearchClick = { mode = HomeMode.Searching("") },
                    onSearchQueryChange = { newQuery -> mode = HomeMode.Searching(newQuery) },
                    onCloseSearch = { mode = HomeMode.Browsing },
                    onLongPressCard = { id -> mode = HomeMode.Selecting(setOf(id)) },
                    onToggleSelected = { id ->
                        val current = mode as? HomeMode.Selecting
                        if (current != null) {
                            val updated = if (id in current.selectedIds) {
                                current.selectedIds - id
                            } else {
                                current.selectedIds + id
                            }
                            mode = if (updated.isEmpty()) HomeMode.Browsing else HomeMode.Selecting(updated)
                        }
                    },
                    onCloseSelection = { mode = HomeMode.Browsing },
                    onDeleteLists = { ids ->
                        lists = lists.filterNot { it.id in ids }
                        mode = HomeMode.Browsing
                    },
                    onUndoDelete = { ids ->
                        lists = lists + initialLists.filter { it.id in ids }
                    },
                )
            }
        }
    }

    private fun successState(lists: List<TierList>, mode: HomeMode = HomeMode.Browsing): TierListsUiState.Success {
        val rankedCount = lists.sumOf { list -> list.tiers.filterNot { it.isPool }.sumOf { it.items.size } }
        return TierListsUiState.Success(lists, lists.size, rankedCount, mode)
    }

    private fun summary(listCount: Int, rankedCount: Int): String = string(
        R.string.tier_lists_summary,
        plural(R.plurals.tier_lists_count, listCount),
        plural(R.plurals.tier_lists_rankings_count, rankedCount),
        string(R.string.tier_lists_private),
    )

    private fun cardCounts(ranked: Int, pool: Int): String = string(
        R.string.tier_lists_card_ranked_and_in_pool,
        plural(R.plurals.tier_lists_ranked_count, ranked),
        plural(R.plurals.tier_lists_in_pool_count, pool),
    )

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun plural(resourceId: Int, count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(resourceId, count, count)

    private fun initialLists(): List<TierList> = listOf(
        tierList(1L, "Sci-fi films", intArrayOf(2, 2, 1, 1, 1, 6)),
        tierList(2L, "Every A24 film", intArrayOf(3, 3, 3, 2, 1, 4)),
    )
}

private class ManualLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}

private fun tierList(id: Long, title: String, counts: IntArray): TierList = TierList(
    id = id,
    title = title,
    tiers = listOf("S", "A", "B", "C", "D", "Pool").mapIndexed { index, label ->
        Tier(
            id = id * 100 + index,
            label = label,
            colorLight = "#000000",
            colorDark = "#000000",
            items = List(counts[index]) { itemIndex ->
                TierItem(
                    id = id * 10_000 + index * 100 + itemIndex,
                    title = "item_$itemIndex",
                    imageUrl = null,
                )
            },
            isPool = label == "Pool",
        )
    },
)
