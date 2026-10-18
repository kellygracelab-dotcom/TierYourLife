package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** The board screen's test rig: one Compose rule and the gestures every board test uses. */
abstract class TierDetailScreenTestBase {

    @get:Rule
    val composeRule = createComposeRule()

    protected fun tierIdAtPoint(pointInRoot: Offset, candidateIds: List<Long>): Long? = candidateIds.firstOrNull { id ->
        val bounds = composeRule.onAllNodesWithTag(TierDetailTestTags.tierRow(id))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .firstOrNull()
            ?.boundsInRoot
        bounds != null && pointInRoot.y >= bounds.top && pointInRoot.y <= bounds.bottom
    }

    protected fun secondLineFirstItemId(items: List<TierItem>): Long {
        val boundsById = items.associate { it.id to tileBounds(it.id) }
        val lines = boundsById.entries.groupBy { it.value.top }.toSortedMap().values.toList()
        assertTrue("expected the tier to wrap to at least two lines", lines.size >= 2)
        return lines[1].minBy { it.value.left }.key
    }

    protected fun tileBounds(itemId: Long): Rect =
        composeRule.onNodeWithTag(TierDetailTestTags.tile(itemId)).fetchSemanticsNode().boundsInRoot

    protected fun rankedRowBounds(itemId: Long): Rect =
        composeRule.onNodeWithTag(TierDetailTestTags.rankedRow(itemId)).fetchSemanticsNode().boundsInRoot

    protected fun dragTile(sourceTag: String, targetTag: String, horizontalBias: Float) {
        val targetBounds = composeRule.onNodeWithTag(targetTag).fetchSemanticsNode().boundsInRoot
        val verticalOffset = if (targetBounds.height > 0f) targetBounds.height * 0.3f else 8f
        val target = Offset(
            x = targetBounds.left + targetBounds.width * horizontalBias,
            y = targetBounds.top + verticalOffset,
        )
        dragTileToRoot(sourceTag, target)
    }

    // Landing before a card is a claim about that card, not about a fraction
    // of the row -- and the row is as wide as the window. A twentieth of the
    // way across a tablet is already most of the way through the first card.
    protected fun dragTileOntoLeadingHalf(sourceTag: String, targetTag: String) {
        val targetBounds = composeRule.onNodeWithTag(targetTag).fetchSemanticsNode().boundsInRoot
        dragTileToRoot(
            sourceTag,
            Offset(x = targetBounds.left + targetBounds.width * 0.25f, y = targetBounds.center.y),
        )
    }

    protected fun dragTileToRoot(sourceTag: String, rootTarget: Offset) {
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

    protected fun beginDrag(sourceTag: String) {
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

    protected fun cancelDrag(sourceTag: String) {
        composeRule.onNodeWithTag(sourceTag).performTouchInput { cancel() }
        composeRule.waitForIdle()
    }

    protected fun dragTileIntoTrash(sourceTag: String) {
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

    protected fun item(id: Long, title: String): TierItem = TierItem(id = id, title = title, imageUrl = null)

    protected fun List<Tier>.asTierList(displayMode: TierListDisplayMode = TierListDisplayMode.WRAP): TierList =
        TierList(id = 1, title = "Sci-fi films", tiers = this, displayMode = displayMode)

    protected fun setScreen(
        state: TierDetailUiState,
        onBack: () -> Unit = {},
        onAddClick: () -> Unit = {},
        onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit = { _, _, _ -> },
        onDeleteItem: (itemId: Long) -> Unit = {},
        onRestoreItem: (itemId: Long) -> Unit = {},
        onReorderTiers: (orderedTierIds: List<Long>) -> Unit = {},
        onDeleteTierToPool: (tierId: Long) -> Unit = {},
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
                    actions = TierDetailActions(
                        onBack = onBack,
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
                    ),
                )
            }
        }
    }

    protected fun openListSettings() {
        composeRule.onNodeWithContentDescription(
            string(ThemeR.string.tier_detail_content_description_more),
        ).performClick()
    }

    protected fun openTierEditor() {
        openListSettings()
        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).performScrollTo().performClick()
    }

    protected fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    protected fun defaultList(): TierList = TierList(
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

    protected fun tierItem(index: Int): TierItem = TierItem(id = index.toLong(), title = "Item $index", imageUrl = null)

    protected fun tier(
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
