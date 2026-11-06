package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What the moderation queue calls an author: a hidden one is Anonymous, not Someone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class ReportAuthorNameTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(name: String, anonymous: Boolean) = composeRule.setContent { Text(reportAuthorName(name, anonymous)) }

    @Test
    fun anonymousWithoutName_isAnonymous() {
        show("", anonymous = true)

        composeRule.onNodeWithText("Anonymous").assertExists()
    }

    @Test
    fun anonymousSeenByItsOwner_keepsTheNameAndSaysSo() {
        show("Taras", anonymous = true)

        composeRule.onNodeWithText("Taras · Anonymous").assertExists()
    }

    @Test
    fun notAnonymous_isUnchanged() {
        show("Taras", anonymous = false)
        composeRule.onNodeWithText("Taras").assertExists()
    }

    @Test
    fun notAnonymousAndNameless_isStillSomeone() {
        show("", anonymous = false)

        composeRule.onNodeWithText("Someone").assertExists()
    }
}
