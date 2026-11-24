package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A phone's pool pulled up through one, two and four lines of cards, never past half the window, and dropped from into the board. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h640dp-mdpi")
class PoolLinesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val memory = InMemoryBoardLayoutMemory()
    private val moves = mutableListOf<Triple<Long, Long, Int>>()
    private lateinit var density: Density

    @Test
    fun aPull_stepsThroughTheLines_andFoldsOnlyFromOne() {
        assertEquals(Pull.Lines(2), pullOutcomeOf(UP, THRESHOLD, walking = true, lines = 1))
        assertEquals(Pull.Lines(4), pullOutcomeOf(UP, THRESHOLD, walking = true, lines = 2))
        assertEquals(Pull.Lines(2), pullOutcomeOf(DOWN, THRESHOLD, walking = true, lines = 4))
        assertEquals(Pull.Lines(1), pullOutcomeOf(DOWN, THRESHOLD, walking = true, lines = 2))
        assertEquals(Pull.Open(false), pullOutcomeOf(DOWN, THRESHOLD, walking = true, lines = 1))
        assertEquals(Pull.Open(true), pullOutcomeOf(UP, THRESHOLD, walking = false, lines = 1))
        assertEquals(null, pullOutcomeOf(THRESHOLD / 2, THRESHOLD, walking = true, lines = 2))
    }

    @Test
    fun onlyWholeLinesAreShown() {
        assertEquals(4, linesThatFit(4, 400.dp))
        assertEquals(2, linesThatFit(4, 200.dp))
        assertEquals(1, linesThatFit(4, 40.dp))
        assertEquals(2, linesThatFit(2, 400.dp))
    }

    @Test
    fun untilPulled_aPhonesPoolKeepsItsOneStrip() {
        setBoard()

        assertEquals(76f, heightDp(TierDetailTestTags.POOL_ITEMS), 1f)
        assertFalse(memory.poolDockRowsChosen)
    }

    @Test
    fun pullingTheHandle_walksOneTwoFour_andBack_rememberingIt() {
        setBoard()

        pull(up = true)
        assertEquals(2, memory.poolDockRows)
        assertTrue(memory.poolDockRowsChosen)
        assertEquals(LINE * 2 + GAP, heightDp(TierDetailTestTags.POOL_ITEMS), 1f)

        pull(up = true)
        assertEquals(4, memory.poolDockRows)

        pull(up = false)
        assertEquals(2, memory.poolDockRows)
        pull(up = false)
        assertEquals(1, memory.poolDockRows)
        assertEquals(LINE, heightDp(TierDetailTestTags.POOL_ITEMS), 1f)

        pull(up = false)
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertDoesNotExist()
    }

    @Test
    fun fourLines_takeHalfThe640Window_atMost_inWholeLines() {
        memory.poolDockRows = 4
        setBoard()

        assertTrue("the pool stays within half the window", heightDp(TierDetailTestTags.POOL_PANEL) <= 320f + 0.5f)
        val shown = heightDp(TierDetailTestTags.POOL_ITEMS)
        val wholeLines = (1..4).map { LINE * it + GAP * (it - 1) }
        assertTrue("$shown is a whole number of lines", wholeLines.any { kotlin.math.abs(it - shown) < 1f })
        assertTrue("more than the one strip", shown > LINE + 1f)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-mdpi")
    fun fourLines_onA411Phone_showAllFour() {
        memory.poolDockRows = 4
        setBoard()

        assertTrue(heightDp(TierDetailTestTags.POOL_PANEL) <= 891f / 2 + 0.5f)
        assertTrue(heightDp(TierDetailTestTags.POOL_ITEMS) >= LINE * 3 + GAP * 2 - 1f)
    }

    @Test
    fun aCardFromTheFourLinePool_dropsIntoTheBottomRowOfTheBoard() {
        memory.poolDockRows = 4
        setBoard()
        val target = composeRule.onNodeWithTag(TierDetailTestTags.tile(BOTTOM_CARD)).fetchSemanticsNode().boundsInRoot

        // Lines scroll up and down, so a card is lifted by holding it, as in the docked pool.
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST + 5)).performTouchInput {
            down(center)
            advanceEventTime(HOLD)
            moveBy(Offset(2f, 0f))
            advanceEventTime(16)
        }
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
        composeRule.onRoot().performTouchInput {
            moveTo(target.center)
            advanceEventTime(16)
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()

        assertEquals(POOL_FIRST + 5, moves.single().first)
        assertEquals(BOTTOM, moves.single().second)
    }

    private fun pull(up: Boolean) {
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_TOGGLE).performTouchInput { if (up) swipeUp() else swipeDown() }
        composeRule.waitForIdle()
    }

    private fun heightDp(tag: String): Float {
        val bounds: Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return bounds.height / density.density
    }

    private fun setBoard() {
        val list = TierList(
            id = 1,
            title = "Films",
            tiers = listOf(
                tier(1, "S", listOf(TierItem(id = 100, title = "Top", imageUrl = null))),
                tier(BOTTOM, "A", listOf(TierItem(id = BOTTOM_CARD, title = "Bottom", imageUrl = null))),
                tier(POOL, "Pool", List(40) { TierItem(id = POOL_FIRST + it, title = "Card $it", imageUrl = null) }, isPool = true),
            ),
        )
        composeRule.setContent {
            density = LocalDensity.current
            CompositionLocalProvider(LocalBoardLayoutMemory provides memory) {
                TierYourLifeTheme {
                    TierDetailScreenContent(
                        state = TierDetailUiState.Success(list),
                        actions = TierDetailActions(onMoveItem = { id, tier, at -> moves += Triple(id, tier, at) }),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun tier(id: Long, label: String, items: List<TierItem>, isPool: Boolean = false) =
        Tier(id = id, label = label, colorLight = "#000000", colorDark = "#000000", items = items, isPool = isPool)

    private companion object {
        const val BOTTOM = 2L
        const val BOTTOM_CARD = 101L
        const val POOL = 6L
        const val POOL_FIRST = 200L
        const val HOLD = 600L
        const val LINE = 76f
        const val GAP = 10f
        const val THRESHOLD = 24f
        const val UP = -40f
        const val DOWN = 40f
    }
}
