package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class FloatingDragTilePositionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theFloatingCopy_staysUnderThePointerInEitherReadingDirection() {
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxSize()) { FloatingDragTile(controllerDraggingTo(POINTER_X)) }
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.fillMaxSize()) { FloatingDragTile(controllerDraggingTo(POINTER_X)) }
                }
            }
        }

        val copies = composeRule.onAllNodesWithTag(TierDetailTestTags.FLOATING_DRAG_TILE)
        val leftToRight = copies[0].getUnclippedBoundsInRoot().left.value
        val rightToLeft = copies[1].getUnclippedBoundsInRoot().left.value

        assertTrue(
            "the copy sat at $leftToRight in English and $rightToLeft in Arabic, " +
                "but the pointer was in the same place for both",
            abs(leftToRight - rightToLeft) < 2f,
        )
    }

    // The pointer is reported against the window. Once a column of boards
    // stands to the left of the board, the overlay no longer starts where the
    // window does -- and reading one as the other put every dragged card the
    // width of that column too far right.
    @Test
    fun theFloatingCopy_ignoresWhereItsOwnPaneBegins() {
        composeRule.setContent {
            TierYourLifeTheme {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(PANE_INSET).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        FloatingDragTile(controllerDraggingTo(POINTER_X))
                    }
                }
            }
        }

        val bounds = composeRule.onNodeWithTag(TierDetailTestTags.FLOATING_DRAG_TILE)
            .getUnclippedBoundsInRoot()
        val centre = with(composeRule.density) { (bounds.left + bounds.right).toPx() / 2f }

        assertTrue(
            "the pointer was at $POINTER_X but the copy was centred on $centre",
            abs(centre - POINTER_X) < 2f,
        )
    }

    private fun controllerDraggingTo(pointerX: Float) = TierDragController().apply {
        registerRowBounds(tierId = 1L, bounds = Rect(0f, 0f, 1000f, 200f))
        setValidTargets(tierIds = listOf(1L), itemIds = listOf(2L))
        beginDrag(
            payload = DragPayload(
                itemId = 2L,
                title = "dragged",
                imageUrl = null,
                sourceTierId = 1L,
                width = 44.dp,
                height = 64.dp,
            ),
            rootPosition = Offset(pointerX, 100f),
        )
    }

    private companion object {
        const val POINTER_X = 700f
        val PANE_INSET = 320.dp
    }
}
