package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.TierRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun newUntouchedList_showsADiscardCross_notTheBackArrow() {
        setScreen(TierDetailUiState.Success(defaultList()), canDiscard = true)

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_discard),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_back),
        ).assertDoesNotExist()
    }

    @Test
    fun touchedList_showsTheBackArrow_notTheDiscardCross() {
        setScreen(TierDetailUiState.Success(defaultList()), canDiscard = false)

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_back),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_discard),
        ).assertDoesNotExist()
    }

    @Test
    fun discardCross_clickInvokesOnDiscardOnce_notOnBack() {
        var discardCalls = 0
        var backCalls = 0
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onBack = { backCalls++ },
            canDiscard = true,
            onDiscard = { discardCalls++ },
        )

        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_discard),
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(1, discardCalls)
            assertEquals(0, backCalls)
        }
    }

    // The first keystroke has to flip the latch even though a cleared field never
    // calls onRenameList at all — otherwise typing then deleting a character would
    // leave the cross showing, which the spec explicitly rules out.
    @Test
    fun typingInTheTitleField_touchesTheListOnTheFirstKeystroke_evenIfClearedAfterward() {
        var touchedCalls = 0
        setScreen(
            TierDetailUiState.Success(defaultList()),
            startInTitleEdit = true,
            canDiscard = true,
            onTitleEditStarted = { touchedCalls++ },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performTextInput("X")
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performTextClearance()

        composeRule.runOnIdle { assertTrue(touchedCalls > 0) }
    }

    // onValueChange also fires when only the selection moves, so without the text
    // comparison guarding it, putting a caret in the field would count as touching the
    // list — the same non-event as opening the search sheet and adding nothing, which
    // the spec says explicitly does not count (docs/design-spec-home.md, section 12).
    @Test
    fun movingTheCaretInTheTitleField_withoutTyping_doesNotTouchTheList() {
        var touchedCalls = 0
        setScreen(
            TierDetailUiState.Success(defaultList()),
            startInTitleEdit = true,
            canDiscard = true,
            onTitleEditStarted = { touchedCalls++ },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE)
            .performTextInputSelection(TextRange(0))

        composeRule.runOnIdle { assertEquals(0, touchedCalls) }
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
    fun liftingATierRow_collapsesAllRankedRowsToOneHeight() {
        val list = listOf(
            tier(id = 1, label = "S", items = List(24) { tierItem(it) }),
            tier(id = 2, label = "A", items = List(1) { tierItem(it + 100) }),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        val before1 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height
        val before2 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).fetchSemanticsNode().boundsInRoot.height
        assertNotEquals("sanity: S (24 items) must be taller than A (1 item) before collapsing", before1, before2)

        beginDrag(TierDetailTestTags.tierBand(1))

        val during1 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height
        val during2 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).fetchSemanticsNode().boundsInRoot.height
        assertEquals(during1, during2, 0.5f)

        cancelDrag(TierDetailTestTags.tierBand(1))
    }

    @Test
    fun releasingATierRow_expandsRowsBackToTheirNormalHeight() {
        val list = listOf(
            tier(id = 1, label = "S", items = List(24) { tierItem(it) }),
            tier(id = 2, label = "A", items = List(1) { tierItem(it + 100) }),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))
        val expandedHeight = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height

        beginDrag(TierDetailTestTags.tierBand(1))
        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { up() }
        composeRule.waitForIdle()

        val afterHeight = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height
        assertEquals(expandedHeight, afterHeight, 0.5f)
    }

    @Test
    fun cancellingATierRowDrag_expandsRowsBackToTheirNormalHeight() {
        val list = listOf(
            tier(id = 1, label = "S", items = List(24) { tierItem(it) }),
            tier(id = 2, label = "A", items = List(1) { tierItem(it + 100) }),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))
        val expandedHeight = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height

        beginDrag(TierDetailTestTags.tierBand(1))
        cancelDrag(TierDetailTestTags.tierBand(1))

        val afterHeight = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height
        assertEquals(expandedHeight, afterHeight, 0.5f)
    }

    @Test
    fun draggingATierRow_reorders_andSubmitsTheFinalSequenceInOneCall() {
        var callCount = 0
        var reorderedIds: List<Long>? = null
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 2, label = "A", items = emptyList()),
            tier(id = 3, label = "B", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onReorderTiers = { ids ->
                callCount++
                reorderedIds = ids
            },
        )

        dragTile(TierDetailTestTags.tierBand(1), TierDetailTestTags.tierRow(3), horizontalBias = 0.5f)

        composeRule.runOnIdle {
            assertEquals(1, callCount)
            assertEquals(listOf(2L, 1L, 3L), reorderedIds)
        }
    }

    @Test
    fun pool_staysAtTheBottom_doesNotReorder_andDoesNotCollapse() {
        var reorderedIds: List<Long>? = null
        val list = listOf(
            tier(id = 1, label = "S", items = List(24) { tierItem(it) }),
            tier(id = 2, label = "A", items = emptyList()),
            tier(id = 6, label = "Pool", items = List(2) { tierItem(it + 100) }, isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onReorderTiers = { ids -> reorderedIds = ids },
        )

        beginDrag(TierDetailTestTags.tierBand(1))

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertIsDisplayed()
        val rankedBottom = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).fetchSemanticsNode().boundsInRoot.bottom
        val poolTop = composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).fetchSemanticsNode().boundsInRoot.top
        assertTrue("pool must stay below every ranked row", poolTop >= rankedBottom)

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { up() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertFalse("the pool's id must never appear in a tier reorder", reorderedIds!!.contains(6L))
        }
    }

    @Test
    fun droppingATierRow_intoTheTrash_removesTheTier_andMovesItsItemsToThePool() {
        var deletedTierId: Long? = null
        var deletedPoolId: Long? = null
        var deletedPoolSize: Int? = null
        var deletedItemIds: List<Long>? = null
        val list = listOf(
            tier(id = 1, label = "S", items = List(2) { tierItem(it) }),
            tier(id = 2, label = "A", items = emptyList()),
            tier(id = 6, label = "Pool", items = List(3) { tierItem(it + 100) }, isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onDeleteTierToPool = { tierId, poolId, poolSize, itemIds ->
                deletedTierId = tierId
                deletedPoolId = poolId
                deletedPoolSize = poolSize
                deletedItemIds = itemIds
            },
        )

        dragTileIntoTrash(TierDetailTestTags.tierBand(1))

        composeRule.runOnIdle {
            assertEquals(1L, deletedTierId)
            assertEquals(6L, deletedPoolId)
            assertEquals(3, deletedPoolSize)
            assertEquals(listOf(0L, 1L), deletedItemIds)
        }
    }

    @Test
    fun undoingATierDeletion_recreatesTheTierWithItsFormerDetails_andReturnsItsItems() {
        var restoredLabel: String? = null
        var restoredCaption: String? = null
        var restoredColorLight: String? = null
        var restoredColorDark: String? = null
        var restoredPosition: Int? = null
        var restoredItemIds: List<Long>? = null
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "Best"),
            tier(
                id = 2,
                label = "A",
                items = List(1) { tierItem(it) },
                caption = "Great",
                colorLight = "#111111",
                colorDark = "#222222",
            ),
            tier(id = 3, label = "B", items = emptyList()),
            tier(id = 6, label = "Pool", items = List(2) { tierItem(it + 100) }, isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onRestoreTier = { label, caption, colorLight, colorDark, position, itemIds ->
                restoredLabel = label
                restoredCaption = caption
                restoredColorLight = colorLight
                restoredColorDark = colorDark
                restoredPosition = position
                restoredItemIds = itemIds
            },
        )

        dragTileIntoTrash(TierDetailTestTags.tierBand(2))
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.runOnIdle {
            assertEquals("A", restoredLabel)
            assertEquals("Great", restoredCaption)
            assertEquals("#111111", restoredColorLight)
            assertEquals("#222222", restoredColorDark)
            assertEquals(1, restoredPosition)
            assertEquals(listOf(0L), restoredItemIds)
        }
    }

    // Regression for a stale drag-target rectangle: S starts big enough that, once it's
    // gone, A and B shift up into space its own (unremoved) ghost rect would otherwise
    // still claim — the defect this task fixes doesn't show up on a small ghost. Wires
    // onDeleteTierToPool back into local state (unlike the other tier-drag tests above,
    // which only capture the callback) because the bug only exists once the row has
    // actually left composition, not merely once a callback fired.
    @Test
    fun afterDeletingALargeTier_droppingATile_landsInEachRemainingRow_notInTheDeletedTiersGhost() {
        var moved: Pair<Long, Long>? = null
        composeRule.setContent {
            var list by remember {
                mutableStateOf(
                    listOf(
                        tier(id = 1, label = "S", items = List(24) { tierItem(it) }),
                        tier(id = 2, label = "A", items = emptyList()),
                        tier(id = 3, label = "B", items = emptyList()),
                        tier(id = 6, label = "Pool", items = List(1) { tierItem(it + 100) }, isPool = true),
                    ).asTierList(),
                )
            }
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    onBack = {},
                    onAddClick = {},
                    onMoveItem = { itemId, toTierId, _ -> moved = itemId to toTierId },
                    onDeleteTierToPool = { tierId, poolId, _, _ ->
                        val removedTier = list.tiers.first { it.id == tierId }
                        list = list.copy(
                            tiers = list.tiers
                                .filterNot { it.id == tierId }
                                .map { t -> if (t.id == poolId) t.copy(items = t.items + removedTier.items) else t },
                        )
                    },
                )
            }
        }

        dragTileIntoTrash(TierDetailTestTags.tierBand(1))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).assertDoesNotExist()

        dragTile(TierDetailTestTags.tile(100), TierDetailTestTags.tierRow(2), horizontalBias = 0.5f)
        composeRule.runOnIdle { assertEquals(100L to 2L, moved) }

        dragTile(TierDetailTestTags.tile(100), TierDetailTestTags.tierRow(3), horizontalBias = 0.5f)
        composeRule.runOnIdle { assertEquals(100L to 3L, moved) }
    }

    // Same class of regression, at tile scale: deleting the middle tile of a row would
    // otherwise leave a ghost entry that skews insertionIndexInGrid's grouping for a
    // later drop into that exact row.
    @Test
    fun afterDeletingATile_insertionIndexInItsFormerRow_staysCorrect() {
        var lastPosition: Int? = null
        composeRule.setContent {
            var list by remember {
                mutableStateOf(
                    listOf(
                        tier(id = 1, label = "S", items = listOf(item(100, "A"), item(101, "B"), item(102, "C"))),
                        tier(id = 6, label = "Pool", items = List(1) { tierItem(it + 200) }, isPool = true),
                    ).asTierList(),
                )
            }
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    onBack = {},
                    onAddClick = {},
                    onMoveItem = { _, _, toPosition -> lastPosition = toPosition },
                    onDeleteItem = { deletedId ->
                        list = list.copy(
                            tiers = list.tiers.map { t -> t.copy(items = t.items.filterNot { it.id == deletedId }) },
                        )
                    },
                )
            }
        }

        dragTileIntoTrash(TierDetailTestTags.tile(101))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(101)).assertDoesNotExist()

        // Row S now holds just [100, 102]. Dropping onto 102's left edge must land right
        // before it, at index 1 — not shifted by a stale ghost of 101's old bounds.
        dragTile(TierDetailTestTags.tile(200), TierDetailTestTags.tile(102), horizontalBias = 0.0f)

        composeRule.runOnIdle { assertEquals(1, lastPosition) }
    }

    @Test
    fun tileDragAndBandDoubleTap_stillWorkAlongsideTheNewRowDrag() {
        var movedItemId: Long? = null
        val list = listOf(
            tier(id = 1, label = "S", items = List(1) { tierItem(it) }, caption = "Masterpiece"),
            tier(id = 2, label = "A", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onMoveItem = { itemId, _, _ -> movedItemId = itemId },
        )

        dragTile(TierDetailTestTags.tile(0), TierDetailTestTags.tierRow(2), horizontalBias = 0.5f)
        composeRule.runOnIdle { assertEquals(0L, movedItemId) }

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertIsDisplayed()
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

    @Test
    fun listSettings_opensFromMoreButton() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openListSettings()

        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).assertIsDisplayed()
    }

    @Test
    fun manualAddButton_opensTheManualEntryDialog() {
        composeRule.setContent {
            var manualEntryVisible by remember { mutableStateOf(false) }
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(defaultList()),
                    onBack = {},
                    onAddClick = {},
                    onManualAddClick = { manualEntryVisible = true },
                )
                if (manualEntryVisible) {
                    ManualEntryDialog(
                        onDismiss = { manualEntryVisible = false },
                        onSave = { _, _ -> },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_DIALOG).assertIsDisplayed()
    }

    @Test
    fun tierEditor_opensFromListSettings() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openTierEditor()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertIsDisplayed()
    }

    @Test
    fun tierEditorTitle_inAddMode_readsAddTier() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openTierEditor()

        composeRule.onNodeWithText("Add tier").assertIsDisplayed()
    }

    @Test
    fun tierEditorTitle_inEditMode_readsEditTier() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }

        composeRule.onNodeWithText("Edit tier").assertIsDisplayed()
    }

    @Test
    fun listSettings_marksTheListsCurrentDisplayModeSelected() {
        val list = listOf(tier(id = 1, label = "S", items = List(1) { tierItem(it) }))
            .asTierList(displayMode = TierListDisplayMode.HORIZONTAL_SCROLL)
        setScreen(TierDetailUiState.Success(list))

        openListSettings()

        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_STRIP).assertIsSelected()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_WRAP).assertIsNotSelected()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_RANKED).assertIsNotSelected()
    }

    @Test
    fun selectingADisplayMode_savesExactlyOnceWithTheChosenValue() {
        var callCount = 0
        var savedMode: TierListDisplayMode? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onSetDisplayMode = { mode ->
                callCount++
                savedMode = mode
            },
        )

        openListSettings()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_STRIP).performClick()

        composeRule.runOnIdle {
            assertEquals(1, callCount)
            assertEquals(TierListDisplayMode.HORIZONTAL_SCROLL, savedMode)
        }
    }

    @Test
    fun tappingHeaderTitle_turnsItIntoAnEditableField() {
        setScreen(TierDetailUiState.Success(defaultList()))

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).assert(hasSetTextAction())
    }

    @Test
    fun editingHeaderTitle_savesTheNewTitleExactlyOnce() {
        var callCount = 0
        var savedTitle: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onRenameList = { title ->
                callCount++
                savedTitle = title
            },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performTextInput("Best sci-fi ever")
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performImeAction()

        composeRule.runOnIdle {
            assertEquals(1, callCount)
            assertEquals("Best sci-fi ever", savedTitle)
        }
    }

    @Test
    fun editingHeaderTitle_withBlankTitle_doesNotSaveAndKeepsTheOldTitle() {
        var callCount = 0
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onRenameList = { callCount++ },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performImeAction()

        composeRule.runOnIdle { assertEquals(0, callCount) }
        composeRule.onNodeWithText("Sci-fi films").assertIsDisplayed()
    }

    @Test
    fun listSettings_noLongerHasARenameRow() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openListSettings()

        composeRule.onNodeWithText("Rename list").assertDoesNotExist()
    }

    @Test
    fun listSettings_stillShowsDisplayModeOptionsAndNewTierRowAfterRenameRowRemoval() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openListSettings()

        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_WRAP).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_STRIP).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_RANKED).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).assertIsDisplayed()
    }

    @Test
    fun horizontalScrollMode_keepsAllItemsOnOneLineAndAllowsScrollingToTheLastOne() {
        val items = List(20) { index -> item((4000 + index).toLong(), "M$index") }
        val list = listOf(tier(id = 1, label = "S", items = items))
            .asTierList(displayMode = TierListDisplayMode.HORIZONTAL_SCROLL)
        setScreen(TierDetailUiState.Success(list))

        val firstTop = tileBounds(items.first().id).top

        composeRule.onNodeWithTag(TierDetailTestTags.tile(items.last().id)).performScrollTo().assertIsDisplayed()
        val lastTop = tileBounds(items.last().id).top

        assertEquals("expected the scrolled-to tile to stay on the same line", firstTop, lastTop, 0.5f)
    }

    @Test
    fun wrapMode_stillWrapsItemsAcrossMultipleLines() {
        val items = List(20) { index -> item((5000 + index).toLong(), "M$index") }
        val list = listOf(tier(id = 1, label = "S", items = items)).asTierList(displayMode = TierListDisplayMode.WRAP)
        setScreen(TierDetailUiState.Success(list))

        val tops = items.map { tileBounds(it.id).top }
        assertTrue("expected the tier to wrap to at least two lines", tops.distinct().size >= 2)
    }

    @Test
    fun flatRankedMode_showsNoTierRowsOnlyOneColumnOfItemRows() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 2, label = "A", items = listOf(item(101, "Arrival"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(100)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(101)).assertIsDisplayed()
    }

    @Test
    fun flatRankedMode_ordersRowsByTierPositionThenItemPositionWithinTier() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "First"), item(101, "Second"))),
            tier(id = 2, label = "A", items = listOf(item(102, "Third"))),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        val tops = listOf(100L, 101L, 102L).map { rankedRowBounds(it).top }
        assertTrue("expected rows top-to-bottom in S,S,A item order", tops[0] < tops[1] && tops[1] < tops[2])
    }

    @Test
    fun flatRankedMode_numbersRowsSequentiallyStartingAtOne() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "First"), item(101, "Second"))),
            tier(id = 2, label = "A", items = listOf(item(102, "Third"))),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithContentDescription("Rank 1, First, tier S.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rank 2, Second, tier S.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rank 3, Third, tier A.").assertIsDisplayed()
    }

    @Test
    fun flatRankedMode_tierBadgeMatchesTheItemsOwnTier() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "First"))),
            tier(id = 2, label = "A", items = listOf(item(101, "Second"))),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithContentDescription("Rank 1, First, tier S.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rank 2, Second, tier A.").assertIsDisplayed()
    }

    @Test
    fun flatRankedMode_showsPoolCollapsedAtTheEnd() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "First"))),
            tier(id = 6, label = "Pool", items = listOf(item(200, "Unranked")), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_COLLAPSED).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_ITEMS).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(200)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(200)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(200)).assertDoesNotExist()
    }

    @Test
    fun flatRankedPool_tapExpandsAndTapAgainCollapses() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "First"))),
            tier(id = 6, label = "Pool", items = listOf(item(200, "Unranked"), item(201, "AlsoUnranked")), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_COLLAPSED).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_ITEMS).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(200)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(201)).assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_COLLAPSED).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_ITEMS).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(200)).assertDoesNotExist()
    }

    @Test
    fun flatRankedPool_collapsedCounterMatchesExpandedItemCount() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(
                id = 6,
                label = "Pool",
                items = listOf(item(200, "One"), item(201, "Two"), item(202, "Three")),
                isPool = true,
            ),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithText("Pool · 3 unranked").assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_COLLAPSED).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(200)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(201)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(202)).assertIsDisplayed()
    }

    @Test
    fun tapRankedRow_opensChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(100)).performClick()

        composeRule.onNodeWithText("Move to a tier").assertIsDisplayed()
    }

    @Test
    fun tapExpandedPoolItem_opensSameChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = listOf(item(200, "Unranked")), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.FLAT_RANKED)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_POOL_COLLAPSED).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedPoolItem(200)).performClick()

        composeRule.onNodeWithText("Move to a tier").assertIsDisplayed()
    }

    @Test
    fun wrapMode_singleTapOnTile_doesNotOpenChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.WRAP)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performClick()

        composeRule.onNodeWithText("Move to a tier").assertDoesNotExist()
    }

    @Test
    fun horizontalScrollMode_singleTapOnTile_doesNotOpenChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.HORIZONTAL_SCROLL)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performClick()

        composeRule.onNodeWithText("Move to a tier").assertDoesNotExist()
    }

    @Test
    fun horizontalScrollMode_doubleTapOnTile_stillOpensChooser() {
        val list = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.HORIZONTAL_SCROLL)
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tile(100)).performTouchInput { doubleClick() }

        composeRule.onNodeWithText("Move to a tier").assertIsDisplayed()
    }

    @Test
    fun switchingToFlatRankedModeFromSettings_redrawsTheScreen() {
        val initialList = listOf(
            tier(id = 1, label = "S", items = listOf(item(100, "Interstellar"))),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList(displayMode = TierListDisplayMode.WRAP)

        composeRule.setContent {
            val listState = remember { mutableStateOf(initialList) }
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(listState.value),
                    onBack = {},
                    onAddClick = {},
                    onSetDisplayMode = { mode -> listState.value = listState.value.copy(displayMode = mode) },
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).assertIsDisplayed()

        openListSettings()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_RANKED).performClick()
        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_back),
        ).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(100)).assertIsDisplayed()
    }

    @Test
    fun addTier_withLabelAndCaption_returnsBothValuesAndBothColorHexValues() {
        var addedLabel: String? = null
        var addedCaption: String? = null
        var addedColorLight: String? = null
        var addedColorDark: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { label, caption, colorLight, colorDark ->
                addedLabel = label
                addedCaption = caption
                addedColorLight = colorLight
                addedColorDark = colorDark
            },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextInput("Zenith")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("Z", addedLabel)
            assertEquals("Zenith", addedCaption)
            assertEquals("#B03A32", addedColorLight)
            assertEquals("#F1948C", addedColorDark)
        }
    }

    @Test
    fun addTier_withEmptyCaption_returnsNullNotEmptyString() {
        var addedCaption: String? = "not yet called"
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, caption, _, _ -> addedCaption = caption },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle { assertNull(addedCaption) }
    }

    @Test
    fun selectingPreset_marksItSelectedAndItsColorsReachTheResult() {
        var addedColorLight: String? = null
        var addedColorDark: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, colorLight, colorDark ->
                addedColorLight = colorLight
                addedColorDark = colorDark
            },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(3)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(3)).assertIsSelected()
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(0)).assertIsNotSelected()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("#3F7F55", addedColorLight)
            assertEquals("#8FD3A3", addedColorDark)
        }
    }

    @Test
    fun customColor_appliesToItsOwnThemeOnly_withoutOverwritingTheOther() {
        var addedColorLight: String? = null
        var addedColorDark: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, colorLight, colorDark ->
                addedColorLight = colorLight
                addedColorDark = colorDark
            },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_TAB_DARK).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextInput("336699")

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("#7A3F8C", addedColorLight)
            assertEquals("#336699", addedColorDark)
            assertNotEquals(addedColorLight, addedColorDark)
        }
    }

    @Test
    fun contrastWarning_doesNotBlockSavingALowContrastTier() {
        var saved = false
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, _, _ -> saved = true },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HEX_FIELD).performTextInput("F5F0FA")

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CONTRAST_READOUT).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertTrue("low contrast must not block saving", saved) }
    }

    @Test
    fun draggingHueSlider_changesTheCustomColor() {
        var addedColorLight: String? = null
        setScreen(
            TierDetailUiState.Success(defaultList()),
            onAddTier = { _, _, colorLight, _ -> addedColorLight = colorLight },
        )

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("Z")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()

        // Revealing the custom panel pushes the sliders below the fold of the sheet's
        // scrolling content (TierEditorSheet.kt), so scroll the Hue slider into view first
        // — performTouchInput acts on a node's on-screen bounds and silently no-ops outside
        // them. The gesture also has to stay off the track's extreme edge: a touch at the
        // very first/last pixel falls just outside Slider's own hit-tested range and never
        // reaches onValueChange, confirmed by probing values from the track's edge inward.
        val hueSlider = composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER)
        hueSlider.performScrollTo()
        hueSlider.performTouchInput {
            down(Offset(width * 0.1f, center.y))
            advanceEventTime(20)
            moveTo(Offset(width * 0.9f, center.y))
            advanceEventTime(20)
            up()
        }

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertNotEquals("#7A3F8C", addedColorLight)
        }
    }

    @Test
    fun hueSliderThumb_isVerticallyCenteredOnItsTrack() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openTierEditor()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).performClick()

        // Slider merges its descendants' semantics into its own node, so the track/thumb
        // sub-tags are only visible via the unmerged tree.
        val trackTag = TierDetailTestTags.sliderTrack(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER)
        val thumbTag = TierDetailTestTags.sliderThumb(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER)
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_HUE_SLIDER).performScrollTo()

        val trackBounds = composeRule.onNodeWithTag(trackTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val thumbBounds = composeRule.onNodeWithTag(thumbTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val trackCenterY = (trackBounds.top + trackBounds.bottom) / 2f
        val thumbCenterY = (thumbBounds.top + thumbBounds.bottom) / 2f

        assertEquals(
            "the thumb's vertical center must land on the track's vertical center",
            trackCenterY,
            thumbCenterY,
            0.5f,
        )
    }

    @Test
    fun doubleTapTierBand_inRankedTier_opensEditorPrefilledWithItsValues() {
        val list = listOf(
            tier(
                id = 1,
                label = "S",
                items = emptyList(),
                caption = "Masterpiece",
                colorLight = "#111111",
                colorDark = "#222222",
            ),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).assertTextContains("S")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).assertTextContains("Masterpiece")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CUSTOM_SWATCH).assertIsSelected()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_LIGHT).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_PREVIEW_DARK).assertIsDisplayed()
    }

    // The pool never reaches TierRow through the real screen (rankedTiers filters it out
    // before rows are built), so this mounts TierRow directly with a pool-flagged tier to
    // exercise the guard itself, rather than a path production code never takes anyway.
    @Test
    fun doubleTapTierBand_onThePool_doesNotOpenEditor() {
        var editedId: Long? = null
        val pool = tier(id = 6, label = "Pool", items = emptyList(), isPool = true)

        composeRule.setContent {
            TierYourLifeTheme {
                val dragController = remember { TierDragController() }
                TierRow(
                    tier = pool,
                    displayMode = TierListDisplayMode.WRAP,
                    dragController = dragController,
                    rankedTierIds = emptyList(),
                    onMoveItem = { _, _, _ -> },
                    onDeleteItem = {},
                    onReorderTiers = {},
                    onDeleteTier = {},
                    onEditTier = { id -> editedId = id },
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(6)).performTouchInput { doubleClick() }

        composeRule.runOnIdle { assertNull(editedId) }
    }

    @Test
    fun editingTier_savesNewLabelAndCaptionExactlyOnce() {
        var callCount = 0
        var editedLabel: String? = null
        var editedCaption: String? = null
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "Old caption"),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, label, caption, _, _ ->
                callCount++
                editedLabel = label
                editedCaption = caption
            },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextInput("S+")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextInput("New caption")
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals(1, callCount)
            assertEquals("S+", editedLabel)
            assertEquals("New caption", editedCaption)
        }
    }

    @Test
    fun editingTier_savesBothColorValues() {
        var editedColorLight: String? = null
        var editedColorDark: String? = null
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, _, _, colorLight, colorDark ->
                editedColorLight = colorLight
                editedColorDark = colorDark
            },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(3)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("#3F7F55", editedColorLight)
            assertEquals("#8FD3A3", editedColorDark)
        }
    }

    @Test
    fun editingTier_withEmptyCaption_savesNullNotEmptyString() {
        var editedCaption: String? = "not yet called"
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList(), caption = "Has a caption"),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, _, caption, _, _ -> editedCaption = caption },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).performClick()

        composeRule.runOnIdle { assertNull(editedCaption) }
    }

    @Test
    fun editingTier_withEmptyLabel_doesNotSave() {
        var callCount = 0
        val list = listOf(
            tier(id = 1, label = "S", items = emptyList()),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onEditTier = { _, _, _, _, _ -> callCount++ },
        )

        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextClearance()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SAVE).assertHasNoClickAction()
        composeRule.runOnIdle { assertEquals(0, callCount) }
    }

    @Test
    fun tierBandColumn_widthTracksCaptionLength_andNeverExceedsAThirdOfTheRow() {
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

        val rowWidth = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.width
        val shortBandWidth = composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).fetchSemanticsNode().boundsInRoot.width
        val longBandWidth = composeRule.onNodeWithTag(TierDetailTestTags.tierBand(2)).fetchSemanticsNode().boundsInRoot.width
        val minWidthPx = with(composeRule.density) { 56.dp.toPx() }

        assertTrue("a short caption's band must be narrower than a long caption's", shortBandWidth < longBandWidth)
        assertTrue("band must never go below the 56dp floor", shortBandWidth >= minWidthPx - 1f)
        assertTrue("band must never exceed a third of the row", longBandWidth <= rowWidth / 3f + 1f)
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
                    TierDetailScreenContent(state = TierDetailUiState.Success(list), onBack = {}, onAddClick = {})
                }
            }
        }

        composeRule.onNodeWithText("S").assertIsDisplayed()
        composeRule.onNodeWithText("Masterpiece").assertDoesNotExist()
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

            // Hold well past any reasonable single-frame settling delay; if autoscroll
            // hadn't actually stopped, whatever is at midTarget would keep changing.
            composeRule.mainClock.advanceTimeBy(1500)
            val tierAtMidAfter = tierIdAtPoint(midTarget, allIds)

            assertEquals("scrolling must stop once the pointer leaves the edge zone", tierAtMidBefore, tierAtMidAfter)

            composeRule.onNodeWithTag(TierDetailTestTags.tile(500)).performTouchInput { cancel() }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
        composeRule.waitForIdle()
    }

    private fun tierIdAtPoint(pointInRoot: Offset, candidateIds: List<Long>): Long? = candidateIds.firstOrNull { id ->
        val bounds = composeRule.onAllNodesWithTag(TierDetailTestTags.tierRow(id))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .firstOrNull()
            ?.boundsInRoot
        bounds != null && pointInRoot.y >= bounds.top && pointInRoot.y <= bounds.bottom
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

    private fun rankedRowBounds(itemId: Long): Rect =
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(itemId)).fetchSemanticsNode().boundsInRoot

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

    private fun List<Tier>.asTierList(displayMode: TierListDisplayMode = TierListDisplayMode.WRAP): TierList =
        TierList(id = 1, title = "Sci-fi films", tiers = this, displayMode = displayMode)

    private fun setScreen(
        state: TierDetailUiState,
        onBack: () -> Unit = {},
        startInTitleEdit: Boolean = false,
        canDiscard: Boolean = false,
        onDiscard: () -> Unit = {},
        onTitleEditStarted: () -> Unit = {},
        onAddClick: () -> Unit = {},
        onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
        onDeleteItem: (itemId: Long) -> Unit = {},
        onRestoreItem: (itemId: Long) -> Unit = {},
        onReorderTiers: (orderedTierIds: List<Long>) -> Unit = {},
        onDeleteTierToPool: (tierId: Long, poolId: Long, poolSize: Int, itemIds: List<Long>) -> Unit =
            { _, _, _, _ -> },
        onRestoreTier: (
            label: String,
            caption: String?,
            colorLight: String,
            colorDark: String,
            position: Int,
            itemIds: List<Long>,
        ) -> Unit = { _, _, _, _, _, _ -> },
        onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit = { _, _, _, _ -> },
        onEditTier: (id: Long, label: String, caption: String?, colorLight: String, colorDark: String) -> Unit =
            { _, _, _, _, _ -> },
        onSetDisplayMode: (displayMode: TierListDisplayMode) -> Unit = {},
        onRenameList: (title: String) -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = state,
                    onBack = onBack,
                    startInTitleEdit = startInTitleEdit,
                    canDiscard = canDiscard,
                    onDiscard = onDiscard,
                    onTitleEditStarted = onTitleEditStarted,
                    onAddClick = onAddClick,
                    onMoveItem = onMoveItem,
                    onDeleteItem = onDeleteItem,
                    onRestoreItem = onRestoreItem,
                    onReorderTiers = onReorderTiers,
                    onDeleteTierToPool = onDeleteTierToPool,
                    onRestoreTier = onRestoreTier,
                    onAddTier = onAddTier,
                    onEditTier = onEditTier,
                    onSetDisplayMode = onSetDisplayMode,
                    onRenameList = onRenameList,
                )
            }
        }
    }

    private fun openListSettings() {
        composeRule.onNodeWithContentDescription(
            string(R.string.tier_detail_content_description_more),
        ).performClick()
    }

    private fun openTierEditor() {
        openListSettings()
        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).performClick()
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun defaultList(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            tier(id = 1, label = "S", items = List(1) { tierItem(it) }),
            tier(id = 2, label = "A", items = List(1) { tierItem(it) }),
            tier(id = 3, label = "B", items = List(1) { tierItem(it) }),
            tier(id = 4, label = "C", items = List(1) { tierItem(it) }),
            tier(id = 5, label = "D", items = List(1) { tierItem(it) }),
            tier(id = 6, label = "Pool", items = List(6) { tierItem(it) }, isPool = true),
        ),
    )

    private fun tierItem(index: Int): TierItem = TierItem(id = index.toLong(), title = "Item $index", imageUrl = null)

    private fun tier(
        id: Long,
        label: String,
        items: List<TierItem>,
        isPool: Boolean = false,
        caption: String? = null,
        colorLight: String = "#000000",
        colorDark: String = "#000000",
    ): Tier = Tier(
        id = id,
        label = label,
        colorLight = colorLight,
        colorDark = colorDark,
        items = items,
        isPool = isPool,
        caption = caption,
    )
}
