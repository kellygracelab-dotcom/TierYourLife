package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WrappedDragReturnTest : TierDetailScreenTestBase() {
    @Test fun aVerticalReturnAcrossTheSecondLine_changesSlotOnce() = checkReturn(LayoutDirection.Ltr)

    @Test fun aVerticalReturnAcrossTheSecondLine_inArabic_changesSlotOnce() = checkReturn(LayoutDirection.Rtl)

    private fun checkReturn(direction: LayoutDirection) {
        val cards = (1L..6L).map { item(it, "Card $it") }
        val board = listOf(tier(1, "S", cards), tier(6, "Pool", emptyList(), isPool = true)).asTierList()
        var moved: Triple<Long, Long, Int>? = null
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Box(Modifier.width(380.dp).fillMaxHeight()) {
                        TierDetailScreenContent(
                            state = TierDetailUiState.Success(board),
                            actions = TierDetailActions(onMoveItem = { id, tier, position -> moved = Triple(id, tier, position) }),
                        )
                    }
                }
            }
        }
        val first = tileBounds(4)
        val last = tileBounds(6)
        assertTrue("five cards on the first line, the sixth alone on the next", last.top > first.bottom)
        val start = first.center
        val bottom = Offset(start.x, last.center.y)
        composeRule.onRoot().performTouchInput {
            down(start)
            advanceEventTime(250)
            moveTo(start + Offset(2f, 0f))
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { moveTo(bottom, delayMillis = 32) }
        composeRule.waitForIdle()
        val tops = mutableListOf<Float>()
        repeat(15) { index ->
            val y = bottom.y + (start.y - bottom.y) * (index + 1) / 15f
            composeRule.onRoot().performTouchInput { moveTo(Offset(start.x, y), delayMillis = 32) }
            composeRule.waitForIdle()
            tops += composeRule.onNodeWithTag(TierDetailTestTags.DROP_SLOT).fetchSemanticsNode().boundsInRoot.top
        }
        val changes = tops.zipWithNext().count { (before, after) -> kotlin.math.abs(before - after) > 2f }
        assertTrue("a monotone return must not alternate slot lines: $tops", changes <= 1)
        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()
        assertEquals(Triple(4L, 1L, 3), moved)
    }
}
