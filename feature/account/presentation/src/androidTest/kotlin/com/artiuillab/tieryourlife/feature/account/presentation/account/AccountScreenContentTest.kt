package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class AccountScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var closed = 0
    private var signInRequests = 0
    private var signOuts = 0
    private val names = mutableListOf<String>()

    private val backUpChanges = mutableListOf<Boolean>()
    private var positioningCancelled = 0
    private val confirmedCrops = mutableListOf<FaceCrop?>()

    @Test
    fun theOfferSaysWhatItWouldBeKeeping() {
        setContent(AccountUiState(account = Account.Guest, boardCount = 3))

        composeRule.onNodeWithTag(AccountTestTags.BACK_UP_BOARDS).assertIsDisplayed()
        composeRule.onNodeWithText(
            string(
                R.string.account_backup_sub,
                plural(R.plurals.account_board_count, 3),
            ),
        ).assertIsDisplayed()
    }

    // Answered here, before anything has gone up. Turning it off later is a
    // different question, because by then there is a copy to delete.
    @Test
    fun turningTheOfferOff_reachesTheCaller() {
        setContent(AccountUiState(account = Account.Guest, backUpBoards = true))

        composeRule.onNodeWithTag(AccountTestTags.BACK_UP_BOARDS).performClick()

        composeRule.runOnIdle { assertEquals(listOf(false), backUpChanges) }
    }

    @Test
    fun signedIn_neverSeesTheOffer() {
        setContent(AccountUiState(account = Account.SignedIn("someone@example.com", null)))

        composeRule.onNodeWithTag(AccountTestTags.BACK_UP_BOARDS).assertDoesNotExist()
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun plural(resourceId: Int, count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(resourceId, count, count)

    private fun setContent(state: AccountUiState) {
        closed = 0
        signInRequests = 0
        signOuts = 0
        backUpChanges.clear()
        names.clear()
        positioningCancelled = 0
        confirmedCrops.clear()
        composeRule.setContent {
            TierYourLifeTheme {
                AccountScreenContent(
                    state = state,
                    onClose = { closed++ },
                    onSignIn = { signInRequests++ },
                    onSignOut = { signOuts++ },
                    onSetName = { names += it },
                    onBackUpBoardsChange = { backUpChanges += it },
                    onCancelPositioning = { positioningCancelled++ },
                    onConfirmPosition = { confirmedCrops += it },
                )
            }
        }
    }

    private fun guest(signingIn: Boolean = false) =
        AccountUiState(account = Account.Guest, signingIn = signingIn)

    private fun signedIn(
        displayName: String? = "Olena M.",
        credits: Int? = 12,
        publicListCount: Int = 3,
    ) = AccountUiState(
        account = Account.SignedIn(
            email = "someone@example.com",
            photoUrl = null,
            displayName = displayName,
        ),
        credits = credits,
        publicListCount = publicListCount,
    )

    @Test
    fun asAGuest_theOfferAndItsThreeReasonsAreShown() {
        setContent(guest())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.reason(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.reason(1)).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.reason(2)).assertIsDisplayed()
    }

    // Declining has to be as easy to reach as accepting, and it leaves without
    // asking anything further.
    @Test
    fun notNow_closesTheScreen_withoutSigningIn() {
        setContent(guest())

        composeRule.onNodeWithTag(AccountTestTags.NOT_NOW).performClick()

        composeRule.runOnIdle {
            assertEquals(1, closed)
            assertEquals(0, signInRequests)
        }
    }

    @Test
    fun signIn_asksOnce() {
        setContent(guest())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).performClick()

        composeRule.runOnIdle { assertEquals(1, signInRequests) }
    }

    // The picker is a separate window; a second tap behind it would open a
    // second one.
    @Test
    fun signIn_isDisabledWhileThePickerIsOpen() {
        setContent(guest(signingIn = true))

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertIsNotEnabled()
    }

    @Test
    fun onceSignedIn_theOfferIsReplacedByTheAccountItself() {
        setContent(signedIn())

        composeRule.onNodeWithTag(AccountTestTags.EMAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.CREDITS).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.DONE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertDoesNotExist()
    }

    // Nothing is counted in the stub build, and a zero would read as bad news
    // on the one screen whose job is to say the account was worth something.
    @Test
    fun signedIn_withNothingCounted_showsNoBalanceAtAll() {
        setContent(signedIn(credits = null))

        composeRule.onNodeWithTag(AccountTestTags.CREDITS).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountTestTags.EMAIL).assertIsDisplayed()
    }

    // Confirmed now, and only because the boards are kept somewhere: before
    // that, signing out changed nothing about anybody's data.
    @Test
    fun signedIn_asksBeforeSigningOut() {
        setContent(signedIn())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT).performScrollTo().performClick()

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT_DIALOG).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, signOuts) }
    }

    @Test
    fun confirmingTheQuestion_signsOut() {
        setContent(signedIn())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT).performScrollTo().performClick()
        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT_CONFIRM).performClick()

        composeRule.runOnIdle {
            assertEquals(1, signOuts)
            assertEquals(0, closed)
        }
    }

    // Both fears the dialog answers, in the order somebody has them: the work
    // first, what they paid for second.
    @Test
    fun theQuestion_answersBothFears() {
        setContent(signedIn())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.account_sign_out_body_1)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.account_sign_out_body_2)).assertIsDisplayed()
    }

    @Test
    fun asAGuest_thereIsNothingToSignOutOf() {
        setContent(guest())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT).assertDoesNotExist()
    }

    // The community block is the whole point of the profile: it shows what
    // strangers see, and the email is deliberately not part of that.
    @Test
    fun signedIn_showsTheAuthorRowTheCommunityWillSee() {
        setContent(signedIn())

        // The name sits inside the clickable row that opens the editor, so it
        // only exists as its own node in the unmerged tree.
        composeRule.onNodeWithTag(AccountTestTags.NAME, useUnmergedTree = true)
            .assertTextEquals("Olena M.")
        composeRule.onNodeWithTag(AccountTestTags.EDIT_NAME).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.COMMUNITY_ROW).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editingTheName_handsBackTheTrimmedValue() {
        setContent(signedIn())

        composeRule.onNodeWithTag(AccountTestTags.EDIT_NAME).performClick()
        composeRule.onNodeWithTag(AccountTestTags.NICKNAME_FIELD).performTextReplacement("  Grace  ")
        composeRule.onNodeWithTag(AccountTestTags.NICKNAME_SAVE).performClick()

        composeRule.runOnIdle { assertEquals(listOf("Grace"), names) }
    }

    // An author with no name is not an author anyone can find.
    @Test
    fun anEmptyName_cannotBeSaved() {
        setContent(signedIn())

        composeRule.onNodeWithTag(AccountTestTags.EDIT_NAME).performClick()
        composeRule.onNodeWithTag(AccountTestTags.NICKNAME_FIELD).performTextClearance()

        composeRule.onNodeWithTag(AccountTestTags.NICKNAME_SAVE).assertIsNotEnabled()
    }

    // Neither panel is right until Firebase answers, and picking one to show
    // meanwhile means showing the wrong one and swapping it.
    @Test
    fun beforeFirebaseAnswers_neitherPanelIsShown() {
        setContent(AccountUiState())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountTestTags.EMAIL).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountTestTags.CLOSE).assertIsDisplayed()
    }

    // A picture that will not open here can still be used as it is: nothing was moved, so no crop.
    @Test
    fun aPictureBeingPlaced_canBeUsedOrLeft() {
        setContent(signedIn().copy(positioning = FaceToPosition("content://nowhere/1", fromGallery = true)))

        composeRule.onNodeWithTag(AccountTestTags.FACE_POSITIONER).assertIsDisplayed()
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodesWithText(string(R.string.account_face_position_failed)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AccountTestTags.FACE_POSITION_CONFIRM).performClick()
        composeRule.onNodeWithTag(AccountTestTags.FACE_POSITION_CANCEL).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf<FaceCrop?>(null), confirmedCrops)
            assertEquals(1, positioningCancelled)
        }
    }

    @Test
    fun theCross_closesTheScreen() {
        setContent(guest())

        composeRule.onNodeWithTag(AccountTestTags.CLOSE).performClick()

        composeRule.runOnIdle { assertEquals(1, closed) }
    }
}
