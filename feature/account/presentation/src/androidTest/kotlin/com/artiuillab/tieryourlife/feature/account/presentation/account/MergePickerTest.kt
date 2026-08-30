package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one question worth asking on the way into an account, and the cases
 * where it must not be asked at all.
 */
@RunWith(AndroidJUnit4::class)
class MergePickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val keeps = mutableListOf<MergeKeep>()
    private var applied = 0
    private var abandoned = 0

    @Test
    fun bothSidesHoldSomething_soTheQuestionIsAsked() {
        setScreen(MergeChoice(accountBoards = 2, localBoards = 3))

        composeRule.onNodeWithTag(AccountTestTags.MERGE).assertIsDisplayed()
        composeRule.onNodeWithText(
            string(R.string.merge_body, plural(2), plural(3)),
        ).assertIsDisplayed()
    }

    // Keeping both is the answer that loses nothing, so it is the one already
    // chosen when the screen appears.
    @Test
    fun keepingEverythingIsChosenAlready() {
        setScreen(MergeChoice(accountBoards = 2, localBoards = 3))

        composeRule.onNodeWithTag(AccountTestTags.MERGE_KEEP_EVERYTHING).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.merge_recommended)).assertIsDisplayed()
    }

    @Test
    fun theOtherAnswerReachesTheCaller() {
        setScreen(MergeChoice(accountBoards = 2, localBoards = 3))

        composeRule.onNodeWithTag(AccountTestTags.MERGE_ACCOUNT_ONLY).performClick()

        composeRule.runOnIdle { assertEquals(listOf(MergeKeep.AccountOnly), keeps) }
    }

    @Test
    fun continuing_reachesTheCaller() {
        setScreen(MergeChoice(accountBoards = 1, localBoards = 1))

        composeRule.onNodeWithTag(AccountTestTags.MERGE_CONTINUE).performClick()

        composeRule.runOnIdle { assertEquals(1, applied) }
    }

    // Closing the question is not an answer to it, so the person ends up
    // signed out with nothing written either way.
    @Test
    fun closingTheQuestion_writesNothingAndSignsOut() {
        setScreen(MergeChoice(accountBoards = 1, localBoards = 1))

        composeRule.onNodeWithTag(AccountTestTags.CLOSE).performClick()

        composeRule.runOnIdle {
            assertEquals(1, abandoned)
            assertEquals(0, applied)
        }
    }

    // Both cases where the question has only one possible answer, and asking
    // it would be a delay rather than a choice.
    @Test
    fun anEmptyAccountIsNeverAskedAbout() {
        assertEquals(false, MergeChoice(accountBoards = 0, localBoards = 4).needed)
    }

    @Test
    fun aPhoneWithNoBoardsIsNeverAskedAbout() {
        assertEquals(false, MergeChoice(accountBoards = 4, localBoards = 0).needed)
    }

    private fun setScreen(choice: MergeChoice) {
        keeps.clear()
        applied = 0
        abandoned = 0
        composeRule.setContent {
            TierYourLifeTheme {
                AccountScreenContent(
                    state = AccountUiState(account = Account.SignedIn(null, null), merge = choice),
                    onClose = {},
                    onSignIn = {},
                    onSignOut = {},
                    onMergeKeepChange = { keeps += it },
                    onApplyMerge = { applied++ },
                    onAbandonMerge = { abandoned++ },
                )
            }
        }
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun plural(count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(R.plurals.account_board_count, count, count)
}
