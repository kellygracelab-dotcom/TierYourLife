package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A card moved within the pool: the slot opens where it would land, the others make room, the drop goes there. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PoolReorderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val moves = mutableListOf<Triple<Long, Long, Int>>()

    @Test
    fun inTheStrip_aCardCarriedRight_opensItsSlotThereAndLandsThere() {
        setBoard()
        val home = bounds(tile(0))
        val fourth = bounds(tile(4))

        liftByPull(tile(0))
        moveTo(Offset(fourth.left + 50f, fourth.center.y))

        composeRule.onNodeWithTag(TierDetailTestTags.DROP_SLOT).assertExists()
        assertEquals("the next card closes the gap", home.left, bounds(tile(1)).left, 1f)
        assertEquals("the slot sits where the third card was", bounds(tile(3)).left + home.width + STRIP_GAP_PX, bounds(TierDetailTestTags.DROP_SLOT).left, 2f)
        drop()

        assertEquals(listOf(Triple(POOL_FIRST, POOL, 3)), moves)
    }

    @Test
    fun inTheStrip_aCardCarriedLeft_landsAfterTheCardWhoseMiddleItPassed() {
        setBoard()
        val first = bounds(tile(1))

        liftByPull(tile(5))
        moveTo(Offset(first.right - 50f, first.center.y))

        composeRule.onNodeWithTag(TierDetailTestTags.DROP_SLOT).assertExists()
        assertEquals("the card after the slot makes room", bounds(TierDetailTestTags.DROP_SLOT).right + STRIP_GAP_PX, bounds(tile(2)).left, 2f)
        drop()

        assertEquals(listOf(Triple(POOL_FIRST + 5, POOL, 2)), moves)
    }

    @Test
    fun aCardFromARow_opensASlotInTheStripAndLandsThere() {
        setBoard()
        val second = bounds(tile(2))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED)).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(60f, 0f))
            advanceEventTime(16)
        }
        moveTo(Offset(second.left + 50f, second.center.y))

        composeRule.onNodeWithTag(TierDetailTestTags.DROP_SLOT).assertExists()
        assertEquals("the slot takes the second card's place", second.left, bounds(TierDetailTestTags.DROP_SLOT).left, 2f)
        drop()

        assertEquals(listOf(Triple(RANKED, POOL, 2)), moves)
    }

    @Test
    @Config(qualifiers = "+land")
    fun inTheColumnBesideTheRows_aCardMovedToTheNextLine_landsThere() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_COLUMN).assertExists()
        val target = bounds(tile(4))

        composeRule.onNodeWithTag(tile(0)).performTouchInput {
            down(center)
            advanceEventTime(HOLD)
            moveBy(Offset(2f, 0f))
            advanceEventTime(16)
        }
        moveTo(Offset(target.left + target.width * 0.25f, target.center.y))

        composeRule.onNodeWithTag(TierDetailTestTags.DROP_SLOT).assertExists()
        drop()

        assertEquals(listOf(Triple(POOL_FIRST, POOL, 3)), moves)
    }

    private fun liftByPull(tag: String) {
        composeRule.onNodeWithTag(tag).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, -60f))
            advanceEventTime(16)
        }
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
    }

    private fun moveTo(rootPoint: Offset) {
        composeRule.onRoot().performTouchInput {
            moveTo(rootPoint)
            advanceEventTime(16)
        }
        composeRule.waitForIdle()
    }

    private fun drop() {
        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    private fun tile(index: Int) = TierDetailTestTags.tile(POOL_FIRST + index)

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun setBoard() {
        val list = TierList(
            id = 1,
            title = "Films",
            tiers = listOf(
                tier(1, "S", listOf(TierItem(id = RANKED, title = "Ranked", imageUrl = null))),
                tier(POOL, "Pool", List(12) { TierItem(id = POOL_FIRST + it, title = "Card $it", imageUrl = null) }, isPool = true),
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
        const val RANKED = 100L
        const val POOL = 6L
        const val POOL_FIRST = 200L
        const val HOLD = 600L

        /** The strip's 10dp between cards on a Pixel 7. */
        const val STRIP_GAP_PX = 26.25f
    }
}
