package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Start from Olena's ranking? Your version will be replaced." — Replace or Cancel, the web's words. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReplaceYoursDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val answers = mutableListOf<String>()

    private fun show(name: String?) {
        composeRule.setContent {
            TierYourLifeTheme {
                ReplaceYoursDialog(name = name, onReplace = { answers += "replace" }, onCancel = { answers += "cancel" })
            }
        }
    }

    @Test
    fun namesWhoseRankingItStartsFrom_andReplaceGoesOn() {
        show("Olena M.")

        composeRule.onNodeWithText("Start from Olena M.’s ranking?").assertExists()
        composeRule.onNodeWithText("Your version will be replaced.").assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.REPLACE_YOURS_CONFIRM).performClick()

        assertEquals(listOf("replace"), answers)
    }

    @Test
    fun withoutAName_andCancelKeepsOnesOwn() {
        show(null)

        composeRule.onNodeWithText("Start from this ranking?").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf("cancel"), answers)
    }

    @Test
    @Config(qualifiers = "+ru")
    fun inRussian_theNameIsNotDeclined() {
        show("Олена")

        composeRule.onNodeWithText("Начать с рейтинга: Олена?").assertExists()
        composeRule.onNodeWithText("Заменить").assertExists()
    }
}
