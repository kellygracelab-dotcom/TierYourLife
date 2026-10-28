package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureProblem
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

/** Every refusal the server can give, in words, and where the form puts it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class ProposalErrorTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val until = Calendar.getInstance(TimeZone.getDefault()).apply { set(2027, Calendar.MARCH, 14, 12, 0) }.timeInMillis

    private val everyError = listOf(
        ProposalError.BadName,
        ProposalError.NameTaken("food"),
        ProposalError.BadIcon,
        ProposalError.BadFileUnder,
        ProposalError.BadPicture(PictureProblem.NotImage),
        ProposalError.TooManyPending,
        ProposalError.TooManyToday,
        ProposalError.Banned(untilMillis = until),
        ProposalError.Banned(untilMillis = null),
        ProposalError.NotSignedIn,
        ProposalError.Failed,
    )

    @Test
    fun everyRefusal_hasWordsOfItsOwn() {
        val words = mutableMapOf<ProposalError, String>()
        composeRule.setContent {
            TierYourLifeTheme { everyError.forEach { words[it] = proposalErrorText(it) } }
        }

        composeRule.runOnIdle {
            assertTrue(words.values.none { it.isBlank() })
            assertEquals(everyError.size, words.values.toSet().size)
            assertEquals("You can’t propose sections until March 14, 2027.", words[ProposalError.Banned(until)])
            assertEquals("That’s ten proposals today. Try again tomorrow.", words[ProposalError.TooManyToday])
        }
    }

    @Test
    fun aRefusalThatTryingAgainCanFix_offersTryAgain() {
        var proposed = 0
        val ready = ProposeSectionUiState(
            signedIn = true,
            name = "Street food",
            picture = PickedPicture("content://picture", PictureSize(2000, 1250)),
            fileUnder = ListCategory.Food.id,
            error = ProposalError.Failed,
        )
        composeRule.setContent {
            TierYourLifeTheme { ProposeSectionContent(ready, onClose = {}, calls = ProposeCallbacks(onPropose = { proposed++ })) }
        }

        composeRule.onNodeWithTag(ProposeTestTags.ERROR).assertExists()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, proposed) }
    }

    @Test
    fun aGuest_seesSignInFirst_andNoForm() {
        var signIn = 0
        composeRule.setContent {
            TierYourLifeTheme { ProposeSectionContent(ProposeSectionUiState(signedIn = false), onClose = {}, onSignIn = { signIn++ }) }
        }

        composeRule.onNodeWithTag(ProposeTestTags.NAME).assertDoesNotExist()
        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.runOnIdle { assertEquals(1, signIn) }
    }
}
