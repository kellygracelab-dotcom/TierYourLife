package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var closed = 0
    private var signInRequests = 0
    private var signOuts = 0

    private fun setContent(state: AccountUiState) {
        closed = 0
        signInRequests = 0
        signOuts = 0
        composeRule.setContent {
            TierYourLifeTheme {
                AccountScreenContent(
                    state = state,
                    onClose = { closed++ },
                    onSignIn = { signInRequests++ },
                    onSignOut = { signOuts++ },
                )
            }
        }
    }

    @Test
    fun asAGuest_theOfferAndItsThreeReasonsAreShown() {
        setContent(AccountUiState())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.reason(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.reason(1)).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.reason(2)).assertIsDisplayed()
    }

    // Declining has to be as easy to reach as accepting, and it leaves without
    // asking anything further.
    @Test
    fun notNow_closesTheScreen_withoutSigningIn() {
        setContent(AccountUiState())

        composeRule.onNodeWithTag(AccountTestTags.NOT_NOW).performClick()

        composeRule.runOnIdle {
            assertEquals(1, closed)
            assertEquals(0, signInRequests)
        }
    }

    @Test
    fun signIn_asksOnce() {
        setContent(AccountUiState())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).performClick()

        composeRule.runOnIdle { assertEquals(1, signInRequests) }
    }

    // The picker is a separate window; a second tap behind it would open a
    // second one.
    @Test
    fun signIn_isDisabledWhileThePickerIsOpen() {
        setContent(AccountUiState(signingIn = true))

        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertIsNotEnabled()
    }

    @Test
    fun onceSignedIn_theOfferIsReplacedByTheAccountItself() {
        setContent(
            AccountUiState(
                account = Account.SignedIn(email = "someone@example.com", photoUrl = null),
                credits = 12,
            ),
        )

        composeRule.onNodeWithTag(AccountTestTags.EMAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.CREDITS).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.DONE).assertIsDisplayed()
        composeRule.onNodeWithTag(AccountTestTags.SIGN_IN).assertDoesNotExist()
    }

    // Nothing is counted in the stub build, and a zero would read as bad news
    // on the one screen whose job is to say the account was worth something.
    @Test
    fun signedIn_withNothingCounted_showsNoBalanceAtAll() {
        setContent(
            AccountUiState(
                account = Account.SignedIn(email = "someone@example.com", photoUrl = null),
                credits = null,
            ),
        )

        composeRule.onNodeWithTag(AccountTestTags.CREDITS).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountTestTags.EMAIL).assertIsDisplayed()
    }

    @Test
    fun signedIn_offersSignOut_andActsWithoutConfirmation() {
        setContent(
            AccountUiState(
                account = Account.SignedIn(email = "someone@example.com", photoUrl = null),
                credits = 12,
            ),
        )

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT).performClick()

        composeRule.runOnIdle {
            assertEquals(1, signOuts)
            assertEquals(0, closed)
        }
    }

    @Test
    fun asAGuest_thereIsNothingToSignOutOf() {
        setContent(AccountUiState())

        composeRule.onNodeWithTag(AccountTestTags.SIGN_OUT).assertDoesNotExist()
    }

    @Test
    fun theCross_closesTheScreen() {
        setContent(AccountUiState())

        composeRule.onNodeWithTag(AccountTestTags.CLOSE).performClick()

        composeRule.runOnIdle { assertEquals(1, closed) }
    }
}
