package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A wrapping row of three hundred cards composes the lines on screen, and a card lifted from it survives the row scrolling away. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class BigTierTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val moves = mutableListOf<Triple<Long, Long, Int>>()

    @Test
    fun aRowOfThreeHundredCards_composesOnlyTheLinesOnScreen() {
        setBoard(cards = 300)

        val composed = composedTiles()
        // Five to a line on this phone, about nine lines on screen, two more each side.
        assertTrue("expected the visible cards, got $composed", composed in 30..90)
        composeRule.onNodeWithTag(TierDetailTestTags.tile(FIRST)).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(FIRST + 299)).assertDoesNotExist()
    }

    @Test
    fun scrolledPastTheRow_itsLastCardsAreComposedAndTheFirstAreNot() {
        setBoard(cards = 300)

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_LIST).performScrollToIndex(2)
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_LIST).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -300f) }

        composeRule.onNodeWithTag(TierDetailTestTags.tile(FIRST + 299)).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(FIRST)).assertDoesNotExist()
        assertTrue(composedTiles() < 90)
    }

    @Test
    fun theRowTakesItsWholeHeight_composedOrNot() {
        setBoard(cards = 300)

        val row = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(BIG)).fetchSemanticsNode()
        val first = bounds(TierDetailTestTags.tile(FIRST))
        // Sixty lines of a 64dp card and an 8dp gap, and the row's padding.
        val expected = (60 * 64 + 59 * 8 + 2 * 10) * DENSITY
        assertEquals(expected, row.size.height.toFloat(), 2f)
        assertEquals(first.top - row.positionInRoot.y, 10 * DENSITY, 1f)
    }

    @Test
    fun aCardLiftedFromTheTop_isCarriedDownByAutoscrollAndDroppedInTheLastRow() {
        setBoard(cards = 120, middle = 120)
        val card = bounds(TierDetailTestTags.tile(FIRST))
        val list = bounds(TierDetailTestTags.TIER_LIST)

        composeRule.onRoot().performTouchInput {
            down(card.center)
            advanceEventTime(16)
            moveBy(Offset(60f, 0f))
            advanceEventTime(16)
            moveTo(Offset(list.center.x, list.bottom - 4f))
            advanceEventTime(16)
        }
        composeRule.mainClock.advanceTimeBy(AUTOSCROLL_MILLIS)

        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(BIG)).assertIsNotDisplayed()
        val last = bounds(TierDetailTestTags.tierRow(LAST))
        composeRule.onRoot().performTouchInput {
            moveTo(Offset(last.right - 8f, last.center.y))
            advanceEventTime(16)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(listOf(Triple(FIRST, LAST, 2)), moves)
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertDoesNotExist()
    }

    private fun composedTiles(): Int = composeRule.onAllNodesWithTagPrefix("tier_detail_tile_").size

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagPrefix(prefix: String) = onAllNodes(
        SemanticsMatcher("tag starts with $prefix") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true },
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun setBoard(cards: Int, middle: Int = 0) {
        val list = TierList(
            id = 1,
            title = "Films",
            displayMode = TierListDisplayMode.WRAP,
            tiers = listOf(
                tier(BIG, "S", List(cards) { TierItem(id = FIRST + it, title = "Card $it", imageUrl = null) }),
                tier(MIDDLE, "B", List(middle) { TierItem(id = MIDDLE_FIRST + it, title = "Middle $it", imageUrl = null) }),
                tier(LAST, "A", List(2) { TierItem(id = OTHER + it, title = "Other $it", imageUrl = null) }),
                tier(POOL, "Pool", emptyList(), isPool = true),
            ),
        )
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    actions = TierDetailActions(onMoveItem = { id, tier, at -> moves += Triple(id, tier, at) }),
                )
            }
        }
    }

    private fun tier(id: Long, label: String, items: List<TierItem>, isPool: Boolean = false) =
        Tier(id = id, label = label, colorLight = "#000000", colorDark = "#000000", items = items, isPool = isPool)

    private companion object {
        const val BIG = 1L
        const val LAST = 2L
        const val POOL = 3L
        const val MIDDLE = 4L
        const val MIDDLE_FIRST = 3_000L
        const val FIRST = 1_000L
        const val OTHER = 5_000L
        const val DENSITY = 2.625f
        const val AUTOSCROLL_MILLIS = 8_000L
    }
}
