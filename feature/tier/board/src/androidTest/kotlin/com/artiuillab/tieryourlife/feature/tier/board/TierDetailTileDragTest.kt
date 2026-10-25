package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDetailTileDragTest : TierDetailScreenTestBase() {

    // The finger does not have to wait: moving sideways lifts the card at once.
    @Test
    fun aSidewaysMove_liftsTheCardWithoutWaiting() {
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(defaultList()), onMoveItem = { id, tier, pos -> moved = Triple(id, tier, pos) })
        val sourceTag = TierDetailTestTags.tile(1)
        val target = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).fetchSemanticsNode().boundsInRoot
        val sourceBounds = composeRule.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
        val start = Offset(sourceBounds.width / 2f, sourceBounds.height / 2f)

        composeRule.onNodeWithTag(sourceTag).performTouchInput {
            down(start)
            advanceEventTime(16)
            moveBy(Offset(60f, 0f))
            advanceEventTime(16)
            moveTo(Offset(target.center.x, target.top + target.height * 0.3f) - sourceBounds.topLeft)
            advanceEventTime(16)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(2L, moved?.second)
    }

    // Up or down is the board scrolling; the card stays where it is.
    @Test
    fun aVerticalMove_doesNotLiftTheCard() {
        var moves = 0
        setScreen(TierDetailUiState.Success(defaultList()), onMoveItem = { _, _, _ -> moves++ })
        val sourceTag = TierDetailTestTags.tile(1)
        val sourceBounds = composeRule.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
        val start = Offset(sourceBounds.width / 2f, sourceBounds.height / 2f)

        composeRule.onNodeWithTag(sourceTag).performTouchInput {
            down(start)
            advanceEventTime(16)
            moveBy(Offset(0f, 80f))
            advanceEventTime(16)
            moveBy(Offset(0f, 200f))
            advanceEventTime(16)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(0, moves)
        composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE).assertDoesNotExist()
    }

    @Test
    fun dragPoolItem_intoRankedTier_movesToRequestedTierAndIndex() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTile(sourceTag = TierDetailTestTags.tile(100), targetTag = TierDetailTestTags.tierRow(1), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(100L, 1L, 0), moved) }
    }

    @Test
    fun dragRankedItem_intoPool_movesToRequestedTierAndIndex() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(200, "Arrival"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTile(sourceTag = TierDetailTestTags.tile(200), targetTag = TierDetailTestTags.POOL_PANEL, horizontalBias = 0.5f)

        composeRule.runOnIdle { assertEquals(Triple(200L, 6L, 0), moved) }
    }

    @Test
    fun dragItem_betweenTwoRankedTiers_movesToRequestedTierAndIndex() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(300, "Dune"))),
            tier(id = 2, label = "A", items = emptyList()),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTile(sourceTag = TierDetailTestTags.tile(300), targetTag = TierDetailTestTags.tierRow(2), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(300L, 2L, 0), moved) }
    }

    @Test
    fun dragItem_withinSameTier_leftToRight_landsOnRequestedIndex() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(401, "First"), item(402, "Second"))),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTile(sourceTag = TierDetailTestTags.tile(401), targetTag = TierDetailTestTags.tierItems(1), horizontalBias = 0.9f)

        composeRule.runOnIdle { assertEquals(Triple(401L, 1L, 1), moved) }
    }

    @Test
    fun dragItem_withinSameTier_rightToLeft_landsOnRequestedIndex() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(401, "First"), item(402, "Second"))),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTileOntoLeadingHalf(sourceTag = TierDetailTestTags.tile(402), targetTag = TierDetailTestTags.tile(401))

        composeRule.runOnIdle { assertEquals(Triple(402L, 1L, 0), moved) }
    }

    @Test
    fun dragItem_ontoTileInSecondLine_landsOnReadingOrderIndexNotWithinLineIndex() {
        val rankedItems = List(20) { index -> item((1000 + index).toLong(), "M$index") }
        val list = listOf(
            tier(id = 1, label = "S", items = rankedItems),
            tier(id = 6, label = "Pool", items = listOf(item(2000, "NewItem")), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        val secondLineAnchor = secondLineFirstItemId(rankedItems)
        val expectedIndex = rankedItems.indexOfFirst { it.id == secondLineAnchor }

        dragTile(sourceTag = TierDetailTestTags.tile(2000), targetTag = TierDetailTestTags.tile(secondLineAnchor), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(2000L, 1L, expectedIndex), moved) }
    }

    @Test
    fun dragItem_withinSameTier_leftToRight_acrossLineWrap_landsOnReadingOrderIndex() {
        val items = List(20) { index -> item((3000 + index).toLong(), "M$index") }
        val list = listOf(
            tier(id = 1, label = "S", items = items),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        val draggedItemId = items.first().id
        val secondLineAnchor = secondLineFirstItemId(items)
        val anchorIndex = items.indexOfFirst { it.id == secondLineAnchor }
        val expectedIndex = anchorIndex - 1

        dragTile(sourceTag = TierDetailTestTags.tile(draggedItemId), targetTag = TierDetailTestTags.tile(secondLineAnchor), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(draggedItemId, 1L, expectedIndex), moved) }
    }

    @Test
    fun dragItem_droppedOutsideAllRows_doesNotInvokeOnMoveItem() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(500, "Enemy"))),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTileToRoot(sourceTag = TierDetailTestTags.tile(500), rootTarget = Offset(20f, 20f))

        composeRule.runOnIdle { assertNull(moved) }
    }

    @Test
    fun draggingNearTheBottomEdge_scrollsTheList_untilAnOffscreenTierIsReachable() {
        val manyTiers = (1..20).map { id -> tier(id = id.toLong(), label = "T$id", items = emptyList()) }
        val list = (manyTiers + tier(id = 100, label = "Pool", items = listOf(item(500, "Interstellar")), isPool = true))
            .asTierList()
        var moved: Pair<Long, Long>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, _ -> moved = itemId to toTierId })

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(20)).assertDoesNotExist()
        val lastVisibleIdBeforeScrolling = (1L..20L).last { id ->
            composeRule.onAllNodesWithTag(TierDetailTestTags.tierRow(id))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        val sourceBounds = composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).fetchSemanticsNode().boundsInRoot
        val start = Offset(sourceBounds.width / 2f, sourceBounds.height / 2f)
        val rowOneBounds = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot
        val poolBounds = composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).fetchSemanticsNode().boundsInRoot
        val edgeTarget = Offset(rowOneBounds.center.x, poolBounds.top - 10f)

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).performTouchInput {
                down(start)
                advanceEventTime(600)
                moveTo(start + Offset(2f, 0f))
                advanceEventTime(20)
                moveTo(edgeTarget - sourceBounds.topLeft)
                advanceEventTime(20)
            }
            composeRule.mainClock.advanceTimeBy(15000)

            composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).performTouchInput { up() }
            composeRule.mainClock.advanceTimeBy(500)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(500L, moved?.first)
            assertTrue(
                "expected a drop into a tier that was off-screen before scrolling (> $lastVisibleIdBeforeScrolling), landed on ${moved?.second}",
                (moved?.second ?: -1L) > lastVisibleIdBeforeScrolling,
            )
        }
    }

    @Test
    fun autoscrollStops_oncePointerLeavesTheEdgeZone() {
        val manyTiers = (1..20).map { id -> tier(id = id.toLong(), label = "T$id", items = emptyList()) }
        val list = (manyTiers + tier(id = 100, label = "Pool", items = listOf(item(500, "Interstellar")), isPool = true))
            .asTierList()
        setScreen(TierDetailUiState.Success(list))

        val sourceBounds = composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).fetchSemanticsNode().boundsInRoot
        val start = Offset(sourceBounds.width / 2f, sourceBounds.height / 2f)
        val rowOneBounds = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot
        val poolBounds = composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).fetchSemanticsNode().boundsInRoot
        val edgeTarget = Offset(rowOneBounds.center.x, poolBounds.top - 10f)
        val midTarget = Offset(rowOneBounds.center.x, (rowOneBounds.top + poolBounds.top) / 2f)
        val allIds = (1L..20L).toList()

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).performTouchInput {
                down(start)
                advanceEventTime(600)
                moveTo(start + Offset(2f, 0f))
                advanceEventTime(20)
                moveTo(edgeTarget - sourceBounds.topLeft)
                advanceEventTime(20)
            }
            composeRule.mainClock.advanceTimeBy(1000)

            composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).performTouchInput {
                moveTo(midTarget - sourceBounds.topLeft)
            }
            composeRule.mainClock.advanceTimeBy(50)
            val tierAtMidBefore = tierIdAtPoint(midTarget, allIds)

            composeRule.mainClock.advanceTimeBy(1500)
            val tierAtMidAfter = tierIdAtPoint(midTarget, allIds)

            assertEquals("scrolling must stop once the pointer leaves the edge zone", tierAtMidBefore, tierAtMidAfter)

            composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).performTouchInput { cancel() }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
        composeRule.waitForIdle()
    }
}
