package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rows, Single strip or Ranked list from a button in the bar, as on the web (Danylo, 16.11.2026). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.MediumTablet)
class ViewButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val chosen = mutableListOf<TierListDisplayMode>()

    private val placed = previewTierList.copy(tiers = previewTierList.tiers.map { if (it.isPool) it.copy(items = emptyList()) else it })

    @Test
    fun theMenu_offersTheOtherViews_andTheChosenOneChangesNothing() {
        show(placed)
        composeRule.onNodeWithTag(TierDetailTestTags.VIEW_MODE).assertContentDescriptionEquals("Display: Wrapped rows").performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.viewMode(TierListDisplayMode.WRAP.name)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.VIEW_MODE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.viewMode(TierListDisplayMode.HORIZONTAL_SCROLL.name)).performClick()
        assertEquals(listOf(TierListDisplayMode.HORIZONTAL_SCROLL), chosen)
    }

    @Test
    fun namesOnCards_standsUnderTheViews_checked_andTurnsOffForEveryList() {
        val memory = InMemoryBoardLayoutMemory()
        composeRule.setContent {
            CompositionLocalProvider(LocalBoardLayoutMemory provides memory) {
                TierYourLifeTheme {
                    TierDetailScreenContent(
                        state = TierDetailUiState.Success(placed),
                        actions = TierDetailActions(onPlay = {}, onPublish = {}, onSetDisplayMode = { chosen += it }),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(TierDetailTestTags.VIEW_MODE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.NAMES_ON_CARDS).assertIsOn().performClick()
        assertFalse(memory.namesOnCards)

        composeRule.onNodeWithTag(TierDetailTestTags.VIEW_MODE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.NAMES_ON_CARDS).assertIsOff().performClick()
        assertTrue(memory.namesOnCards)
        assertTrue(chosen.isEmpty())
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun onAPhone_itGivesWayToPublish() {
        show(placed)
        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.VIEW_MODE)).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun onAPhone_withNothingToPublish_itIsThere() {
        show(placed.copy(publishedId = "p", displayMode = TierListDisplayMode.FLAT_RANKED))
        composeRule.onNodeWithTag(TierDetailTestTags.VIEW_MODE).assertContentDescriptionEquals("Display: Ranked list")
    }

    @Test
    fun aBoardThatIsNotOnesOwn_hasNone() {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(state = TierDetailUiState.Success(placed), actions = TierDetailActions(onPublish = {}), readOnly = true)
            }
        }
        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.VIEW_MODE)).fetchSemanticsNodes().size)
    }

    private fun show(list: TierList) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    actions = TierDetailActions(onPlay = {}, onPublish = {}, onSetDisplayMode = { chosen += it }),
                )
            }
        }
    }
}
