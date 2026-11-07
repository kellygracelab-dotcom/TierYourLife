package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real type at a phone's 420 dpi, on the board's ordinary 44 dp card with 8 dp between cards. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class TextCardTitleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun danylosTitles_readWhole_atOneSizeAboveTheFloor() {
        val (fitter, measurer) = fitterAt(fontScale = 1f)
        val shared = sharedCardTitleSize(listOf("Interstellar", "Solid picks")) { fitter.fit(it) }!!

        assertTrue("shared $shared", shared >= CARD_TITLE_SMALLEST_SP)
        listOf("Interstellar", "Solid picks").forEach { title ->
            val fitted = fitter.fit(title, shared)
            assertEquals(shared, fitted.size)
            assertWhole(title, fitted)
            assertNoEllipsis(fitter, measurer, fitted)
        }
        assertEquals(listOf("Solid", "picks"), fitter.fit("Solid picks", shared).lines)
    }

    @Test
    fun aRussianTitle_keepsItsFirstWordWhole_atTheFloor() {
        val (fitter, measurer) = fitterAt(fontScale = 1f)
        val fitted = fitter.fit("Унесённые призраками")

        assertEquals(CARD_TITLE_SMALLEST_SP, fitted.size)
        assertEquals(listOf("Унесённые", "призраками"), fitted.lines)
        assertTrue(measurer.width(fitter, "Унесённые", fitted.size) <= fitter.room.width + fitted.extra)
    }

    @Test
    fun aLongTitle_takesThreeLinesAtMost_andLeavesTheRestForTheEllipsis() {
        val (fitter, _) = fitterAt(fontScale = 1f)
        val fitted = fitter.fit(ETERNAL)

        assertEquals(CARD_TITLE_MAX_LINES, fitted.lines.size)
        assertWhole(ETERNAL, fitted)
    }

    @Test
    fun onASidewaysPhonesSmallerCard_interstellarStillReadsWhole_leaningIntoTheGap() {
        val (fitter, measurer) = fitterAt(fontScale = 1f, card = DpSize(40.dp, 58.dp), gap = 6.dp)
        val fitted = fitter.fit("Interstellar")

        assertEquals(listOf("Interstellar"), fitted.lines)
        assertNoEllipsis(fitter, measurer, fitted)
        assertTrue(fitter.growth(fitted.extra) > 0)
    }

    @Test
    fun aOneWordCard_onAnOrdinaryTile_isNotCutInTwo() {
        val (fitter, measurer) = fitterAt(fontScale = 1f)
        val fitted = fitter.fit("Item-293")

        assertEquals(listOf("Item-293"), fitted.lines)
        assertNoEllipsis(fitter, measurer, fitted)
    }

    @Test
    fun atLargeType_noWordSplits_andNoCardLeansFurtherThanTheGapAllows() {
        val scales = listOf(1.3f, 1.5f)
        fittersAt(scales).zip(scales).forEach { (pair, scale) ->
            val fitter = pair.first
            val gapKept = (2 * 2.625f).toInt()
            listOf("Interstellar", "Solid picks", "Унесённые призраками", ETERNAL).forEach { title ->
                val fitted = fitter.fit(title)
                assertTrue("$title at $scale: ${fitted.size}", fitted.size >= CARD_TITLE_SMALLEST_SP)
                assertTrue("$title at $scale: ${fitted.lines}", fitted.lines.size <= CARD_TITLE_MAX_LINES)
                assertWhole(title, fitted)
                assertTrue("$title at $scale leans ${fitter.growth(fitted.extra)}", fitter.growth(fitted.extra) <= (8 * 2.625f).toInt() - gapKept)
            }
        }
    }

    @Test
    fun onABoard_eachCardIsFoundByItsWholeName_andStaysNearItsCell() {
        val titles = listOf("Interstellar", "Solid picks", "Унесённые призраками", ETERNAL)
        val items = titles.mapIndexed { index, title -> TierItem(id = index.toLong(), title = title, imageUrl = null) }
        composeRule.setContent {
            TierYourLifeTheme {
                ProvideTextCardRoom(items, 44.dp, 64.dp, 8.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { ItemTile(it, 44.dp, 64.dp) }
                    }
                }
            }
        }

        val widest = (44 + 8 - 2) * composeRule.density.density
        titles.forEach { title ->
            val bounds = composeRule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
            assertTrue("$title is ${bounds.width} px", bounds.width <= widest)
        }
    }

    private fun fitterAt(fontScale: Float, card: DpSize = ORDINARY, gap: Dp = 8.dp): Pair<TextCardFitter, TextMeasurer> =
        fittersAt(listOf(fontScale), card, gap).single()

    private fun fittersAt(fontScales: List<Float>, card: DpSize = ORDINARY, gap: Dp = 8.dp): List<Pair<TextCardFitter, TextMeasurer>> {
        val answers = mutableListOf<Pair<TextCardFitter, TextMeasurer>>()
        composeRule.setContent {
            TierYourLifeTheme {
                fontScales.forEach { fontScale ->
                    val density = Density(LocalDensity.current.density, fontScale)
                    CompositionLocalProvider(LocalDensity provides density) {
                        val measurer = rememberTextMeasurer()
                        answers += TextCardFitter(measurer, LocalTextStyle.current, density, card.width, card.height, gap) to measurer
                    }
                }
            }
        }
        composeRule.waitForIdle()
        return answers
    }

    private fun TextMeasurer.width(fitter: TextCardFitter, text: String, size: Float): Int =
        measure(text, fitter.style(size), softWrap = false, maxLines = 1).size.width

    private fun assertWhole(title: String, fitted: CardTitle) {
        assertEquals("$title came out as ${fitted.lines}", title, fitted.lines.joinToString(" "))
    }

    private fun assertNoEllipsis(fitter: TextCardFitter, measurer: TextMeasurer, fitted: CardTitle) {
        fitted.lines.forEach { line ->
            assertTrue("'$line' at ${fitted.size}", measurer.width(fitter, line, fitted.size) <= fitter.room.width + fitted.extra)
        }
    }

    private companion object {
        const val ETERNAL = "Eternal Sunshine of the Spotless Mind"
        val ORDINARY = DpSize(44.dp, 64.dp)
    }
}
