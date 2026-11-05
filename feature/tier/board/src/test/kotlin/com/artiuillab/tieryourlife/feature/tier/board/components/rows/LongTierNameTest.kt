package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A tier named in words rather than a letter: it wraps between them and gets smaller, and the row keeps its shape. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LongTierNameTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val style = TextStyle(fontSize = 24.sp, lineHeight = 28.sp)

    @Test
    fun aLetter_keepsItsSize() {
        val (letter) = fit(listOf("S" to null))

        assertEquals(style, letter.first)
    }

    @Test
    fun aLongName_getsSmaller_andBreaksOnlyBetweenWords() {
        val (fitted, layout) = fit(listOf(SHORTER to null)).single()

        assertTrue(fitted.fontSize.value < style.fontSize.value)
        assertEquals(fitted.fontSize.value * 28f / 24f, fitted.lineHeight.value, 0.01f)
        assertTrue(layout.lineCount in 2..BAND_LABEL_MAX_LINES)
        val ends = (0 until layout.lineCount - 1).map { layout.getLineEnd(it) }
        assertTrue("broke a word at $ends", ends.all { SHORTER[it - 1] == ' ' || SHORTER[it] == ' ' })
    }

    @Test
    fun aRowThatDoesNotGrow_keepsTheNameInsideItsHeight() {
        val (free, held) = fit(listOf(LONG_NAME to null, LONG_NAME to 40))

        assertTrue(held.second.size.height <= 40)
        assertTrue(held.first.fontSize <= free.first.fontSize)
    }

    @Test
    fun aLongNameOnEveryKindOfBoard_staysInItsBand_andTheRowsKeepTheirHeight() {
        var mode by mutableStateOf(TierListDisplayMode.WRAP)
        composeRule.setContent {
            TierYourLifeTheme { TierDetailScreenContent(state = TierDetailUiState.Success(boardWithALongName.copy(displayMode = mode))) }
        }

        listOf(TierListDisplayMode.WRAP, TierListDisplayMode.HORIZONTAL_SCROLL).forEach { shown ->
            mode = shown
            composeRule.waitForIdle()
            val band = bounds(TierDetailTestTags.tierBand(longTier))
            val name = composeRule.onNode(hasText(LONG_NAME), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("$shown: $name outside $band", name.left >= band.left - 1 && name.right <= band.right + 1 && name.bottom <= band.bottom + 1)
        }
        val short = bounds(TierDetailTestTags.tierRow(previewTierList.tiers[1].id))
        assertEquals(short.height, bounds(TierDetailTestTags.tierRow(longTier)).height, 1f)

        mode = TierListDisplayMode.FLAT_RANKED
        composeRule.waitForIdle()
        val badge = composeRule.onAllNodes(hasText(LONG_NAME), useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
        assertTrue("the badge took ${badge.width}px", badge.width <= 96 * composeRule.density.density)
    }

    @Test
    fun longNames_asTheyLook() {
        composeRule.setContent {
            TierYourLifeTheme { TierDetailScreenContent(state = TierDetailUiState.Success(boardWithALongName)) }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/board_long_tier_names.png", roborazziOptions = OPTIONS)
    }

    private fun fit(labels: List<Pair<String, Int?>>): List<Pair<TextStyle, TextLayoutResult>> {
        var answers = emptyList<Pair<TextStyle, TextLayoutResult>>()
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val measurer = rememberTextMeasurer()
                answers = labels.map { (label, heightPx) ->
                    val fitted = fittedBandLabel(measurer, label, style, WIDTH_PX, heightPx)
                    fitted to measurer.measure(label, fitted, maxLines = BAND_LABEL_MAX_LINES, constraints = Constraints(maxWidth = WIDTH_PX))
                }
            }
        }
        composeRule.waitForIdle()
        return answers
    }

    private val boardWithALongName = previewTierList.copy(
        tiers = previewTierList.tiers.mapIndexed { index, tier -> if (index == 0) tier.copy(label = LONG_NAME, caption = "Every time") else tier },
    )

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private val longTier: Long get() = previewTierList.tiers.first().id

    private companion object {
        const val WIDTH_PX = 52
        const val SHORTER = "Absolutely the best"
        const val LONG_NAME = "Would watch again with friends"
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
