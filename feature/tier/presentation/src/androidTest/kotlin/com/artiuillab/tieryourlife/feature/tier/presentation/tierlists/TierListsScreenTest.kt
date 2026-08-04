package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
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
        setScreen(TierListsUiState.Success(lists))

        composeRule.onNodeWithText(string(R.string.tier_lists_title)).assertIsDisplayed()
        composeRule.onNodeWithText(summary(listCount = 2, rankedCount = 19)).assertIsDisplayed()
        composeRule.onNodeWithText("Sci-fi films").assertIsDisplayed()
        composeRule.onNodeWithText("Every A24 film").assertIsDisplayed()
        composeRule.onNodeWithText(cardCounts(ranked = 7, pool = 6)).assertIsDisplayed()
        composeRule.onNodeWithText(cardCounts(ranked = 12, pool = 4)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.tier_lists_recent_changes)).assertIsDisplayed()
    }

    @Test
    fun themeButton_clickInvokesToggleCallback() {
        var calls = 0
        setScreen(TierListsUiState.Success(emptyList()), onToggleTheme = { calls++ })

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_lists_content_description_toggle_theme),
        ).performClick()

        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun tierListCard_clickPassesIdToCallback() {
        var clickedId: Long? = null
        setScreen(
            TierListsUiState.Success(listOf(tierList(7L, "Sci-fi films", intArrayOf(1, 0, 0, 0, 0, 1)))),
            onTierListClick = { clickedId = it },
        )

        composeRule.onNodeWithTag("tier_list_card_7").performClick()

        composeRule.runOnIdle { assertEquals(7L, clickedId) }
    }

    @Test
    fun loadingState_displaysProgressIndicator() {
        setScreen(TierListsUiState.Loading)

        composeRule.onNodeWithTag("tier_lists_loading").assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        setScreen(TierListsUiState.Error("Test failure"))

        composeRule.onNodeWithText("Test failure").assertIsDisplayed()
    }

    @Test
    fun emptySuccessState_displaysZeroCounts() {
        setScreen(TierListsUiState.Success(emptyList()))

        composeRule.onNodeWithText(summary(listCount = 0, rankedCount = 0)).assertIsDisplayed()
    }

    // Exercises OnResumeEffect directly against a lifecycle we drive by hand, rather
    // than through real navigation — this is what proves the trigger itself is sound:
    // one catch-up resume for the screen's first appearance, none for an unrelated
    // recomposition, and exactly one more for a genuine pause/resume cycle (leaving
    // for the detail screen and coming back).
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
                // Read so this block recomposes when the trigger changes, without ever
                // touching the lifecycle itself.
                recomposeTrigger
                OnResumeEffect(onResume = { resumeCount++ })
            }
        }

        // Registering the observer on an already-resumed lifecycle delivers one
        // catch-up ON_RESUME — this is what covers the screen's very first appearance.
        composeRule.runOnIdle { assertEquals(1, resumeCount) }

        // A plain recomposition, lifecycle untouched, must not count as a second resume.
        recomposeTrigger++
        composeRule.runOnIdle { assertEquals(1, resumeCount) }

        // Leaving and returning to the screen is a real pause/resume cycle, and must.
        composeRule.runOnIdle {
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeRule.runOnIdle { assertEquals(2, resumeCount) }
    }

    private fun setScreen(
        state: TierListsUiState,
        onTierListClick: (Long) -> Unit = {},
        onToggleTheme: () -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(
                    state = state,
                    onTierListClick = onTierListClick,
                    onToggleTheme = onToggleTheme,
                )
            }
        }
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

// A LifecycleOwner whose state this test drives by hand, standing in for the
// navigation back stack entry's own lifecycle without needing real navigation.
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
