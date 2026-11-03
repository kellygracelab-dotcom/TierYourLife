package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

/** A finger on a card in a strip that scrolls sideways: sideways is scrolling, the card waits for a hold or an upward pull. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TileLiftTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val moves = mutableListOf<Triple<Long, Long, Int>>()

    @Test
    fun inThePool_aSidewaysSwipeAfterAPause_scrollsTheStripAndLiftsNothing() {
        setBoard(TierListDisplayMode.WRAP)
        val before = bounds(TierDetailTestTags.tile(POOL_FIRST + 2))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST + 2)).performTouchInput {
            down(center)
            advanceEventTime(PAUSE_LONGER_THAN_THE_OLD_HOLD)
            repeat(10) {
                moveBy(Offset(-25f, 2f))
                advanceEventTime(16)
            }
        }
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).performTouchInput { up() }
        composeRule.waitForIdle()

        assertTrue("expected the strip to have scrolled", bounds(TierDetailTestTags.tile(POOL_FIRST + 2)).left < before.left - 100f)
        assertEquals(emptyList<Triple<Long, Long, Int>>(), moves)
    }

    @Test
    fun inThePool_anUpwardPull_liftsTheCardAtOnce() {
        setBoard(TierListDisplayMode.WRAP)
        val source = bounds(TierDetailTestTags.tile(POOL_FIRST))
        val target = bounds(TierDetailTestTags.tierRow(2))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, -60f))
            advanceEventTime(16)
        }
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performTouchInput {
            moveTo(Offset(target.left + target.width * 0.5f, target.center.y) - source.topLeft)
            advanceEventTime(16)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(listOf(POOL_FIRST to 2L), moves.map { it.first to it.second })
    }

    @Test
    fun inThePool_aHold_liftsTheCard() {
        setBoard(TierListDisplayMode.WRAP)

        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performTouchInput {
            down(center)
            advanceEventTime(HOLD)
            moveBy(Offset(30f, 0f))
            advanceEventTime(16)
        }

        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
    }

    @Test
    fun inASidewaysTierStrip_aSwipeEitherWay_liftsNothing() {
        setBoard(TierListDisplayMode.HORIZONTAL_SCROLL)
        val before = bounds(TierDetailTestTags.tile(RANKED_FIRST + 2))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED_FIRST + 2)).performTouchInput {
            down(center)
            advanceEventTime(PAUSE_LONGER_THAN_THE_OLD_HOLD)
            repeat(10) {
                moveBy(Offset(-25f, 0f))
                advanceEventTime(16)
            }
        }
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.tierItems(1)).performTouchInput { up() }
        composeRule.waitForIdle()
        assertTrue("expected the strip to have scrolled", bounds(TierDetailTestTags.tile(RANKED_FIRST + 2)).left < before.left - 100f)

        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED_FIRST + 2)).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, -60f))
            advanceEventTime(16)
        }
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_LIST).performTouchInput { up() }
        composeRule.waitForIdle()

        assertEquals(emptyList<Triple<Long, Long, Int>>(), moves)
    }

    @Test
    fun inASidewaysTierStrip_aHold_liftsTheCard() {
        setBoard(TierListDisplayMode.HORIZONTAL_SCROLL)

        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED_FIRST)).performTouchInput {
            down(center)
            advanceEventTime(HOLD)
            moveBy(Offset(0f, 30f))
            advanceEventTime(16)
        }

        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
    }

    @Test
    fun inAWrappingRow_aSidewaysMove_stillLiftsTheCardAtOnce() {
        setBoard(TierListDisplayMode.WRAP)

        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED_FIRST)).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(60f, 0f))
            advanceEventTime(16)
        }

        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertExists()
    }

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun setBoard(displayMode: TierListDisplayMode) {
        val list = TierList(
            id = 1,
            title = "Films",
            displayMode = displayMode,
            tiers = listOf(
                tier(1, "S", cards(RANKED_FIRST, 12)),
                tier(2, "A", emptyList()),
                tier(6, "Pool", cards(POOL_FIRST, 12), isPool = true),
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

    private fun cards(first: Long, count: Int) = List(count) { TierItem(id = first + it, title = "Card $it", imageUrl = null) }

    private fun tier(id: Long, label: String, items: List<TierItem>, isPool: Boolean = false) =
        Tier(id = id, label = label, colorLight = "#000000", colorDark = "#000000", items = items, isPool = isPool)

    private companion object {
        const val RANKED_FIRST = 100L
        const val POOL_FIRST = 200L
        const val PAUSE_LONGER_THAN_THE_OLD_HOLD = 150L
        const val HOLD = 250L
    }
}
