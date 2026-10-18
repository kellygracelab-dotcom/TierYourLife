package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDetailMoveSheetTest : TierDetailScreenTestBase() {

    @Test
    fun doubleTapTile_inRankedTier_opensChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }

        composeRule.onNodeWithText("Move to a tier").assertIsDisplayed()
    }

    @Test
    fun doubleTapTile_inPool_opensSameChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }

        composeRule.onNodeWithText("Move to a tier").assertIsDisplayed()
    }

    @Test
    fun moveSheet_currentTier_isMarkedAndNotSelectableAsDestination() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 2, label = "A", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }

        composeRule.onNodeWithText("Currently here").assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.moveSheetTierOption(1)).performClick()

        composeRule.runOnIdle { assertNull(moved) }
    }

    @Test
    fun moveSheet_selectingAnotherTier_invokesMoveWithCorrectIdAndTier() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 2, label = "A", items = listOf(item(201, "Dune"), item(202, "Arrival"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.moveSheetTierOption(2)).performClick()

        composeRule.runOnIdle { assertEquals(Triple(100L, 2L, 2), moved) }
    }

    @Test
    fun moveSheet_backToPool_invokesMoveIntoThisListsPool() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = listOf(item(900, "Existing")), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_POOL).performClick()

        composeRule.runOnIdle { assertEquals(Triple(100L, 6L, 1), moved) }
    }

    @Test
    fun moveSheet_remove_invokesDeleteExactlyOnce() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        val deleted = mutableListOf<Long>()
        setScreen(TierDetailUiState.Success(list), onDeleteItem = { deleted += it })

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_REMOVE).performClick()

        composeRule.runOnIdle { assertEquals(listOf(100L), deleted) }
    }

    @Test
    fun longPress_onTile_stillStartsDraggingNotChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTile(sourceTag = TierDetailTestTags.tile(100), targetTag = TierDetailTestTags.tierRow(1), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(100L, 1L, 0), moved) }
        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET).assertDoesNotExist()
    }
}
