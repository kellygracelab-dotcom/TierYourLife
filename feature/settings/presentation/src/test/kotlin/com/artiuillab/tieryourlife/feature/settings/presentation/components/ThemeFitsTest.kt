package com.artiuillab.tieryourlife.feature.settings.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Coordinator, 26.11.2026: on a 320dp phone "System" was cut off; the three now share the card's width. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w320dp-h640dp-port-mdpi")
class ThemeFitsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val segments = listOf(
        "Light" to SettingsTestTags.THEME_LIGHT,
        "Dark" to SettingsTestTags.THEME_DARK,
        "System" to SettingsTestTags.THEME_SYSTEM,
    )

    @Test
    fun onTheSmallestPhone_eachChoiceIsWhole_sideBySide() {
        var shown by mutableStateOf(ThemeChoice.LIGHT)
        composeRule.setContent {
            // Inside the Appearance card, 16dp in from each edge.
            TierYourLifeTheme { Box(Modifier.padding(horizontal = 16.dp)) { ThemeSection(themeChoice = shown, onThemeChoiceChange = {}) } }
        }
        ThemeChoice.entries.forEach { chosen ->
            shown = chosen
            composeRule.waitForIdle()

            segments.forEach { (label, tag) ->
                val text = composeRule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
                val layout = mutableListOf<TextLayoutResult>()
                text.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layout)
                val whole = layout.single().let { it.lineCount == 1 && it.multiParagraph.intrinsics.maxIntrinsicWidth <= it.size.width }
                assertTrue("$label cut off with $chosen chosen", whole)
                val segment = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
                // The segment's own 12dp padding stays clear on both sides: nothing runs into the border.
                assertTrue(
                    "$label pressed to its border with $chosen chosen",
                    text.boundsInRoot.left - segment.left >= PADDING && segment.right - text.boundsInRoot.right >= PADDING,
                )
            }
            // The card's width, 16dp in from each of its edges: 32 to 288 on a 320dp phone at 1px per dp.
            val row = segments.map { (_, tag) -> composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot }
            assertTrue("row from ${row.first().left} to ${row.last().right}", row.first().left == ROW_LEFT && row.last().right == ROW_RIGHT)
            // Side by side, not stacked: all three on one line.
            val tops = segments.map { (_, tag) -> composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top }
            assertFalse("stacked with $chosen chosen", tops.distinct().size > 1)
        }
    }

    private companion object {
        const val PADDING = 12f
        const val ROW_LEFT = 32f
        const val ROW_RIGHT = 288f
    }
}
