package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierDetailTierRowDragTest : TierDetailScreenTestBase() {

    @Test
    fun liftingATierRow_collapsesAllRankedRowsToOneHeight() {
        val list = listOf(
            tier(id = 1, label = "S", items = List(24) { tierItem(it) }),
            tier(id = 2, label = "A", items = List(1) { tierItem(it + 100) }),
            tier(id = 3, label = "B", items = List(6) { tierItem(it + 200) }),
            tier(id = 6, label = "Pool", items = emptyList(), isPool = true),
        ).asTierList()
        setScreen(TierDetailUiState.Success(list))

        val before1 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height
        val before2 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).fetchSemanticsNode().boundsInRoot.height
        assertNotEquals("sanity: S (24 items) must be taller than A (1 item) before collapsing", before1, before2)

        beginDrag(TierDetailTestTags.tierBand(1))

        val liftedHeight = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(1)).fetchSemanticsNode().boundsInRoot.height
        val otherHeight1 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(2)).fetchSemanticsNode().boundsInRoot.height
        val otherHeight2 = composeRule.onNodeWithTag(TierDetailTestTags.tierRow(3)).fetchSemanticsNode().boundsInRoot.height
        assertEquals(
            "every ranked row not being lifted collapses to the same height",
            otherHeight1,
            otherHeight2,
            0.5f,
        )
        assertEquals(
            "the lifted row keeps its pickup scale on top of the shared collapsed height",
            otherHeight1 * 1.02f,
            liftedHeight,
            0.5f,
        )

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
            assertEquals(listOf(2L, 3L, 1L), reorderedIds)
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
            assertFalse("the pool's id must never appear in a tier reorder", reorderedIds.orEmpty().contains(6L))
        }
    }

    @Test
    fun droppingATierRow_intoTheTrash_requestsAnAtomicTierDeletion() {
        var deletedTierId: Long? = null
        val list = listOf(
            tier(id = 1, label = "S", items = List(2) { tierItem(it) }),
            tier(id = 2, label = "A", items = emptyList()),
            tier(id = 6, label = "Pool", items = List(3) { tierItem(it + 100) }, isPool = true),
        ).asTierList()
        setScreen(
            TierDetailUiState.Success(list),
            onDeleteTierToPool = { tierId -> deletedTierId = tierId },
        )

        dragTileIntoTrash(TierDetailTestTags.tierBand(1))

        composeRule.runOnIdle {
            assertEquals(1L, deletedTierId)
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
                    actions = TierDetailActions(
                        onMoveItem = { itemId, toTierId, _ -> moved = itemId to toTierId },
                        onDeleteTierToPool = { tierId ->
                            val removedTier = list.tiers.first { it.id == tierId }
                            val poolId = list.tiers.first { it.isPool }.id
                            list = list.copy(
                                tiers = list.tiers
                                    .filterNot { it.id == tierId }
                                    .map { t -> if (t.id == poolId) t.copy(items = t.items + removedTier.items) else t },
                            )
                        },
                    ),
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
                    actions = TierDetailActions(
                        onMoveItem = { _, _, toPosition -> lastPosition = toPosition },
                        onDeleteItem = { deletedId ->
                            list = list.copy(
                                tiers = list.tiers.map { t -> t.copy(items = t.items.filterNot { it.id == deletedId }) },
                            )
                        },
                    ),
                )
            }
        }

        dragTileIntoTrash(TierDetailTestTags.tile(101))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(101)).assertDoesNotExist()

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
}
