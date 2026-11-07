package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRailTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRail_offersEveryDestinationAndTheNewBoardButton() {
        composeRule.setContent {
            TierYourLifeTheme { HomeRail(selected = RailDestination.Lists, onSelect = {}, onNewList = {}) }
        }

        RailDestination.entries.forEach { destination ->
            composeRule.onNodeWithTag(RailTestTags.item(destination)).assertExists()
        }
        composeRule.onNodeWithTag(RailTestTags.NEW_LIST).assertExists()
    }

    // Exactly one item lit, and it is the one asked for. Two lit items would
    // say the app is in two places; none would say the rail is decoration.
    @Test
    fun theSelectedItem_isTheOnlyOneLit() {
        composeRule.setContent {
            TierYourLifeTheme { HomeRail(selected = RailDestination.Community, onSelect = {}, onNewList = {}) }
        }

        composeRule.onNodeWithTag(RailTestTags.item(RailDestination.Community)).assertIsSelected()
        composeRule.onNodeWithTag(RailTestTags.item(RailDestination.Lists)).assertIsNotSelected()
        composeRule.onNodeWithTag(RailTestTags.item(RailDestination.Settings)).assertIsNotSelected()
    }

    // Off the rail's own destinations -- on a board, say -- nothing is lit.
    // Lighting Lists there would claim the board is the list screen.
    @Test
    fun withNothingSelected_nothingIsLit() {
        composeRule.setContent {
            TierYourLifeTheme { HomeRail(selected = null, onSelect = {}, onNewList = {}) }
        }

        RailDestination.entries.forEach { destination ->
            composeRule.onNodeWithTag(RailTestTags.item(destination)).assertIsNotSelected()
        }
    }

    @Test
    fun tappingAnItem_saysWhichOne() {
        val chosen = mutableListOf<RailDestination>()
        composeRule.setContent {
            TierYourLifeTheme { HomeRail(selected = RailDestination.Lists, onSelect = chosen::add, onNewList = {}) }
        }

        composeRule.onNodeWithTag(RailTestTags.item(RailDestination.Settings)).performClick()

        assertEquals(listOf(RailDestination.Settings), chosen)
    }

    // The button is the phone's corner button, moved. It makes a board; it does
    // not choose a destination, so it must not report itself as one.
    @Test
    fun theNewBoardButton_asksForABoardAndNotADestination() {
        var boardsAsked = 0
        val chosen = mutableListOf<RailDestination>()
        composeRule.setContent {
            TierYourLifeTheme {
                HomeRail(selected = RailDestination.Lists, onSelect = chosen::add, onNewList = { boardsAsked++ })
            }
        }

        composeRule.onNodeWithTag(RailTestTags.NEW_LIST).performClick()

        assertEquals(1, boardsAsked)
        assertEquals(emptyList<RailDestination>(), chosen)
    }

    @Test
    fun atFontScale2_everyLabelBreaksOnlyBetweenWords() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                TierYourLifeTheme { HomeRail(selected = null, onSelect = {}, onNewList = {}) }
            }
        }

        RailDestination.entries.forEach { destination ->
            val node = composeRule.onNodeWithTag(RailTestTags.label(destination), useUnmergedTree = true)
                .fetchSemanticsNode()
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            val layout = results.single()
            val words = layout.layoutInput.text.text.split(' ').toSet()
            val lines = (0 until layout.lineCount).map { line ->
                layout.layoutInput.text.text.substring(layout.getLineStart(line), layout.getLineEnd(line)).trim()
            }
            assertTrue("$destination: $lines", lines.all { line -> line.split(' ').all { it in words } })
        }
    }
}
