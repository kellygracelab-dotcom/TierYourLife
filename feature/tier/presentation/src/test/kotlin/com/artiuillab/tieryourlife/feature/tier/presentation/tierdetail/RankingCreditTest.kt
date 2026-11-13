package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 13.11: "Your ranking of a list" read as if the author's name had been lost; it names the Community instead. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class RankingCreditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0
    private var putAway = 0

    private fun show(authorName: String) {
        composeRule.setContent {
            TierYourLifeTheme {
                RankingCredit(authorName = authorName, onOpen = { opened++ }, onPutAway = { putAway++ })
            }
        }
    }

    @Test
    fun anAuthorWithAPublicName_isNamed() {
        show(" Olena ")

        composeRule.onNodeWithText("Your ranking of a Community list by Olena").assertExists()
    }

    // An editorial or anonymous list, and a person with no public name, alike.
    @Test
    fun aListThatNamesNobody_isACommunityList() {
        show("")

        composeRule.onNodeWithText("Your ranking of a Community list").assertExists()
        composeRule.onNodeWithText("by", substring = true).assertDoesNotExist()
    }

    @Test
    fun theLine_opensTheOriginal_orIsPutAway() {
        show("Olena")

        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithContentDescription("Hide this line").performClick()

        composeRule.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, putAway)
        }
    }

    @Test
    fun aLinePutAway_staysAwayWhenTheBoardOpensAgain() {
        val preferences = FakeAppPreferences()
        RankingCreditViewModel(preferences).putAway("board-1")

        val reopened = RankingCreditViewModel(preferences)

        assertTrue("board-1" in reopened.putAway.value)
        assertTrue("board-2" !in reopened.putAway.value)
    }
}
