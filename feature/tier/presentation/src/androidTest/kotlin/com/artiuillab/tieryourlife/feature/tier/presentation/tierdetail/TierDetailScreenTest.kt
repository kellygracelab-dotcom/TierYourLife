package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun successState_tierWithManyItems_wrapsToNewLinesAndGrowsTaller() {
        // Titles are short so the tile's fallback label (title.take(6).uppercase())
        // stays unique and predictable instead of being truncated into "MOVIE ".
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
            tier(id = 1, label = "S", items = List(3) { movie(it) }),
        ).asTierList()

        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1L)).assertHeightIsEqualTo(84.dp)
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

    @Test
    fun dragPoolItem_intoRankedTier_movesToRequestedTierAndIndex() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(100, "Interstellar")), isPool = true),
        ).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        setScreen(TierDetailUiState.Success(list), onMoveItem = { itemId, toTierId, toPosition -> moved = Triple(itemId, toTierId, toPosition) })

        // Target the whole row, not the (empty, degenerate-bounds) items area directly —
        // same reason as the empty-pool case below: an empty FlowRow reports a zero-size
        // rect from fetchSemanticsNode, which isn't a reliable point to aim a test at.
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

        // Target the whole pool panel, not the (empty, zero-height) items row directly: an
        // empty LazyRow reports degenerate bounds, which is harmless for the real hover
        // hit-test (it uses the panel's own bounds) but not a reliable point to aim a test at.
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

        // Target the whole row; see the pool-item-into-ranked-tier test above for why an
        // empty items area isn't a reliable point to aim at.
        dragTile(sourceTag = TierDetailTestTags.tile(300), targetTag = TierDetailTestTags.tierRow(2), horizontalBias = 0.1f)

        composeRule.runOnIdle { assertEquals(Triple(300L, 2L, 0), moved) }
    }

    @Test
    fun dragItem_withinSameTier_leftToRight_landsOnRequestedIndex() {
        // Regression for the off-by-one that only shows up moving forward: toPosition is an
        // index into the row WITHOUT the dragged item, so dropping item 401 past item 402's
        // midpoint must land it at index 1 (right after 402), not index 2 or back at index 0.
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

        dragTile(sourceTag = TierDetailTestTags.tile(402), targetTag = TierDetailTestTags.tierItems(1), horizontalBias = 0.05f)

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

        // Dropped from the pool, so nothing in the S tier is excluded from indexing:
        // the expected position is exactly the anchor tile's own reading-order index.
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
        // The dragged item (index 0) is excluded from the target list, so every raw
        // index past it shifts back by one.
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

        // The top bar sits above every tier row and the pool panel, so it is never a valid drop target.
        dragTileToRoot(sourceTag = TierDetailTestTags.tile(500), rootTarget = Offset(20f, 20f))

        composeRule.runOnIdle { assertNull(moved) }
    }

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

        // The current tier's own row isn't a destination: tapping it must not trigger a move.
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

        // Tier A already has two items, so the poster is appended at index 2 (the end).
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

        // This list's pool already holds one item, so the poster lands at index 1.
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

        // The top bar sits above every tier row, the pool panel, and the trash, so it's never a valid drop target.
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

        // Trigger the snackbar, then drag a different tile out of the pool — right where
        // the message now floats — while it's still up.
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

        // Same message, same action, regardless of which of the two delete paths fired it.
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

    // Line capacity depends on the test device's actual screen width, so this reads
    // the real rendered geometry instead of assuming how many items fit per line.
    private fun secondLineFirstItemId(items: List<TierItem>): Long {
        val boundsById = items.associate { it.id to tileBounds(it.id) }
        val lines = boundsById.entries.groupBy { it.value.top }.toSortedMap().values.toList()
        assertTrue("expected the tier to wrap to at least two lines", lines.size >= 2)
        return lines[1].minBy { it.value.left }.key
    }

    private fun tileBounds(itemId: Long): Rect =
        composeRule.onNodeWithTag(TierDetailTestTags.tile(itemId)).fetchSemanticsNode().boundsInRoot

    private fun dragTile(sourceTag: String, targetTag: String, horizontalBias: Float) {
        val targetBounds = composeRule.onNodeWithTag(targetTag).fetchSemanticsNode().boundsInRoot
        // An empty items row (e.g. an empty pool) has zero height, unlike a tier row whose
        // outer height is fixed regardless of content. Fall back to a small offset from the
        // top so the drop point still lands unambiguously inside it instead of exactly on its
        // degenerate (zero-height) edge. When targeting a tile, biasing above its exact
        // vertical centre (rather than sitting exactly on it) keeps the point strictly inside
        // that tile's own line instead of exactly on the line-vs-line boundary.
        val verticalOffset = if (targetBounds.height > 0f) targetBounds.height * 0.3f else 8f
        val target = Offset(
            x = targetBounds.left + targetBounds.width * horizontalBias,
            y = targetBounds.top + verticalOffset,
        )
        dragTileToRoot(sourceTag, target)
    }

    private fun dragTileToRoot(sourceTag: String, rootTarget: Offset) {
        val sourceBounds = composeRule.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
        val start = Offset(sourceBounds.width / 2f, sourceBounds.height / 2f)
        val end = rootTarget - sourceBounds.topLeft

        composeRule.onNodeWithTag(sourceTag).performTouchInput {
            down(start)
            advanceEventTime(600)
            moveTo(start + Offset(2f, 0f))
            advanceEventTime(20)
            moveTo(end)
            advanceEventTime(20)
            up()
        }
        composeRule.waitForIdle()
    }

    // Starts a drag and leaves the pointer down, without picking a drop target yet. The
    // trash only enters the tree once dragging starts, so its own bounds aren't known
    // until after this — and after a recomposition, hence the separate performTouchInput
    // call below rather than folding the nudge into a single block like dragTileToRoot.
    private fun beginDrag(sourceTag: String) {
        val sourceBounds = composeRule.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
        val start = Offset(sourceBounds.width / 2f, sourceBounds.height / 2f)
        composeRule.onNodeWithTag(sourceTag).performTouchInput {
            down(start)
            advanceEventTime(600)
            moveTo(start + Offset(2f, 0f))
            advanceEventTime(20)
        }
        composeRule.waitForIdle()
    }

    private fun cancelDrag(sourceTag: String) {
        composeRule.onNodeWithTag(sourceTag).performTouchInput { cancel() }
        composeRule.waitForIdle()
    }

    private fun dragTileIntoTrash(sourceTag: String) {
        beginDrag(sourceTag)

        val sourceBounds = composeRule.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
        val trashBounds = composeRule.onNodeWithTag(TierDetailTestTags.TRASH_TARGET).fetchSemanticsNode().boundsInRoot
        val trashCenterInRoot = Offset(trashBounds.left + trashBounds.width / 2f, trashBounds.top + trashBounds.height / 2f)
        val end = trashCenterInRoot - sourceBounds.topLeft

        composeRule.onNodeWithTag(sourceTag).performTouchInput {
            moveTo(end)
            advanceEventTime(20)
            up()
        }
        composeRule.waitForIdle()
    }

    private fun item(id: Long, title: String): TierItem = TierItem(id = id, title = title, imageUrl = null)

    private fun List<Tier>.asTierList(): TierList = TierList(id = 1, title = "Sci-fi films", tiers = this)

    private fun setScreen(
        state: TierDetailUiState,
        onBack: () -> Unit = {},
        onAddClick: () -> Unit = {},
        onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
        onDeleteItem: (itemId: Long) -> Unit = {},
        onRestoreItem: (itemId: Long) -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = state,
                    onBack = onBack,
                    onAddClick = onAddClick,
                    onMoveItem = onMoveItem,
                    onDeleteItem = onDeleteItem,
                    onRestoreItem = onRestoreItem,
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
