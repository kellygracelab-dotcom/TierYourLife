package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDetailTrashTest : TierDetailScreenTestBase() {

    @Test
    fun trashTarget_atRest_isNotOnScreen() {
        setScreen(TierDetailUiState.Success(defaultList()))

        composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).assertDoesNotExist()
    }

    @Test
    fun trashTarget_whileDraggingFromRankedTier_appears() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        beginDrag(TierDetailTestTags.tile(100))

        composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).assertIsDisplayed()

        cancelDrag(TierDetailTestTags.tile(100))
    }

    @Test
    fun trashTarget_whileDraggingFromPool_appears() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        beginDrag(TierDetailTestTags.tile(100))

        composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).assertIsDisplayed()

        cancelDrag(TierDetailTestTags.tile(100))
    }

    @Test
    fun dropInTrash_fromRankedTier_deletesItemExactlyOnce() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        val deleted = mutableListOf<Long>()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(
            TierDetailUiState.Success(list),
            onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) },
            onDeleteItem = { deleted += it },
        )

        dragTileIntoTrash(TierDetailTestTags.tile(100))

        composeRule.runOnIdle {
            assertEquals(listOf(100L), deleted)
            assertNull(moved)
        }
    }

    @Test
    fun dropInTrash_fromPool_deletesItemExactlyOnce() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        val deleted = mutableListOf<Long>()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(
            TierDetailUiState.Success(list),
            onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) },
            onDeleteItem = { deleted += it },
        )

        dragTileIntoTrash(TierDetailTestTags.tile(100))

        composeRule.runOnIdle {
            assertEquals(listOf(100L), deleted)
            assertNull(moved)
        }
    }

    @Test
    fun dropInRow_stillMovesNotDeletes() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        val deleted = mutableListOf<Long>()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(
            TierDetailUiState.Success(list),
            onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) },
            onDeleteItem = { deleted += it },
        )

        dragTile(sourceTag = TierDetailTestTags.tile(100), targetTag = TierDetailTestTags.tierRow(1), horizontalBias = 0.1f)

        composeRule.runOnIdle {
            assertEquals(Triple(100L, 1L, 0), moved)
            assertTrue(deleted.isEmpty())
        }
    }

    @Test
    fun dropOutsideAllTargets_invokesNeitherMoveNorDelete() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(500, "Enemy"))),
        ).asTierList()
        val deleted = mutableListOf<Long>()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(
            TierDetailUiState.Success(list),
            onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) },
            onDeleteItem = { deleted += it },
        )

        dragTileToRoot(sourceTag = TierDetailTestTags.tile(500), rootTarget = Offset(20f, 20f))

        composeRule.runOnIdle {
            assertNull(moved)
            assertTrue(deleted.isEmpty())
        }
    }

    @Test
    fun dragCancelled_trashDisappears() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        beginDrag(TierDetailTestTags.tile(100))
        composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).assertIsDisplayed()

        cancelDrag(TierDetailTestTags.tile(100))

        composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).assertDoesNotExist()
    }

    @Test
    fun dropInTrash_showsDeletedMessage() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        dragTileIntoTrash(TierDetailTestTags.tile(100))

        composeRule.onNodeWithText("Interstellar moved to the trash").assertIsDisplayed()
    }

    @Test
    fun deletedItemSnackbar_doesNotOverlapPoolPanel() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        dragTileIntoTrash(TierDetailTestTags.tile(100))

        val snackbarBounds = composeRule.onNodeWithTag(TierDetailTestTags.DELETED_ITEM_SNACKBAR).fetchSemanticsNode().boundsInRoot
        val poolBounds = composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).fetchSemanticsNode().boundsInRoot
        val overlapsVertically = snackbarBounds.top < poolBounds.bottom && snackbarBounds.bottom > poolBounds.top
        val overlapsHorizontally = snackbarBounds.left < poolBounds.right && snackbarBounds.right > poolBounds.left

        assertTrue(
            "snackbar $snackbarBounds must not overlap pool $poolBounds",
            !(overlapsVertically && overlapsHorizontally),
        )
    }

    @Test
    fun dragWhileSnackbarVisible_stillCompletesTheMove() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar"), item(200, "Arrival")), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        dragTileIntoTrash(TierDetailTestTags.tile(100))
        composeRule.onNodeWithTag(TierDetailTestTags.DELETED_ITEM_SNACKBAR).assertIsDisplayed()

        dragTile(sourceTag = TierDetailTestTags.tile(200), targetTag = TierDetailTestTags.tierRow(1), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(200L, 1L, 0), moved) }
    }

    @Test
    fun doubleTapWhileSnackbarVisible_stillOpensChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar"), item(200, "Arrival")), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        dragTileIntoTrash(TierDetailTestTags.tile(100))
        composeRule.onNodeWithTag(TierDetailTestTags.DELETED_ITEM_SNACKBAR).assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.tile(200)).performTouchInput { doubleClick() }

        composeRule.onNodeWithText("Move to a tier").assertIsDisplayed()
    }

    @Test
    fun rapidSuccessiveDeletions_showOnlyLatestMessage_notQueued() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"), item(101, "Arrival"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        dragTileIntoTrash(TierDetailTestTags.tile(100))
        composeRule.onNodeWithText("Interstellar moved to the trash").assertIsDisplayed()

        dragTileIntoTrash(TierDetailTestTags.tile(101))

        composeRule.onNodeWithText("Arrival moved to the trash").assertIsDisplayed()
        composeRule.onNodeWithText("Interstellar moved to the trash").assertDoesNotExist()
    }

    @Test
    fun undoAction_afterDropInTrash_restoresSameItem() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        val restored = mutableListOf<Long>()
        setScreen(TierDetailUiState.Success(list), onRestoreItem = { restored += it })

        dragTileIntoTrash(TierDetailTestTags.tile(100))
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.runOnIdle { assertEquals(listOf(100L), restored) }
    }

    @Test
    fun removeFromMoveSheet_showsSameMessageAndIsUndoable() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        val restored = mutableListOf<Long>()
        setScreen(TierDetailUiState.Success(list), onRestoreItem = { restored += it })

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_REMOVE).performClick()

        composeRule.onNodeWithText("Interstellar moved to the trash").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.runOnIdle { assertEquals(listOf(100L), restored) }
    }

    @Test
    fun deletedItem_withoutPressingUndo_staysDeleted() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        val restored = mutableListOf<Long>()
        setScreen(TierDetailUiState.Success(list), onRestoreItem = { restored += it })

        dragTileIntoTrash(TierDetailTestTags.tile(100))

        composeRule.runOnIdle { assertTrue(restored.isEmpty()) }
    }

    @Test
    fun trashTarget_doesNotOverlapPoolPanel() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        beginDrag(TierDetailTestTags.tile(100))

        val trashBounds = composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).fetchSemanticsNode().boundsInRoot
        val poolBounds = composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).fetchSemanticsNode().boundsInRoot
        val overlapsVertically = trashBounds.top < poolBounds.bottom && trashBounds.bottom > poolBounds.top
        val overlapsHorizontally = trashBounds.left < poolBounds.right && trashBounds.right > poolBounds.left

        assertTrue(
            "trash $trashBounds must not overlap pool $poolBounds",
            !(overlapsVertically && overlapsHorizontally),
        )

        cancelDrag(TierDetailTestTags.tile(100))
    }
}
