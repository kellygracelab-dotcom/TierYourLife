package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** At 420 dpi a dp is 2.625 px, and paddings rounding up left the name a pixel less than it was fitted to. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class NamesOnAFineScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun twoWordTierNames_breakBetweenTheWords() {
        composeRule.setContent {
            TierYourLifeTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TWO_WORDS.forEach { label ->
                        Column(Modifier.width(TIER_BAND_WIDTH).padding(horizontal = BAND_CAPTION_PADDING)) {
                            BandText(
                                label = label,
                                caption = null,
                                labelStyle = TierYourLifeType.current.tierBandLetter,
                                captionStyle = TierYourLifeType.current.tierBandCaption,
                                color = Color.Black,
                                width = TIER_BAND_WIDTH,
                                padding = BAND_CAPTION_PADDING,
                            )
                        }
                    }
                }
            }
        }

        TWO_WORDS.forEach(::assertBreaksOnlyBetweenWords)
    }

    @Test
    fun theMoveSheet_keepsTheNameInItsBadge_andWritesItOutBeside() {
        show(board(label = LONG_NAME, caption = "Every time"))
        composeRule.onNodeWithTag(TierDetailTestTags.tile(previewTierList.tiers[1].items.first().id)).performTouchInput { doubleClick() }

        val inTheSheet = hasAnyAncestor(hasTestTag(TierDetailTestTags.moveSheetTierOption(previewTierList.tiers[0].id)))
        val nodes = composeRule.onAllNodes(hasText(LONG_NAME) and inTheSheet, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals("the badge and the name beside it", 2, nodes.size)
        val badge = nodes.minBy { it.boundsInRoot.width }.boundsInRoot
        assertTrue("the badge's name took $badge", badge.width <= 40 * composeRule.density.density && badge.height <= 40 * composeRule.density.density)
        assertEquals(1, composeRule.onAllNodes(hasText("Every time") and inTheSheet, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    private fun show(list: TierList) = composeRule.setContent {
        TierYourLifeTheme { TierDetailScreenContent(state = TierDetailUiState.Success(list)) }
    }

    private fun board(label: String? = null, caption: String? = null): TierList = previewTierList.copy(
        tiers = previewTierList.tiers.mapIndexed { index, tier ->
            when {
                index == 0 && label != null -> tier.copy(label = label, caption = caption)
                else -> tier
            }
        },
    )

    private fun layoutOf(text: String): TextLayoutResult {
        val node = composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().first()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.single()
    }

    private fun assertBreaksOnlyBetweenWords(text: String) {
        val layout = layoutOf(text)
        val ends = (0 until layout.lineCount - 1).map { layout.getLineEnd(it) }
        assertTrue("'$text' broke at $ends", ends.all { text[it - 1].isWhitespace() || text[it].isWhitespace() || text[it - 1] == '-' })
    }

    private companion object {
        val TWO_WORDS = listOf(
            "Must watch", "Top pick", "Great films", "Never again", "Love it", "Rewatch soon", "Hidden gems",
            "Fine enough", "Skip these", "All time", "Mixed bag", "Guilty pleasure", "Pure gold", "Classics only",
            "Watch later", "Cult hits", "Big hearts", "Solid picks", "Worth it", "Hard pass", "Comfort food",
            "Slow burn", "Fan service", "Kind of", "Best ever", "Nearly there", "Quite good", "Bottom shelf",
        )
        const val LONG_NAME = "Would watch again with friends on a long rainy weekend, and then once more after"
    }
}
