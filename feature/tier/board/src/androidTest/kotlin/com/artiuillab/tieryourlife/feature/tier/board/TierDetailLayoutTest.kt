package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TIER_BAND_WIDTH
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@RunWith(AndroidJUnit4::class)
class TierDetailLayoutTest : TierDetailScreenTestBase() {

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
    fun successState_tierWithManyItems_wrapsToNewLinesAndGrowsTaller() {
        val manyItems = List(24) { index ->
            TierItem(id = index.toLong(), title = "M$index", imageUrl = null)
        }
        val list = listOf(
            tier(id = 1, label = "S", items = manyItems),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()

        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithText("M23").assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1L)).assertHeightIsAtLeast(156.dp)
    }

    @Test
    fun successState_tierWithThreeItems_occupiesOneLineAtOriginalHeight() {
        val list = listOf(
            tier(id = 1, label = "S", items = List(3) { tierItem(it) }),
        ).asTierList()

        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1L)).assertHeightIsEqualTo(84.dp)
    }

    // A bar with an empty title appeared and then filled in, which is what a
    // freshly created list flickered. The screen waits instead. A list that
    // cannot be loaded still draws its own bar, so back is never truly gone.
    @Test
    fun loadingState_showsTheIndicatorWithoutAHalfDrawnTopBar() {
        setScreen(TierDetailUiState.Loading)

        composeRule.onNodeWithTag(TierDetailTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            string(ThemeR.string.tier_detail_content_description_back),
        ).assertDoesNotExist()
    }

    @Test
    fun errorState_stillOffersAWayBack() {
        setScreen(TierDetailUiState.Error)

        composeRule.onNodeWithContentDescription(
            string(ThemeR.string.tier_detail_content_description_back),
        ).assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        setScreen(TierDetailUiState.Error)

        composeRule.onNodeWithText(string(R.string.tier_detail_not_found)).assertIsDisplayed()
    }

    @Test
    fun openingAList_leavesTheTitleAsText_soNoKeyboardAppears() {
        setScreen(TierDetailUiState.Success(defaultList()))

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).assert(hasSetTextAction().not())
    }

    @Test
    fun tappingTheTitle_isWhatOpensTheEditor() {
        setScreen(TierDetailUiState.Success(defaultList()))

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).assert(hasSetTextAction())
    }

    @Test
    fun backButton_clickInvokesOnBackOnce() {
        var calls = 0
        setScreen(TierDetailUiState.Success(defaultList()), onBack = { calls++ })

        composeRule.onNodeWithContentDescription(
            string(ThemeR.string.tier_detail_content_description_back),
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

    // The letters are the spine of the board and only work as a column, so a
    // caption can never push its own band wider than the others.
    @Test
    fun everyTierBand_isOneWidth_whateverItsCaption() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "X"),
            tier(
                id = 2,
                label = "A",
                items = emptyList(),
                caption = "A very very long caption that keeps going and going and going",
            ),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        val shortBandWidth = composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).fetchSemanticsNode().boundsInRoot.width
        val longBandWidth = composeRule.onNodeWithTag(TierDetailTestTags.tierBand(2)).fetchSemanticsNode().boundsInRoot.width
        val fixedWidthPx = with(composeRule.density) { TIER_BAND_WIDTH.toPx() }

        assertEquals("both bands must be one width", longBandWidth, shortBandWidth, 1f)
        assertEquals("and that width is the fixed one", fixedWidthPx, longBandWidth, 1f)
    }

    @Test
    fun largeFontScale_hidesTierCaption_keepsTheLetter() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "Masterpiece"),
        ).asTierList()

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = 2f)) {
                TierYourLifeTheme {
                    TierDetailScreenContent(state = TierDetailUiState.Success(list))
                }
            }
        }

        composeRule.onNodeWithText("S").assertIsDisplayed()
        composeRule.onNodeWithText("Masterpiece").assertDoesNotExist()
    }

    // Someone else's pool is material to rank, not somewhere to add to, and the
    // trash would offer to destroy cards the reader does not own.
    @Test
    fun aReadOnlyBoard_offersNothingThatWouldChangeSomeoneElsesList() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = List(2) { tierItem(it + 100) }, isPool = true),
        ).asTierList()

        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    actions = TierDetailActions(),
                    readOnly = true,
                    subtitle = "by Olena M.",
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.ADD_CHIP).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.GENERATE_CHIP).assertDoesNotExist()
        // A reader's pool starts folded; the cards are there once it is opened.
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_TOGGLE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertIsDisplayed()
    }
}
