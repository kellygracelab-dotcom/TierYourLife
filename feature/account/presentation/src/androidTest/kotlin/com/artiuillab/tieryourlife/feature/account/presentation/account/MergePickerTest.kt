package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The one question on the way into an account used before: this phone's own boards, and only those. */
@RunWith(AndroidJUnit4::class)
class MergePickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var movedIn = 0
    private var keptHere = 0

    @Test
    fun theQuestionNamesTheBoardsAndTheAccount() {
        setScreen(GuestBoardsQuestion(boards = 3, into = EMAIL))

        composeRule.onNodeWithTag(AccountTestTags.MERGE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.merge_body, plural(3), EMAIL)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.merge_action_move_in, EMAIL)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.merge_action_keep_here)).assertIsDisplayed()
    }

    @Test
    fun boardsOfAnAccountSignedOutOf_areAskedAboutInTheirOwnWords() {
        setScreen(GuestBoardsQuestion(boards = 2, into = EMAIL, fromAnotherAccount = true))

        composeRule.onNodeWithText(string(R.string.merge_held_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.merge_held_body, plural(2), EMAIL)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.merge_action_move_in, EMAIL)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.merge_action_keep_here)).assertIsDisplayed()
    }

    @Test
    fun anAccountWithoutAnEmail_isStillNamed() {
        setScreen(GuestBoardsQuestion(boards = 1, into = null))

        val account = string(R.string.merge_account_fallback)
        composeRule.onNodeWithText(string(R.string.merge_body, plural(1), account)).assertIsDisplayed()
    }

    @Test
    fun movingIn_reachesTheCaller() {
        setScreen(GuestBoardsQuestion(boards = 2, into = EMAIL))

        composeRule.onNodeWithTag(AccountTestTags.MERGE_MOVE_IN).performClick()

        composeRule.runOnIdle {
            assertEquals(1, movedIn)
            assertEquals(0, keptHere)
        }
    }

    @Test
    fun keepingHere_reachesTheCaller() {
        setScreen(GuestBoardsQuestion(boards = 2, into = EMAIL))

        composeRule.onNodeWithTag(AccountTestTags.MERGE_KEEP_HERE).performClick()

        composeRule.runOnIdle {
            assertEquals(1, keptHere)
            assertEquals(0, movedIn)
        }
    }

    // Closing is the answer that changes nothing: the boards stay where they are.
    @Test
    fun closingTheQuestion_keepsTheBoardsHere() {
        setScreen(GuestBoardsQuestion(boards = 1, into = EMAIL))

        composeRule.onNodeWithTag(AccountTestTags.CLOSE).performClick()

        composeRule.runOnIdle {
            assertEquals(1, keptHere)
            assertEquals(0, movedIn)
        }
    }

    private fun setScreen(question: GuestBoardsQuestion) {
        movedIn = 0
        keptHere = 0
        composeRule.setContent {
            TierYourLifeTheme {
                AccountScreenContent(
                    state = AccountUiState(account = Account.SignedIn(question.into, null), merge = question),
                    onClose = {},
                    onSignIn = {},
                    onSignOut = {},
                    onMoveGuestBoardsIn = { movedIn++ },
                    onKeepGuestBoardsHere = { keptHere++ },
                )
            }
        }
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun plural(count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(R.plurals.account_board_count, count, count)

    private companion object {
        const val EMAIL = "someone@example.com"
    }
}
