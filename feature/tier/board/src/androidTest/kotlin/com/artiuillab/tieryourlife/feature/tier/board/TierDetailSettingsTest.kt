package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@RunWith(AndroidJUnit4::class)
class TierDetailSettingsTest : TierDetailScreenTestBase() {

    @Test
    fun listSettings_opensFromMoreButton() {
        setScreen(TierDetailUiState.Success(defaultList()))

        openListSettings()

        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun manualAddButton_opensTheManualEntryDialog() {
        composeRule.setContent {
            var manualEntryVisible by remember { mutableStateOf(false) }
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(defaultList()),
                    actions = TierDetailActions(onManualAddClick = { manualEntryVisible = true }),
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
        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun horizontalScrollMode_keepsAllItemsOnOneLineAndAllowsScrollingToTheLastOne() {
        val items = List(20) { index -> item((4000 + index).toLong(), "M$index") }
        val list = listOf(tier(id = 1, label = "S", items = items))
            .asTierList(displayMode = TierListDisplayMode.HORIZONTAL_SCROLL)
        setScreen(TierDetailUiState.Success(list))

        val firstTop = tileBounds(items.first().id).top

        // The strip is lazy, so the last tile is not composed until scrolled to
        // -- which is the point of it, and why this asks the row to scroll
        // rather than asking a tile that does not exist yet.
        composeRule.onNodeWithTag(TierDetailTestTags.tierItems(1L))
            .performScrollToIndex(items.lastIndex)
        composeRule.onNodeWithTag(TierDetailTestTags.tile(items.last().id)).assertIsDisplayed()
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
                    actions = TierDetailActions(
                        onSetDisplayMode = { mode -> listState.value = listState.value.copy(displayMode = mode) },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).assertIsDisplayed()

        openListSettings()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_MODE_RANKED).performClick()
        composeRule.onNodeWithContentDescription(
            string(ThemeR.string.tier_detail_content_description_back),
        ).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(100)).assertIsDisplayed()
    }
}
