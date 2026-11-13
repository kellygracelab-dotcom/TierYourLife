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

/** 13.11: under a copy's title, the website's plain link to the original, naming nobody, which can be hidden. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class OriginalLinkTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0
    private var hidden = 0

    @Test
    fun theLink_opensTheOriginal_orIsHidden() {
        composeRule.setContent {
            TierYourLifeTheme { OriginalLink(onOpen = { opened++ }, onHide = { hidden++ }) }
        }

        composeRule.onNodeWithText("by", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Original list and rankings").performClick()
        composeRule.onNodeWithContentDescription("Hide this link").performClick()

        composeRule.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, hidden)
        }
    }

    @Test
    fun aHiddenLink_staysHiddenWhenTheBoardOpensAgain() {
        val preferences = FakeAppPreferences()
        OriginalLinkViewModel(preferences).hide("board-1")

        val reopened = OriginalLinkViewModel(preferences)

        assertTrue("board-1" in reopened.hidden.value)
        assertTrue("board-2" !in reopened.hidden.value)
    }
}
