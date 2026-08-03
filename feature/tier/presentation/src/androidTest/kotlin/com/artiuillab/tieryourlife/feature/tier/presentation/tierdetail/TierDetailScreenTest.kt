package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class TierDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successState_displaysTitleTiersAndPoolCounter() {
        setScreen(TierDetailUiState.Success(defaultList()))

        composeRule.onNodeWithText("Sci-fi films").assertIsDisplayed()
        composeRule.onNodeWithText("S").assertIsDisplayed()
        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
        composeRule.onNodeWithText("C").assertIsDisplayed()
        composeRule.onNodeWithText("D").assertIsDisplayed()
        composeRule.onNodeWithText("Pool · 6 unranked").assertIsDisplayed()
    }

    @Test
    fun successState_tierWithManyItems_rendersWithoutCrashing() {
        // Regression: TierRow used to wrap a LazyRow in Modifier.height(IntrinsicSize.Min),
        // which throws "Asking for intrinsic measurements of SubcomposeLayout layouts is
        // not supported" as soon as a tier holds enough items to need lazy layout.
        // Titles are short so the tile's fallback label (title.take(6).uppercase())
        // stays unique and predictable instead of being truncated into "MOVIE ".
        val manyItems = List(24) { index ->
            TierItem(id = index.toLong(), title = "M$index", imageUrl = null)
        }
        val list = TierList(
            id = 1,
            title = "Big list",
            tiers = listOf(
                tier(id = 1, label = "S", items = manyItems),
                tier(id = 2, label = "A", items = emptyList()),
                tier(id = 3, label = "B", items = emptyList()),
                tier(id = 4, label = "C", items = emptyList()),
                tier(id = 5, label = "D", items = emptyList()),
                tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
            ),
        )

        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierItems(1L)).performScrollToKey(23L)
        composeRule.onNodeWithText("M23").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsIndicatorAndTopBarTogether() {
        setScreen(TierDetailUiState.Loading)

        composeRule.onNodeWithTag(TierDetailTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_back),
        ).assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        setScreen(TierDetailUiState.Error("Tier list not found"))

        composeRule.onNodeWithText("Tier list not found").assertIsDisplayed()
    }

    @Test
    fun backButton_clickInvokesOnBackOnce() {
        var calls = 0
        setScreen(TierDetailUiState.Success(defaultList()), onBack = { calls++ })

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_back),
        ).performClick()

        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun addChip_clickInvokesOnAddClickOnce() {
        var calls = 0
        setScreen(TierDetailUiState.Success(defaultList()), onAddClick = { calls++ })

        composeRule.onNodeWithTag(TierDetailTestTags.ADD_CHIP).performClick()

        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    private fun setScreen(
        state: TierDetailUiState,
        onBack: () -> Unit = {},
        onAddClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = state,
                    onBack = onBack,
                    onAddClick = onAddClick,
                )
            }
        }
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun defaultList(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            tier(id = 1, label = "S", items = List(1) { movie(it) }),
            tier(id = 2, label = "A", items = List(1) { movie(it) }),
            tier(id = 3, label = "B", items = List(1) { movie(it) }),
            tier(id = 4, label = "C", items = List(1) { movie(it) }),
            tier(id = 5, label = "D", items = List(1) { movie(it) }),
            tier(id = 6, label = "Pool", items = List(6) { movie(it) }, isPool = true),
        ),
    )

    private fun movie(index: Int): TierItem = TierItem(id = index.toLong(), title = "Movie $index", imageUrl = null)

    private fun tier(id: Long, label: String, items: List<TierItem>, isPool: Boolean = false): Tier = Tier(
        id = id,
        label = label,
        colorLight = "#000000",
        colorDark = "#000000",
        items = items,
        isPool = isPool,
    )
}
