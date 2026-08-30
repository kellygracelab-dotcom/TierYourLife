package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the list screen says about where these boards are kept.
 *
 * The rule underneath every case: the card is asked once and the footer is a
 * fact. Somebody signed in should see neither, and somebody with nothing to
 * lose should not be sold an account.
 */
@RunWith(AndroidJUnit4::class)
class LocalOnlyTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aGuestWithABoard_isOfferedAnAccountOnceAndToldWhereItLives() {
        setScreen(LocalOnly.Here(offerSignIn = true), boards = 1)

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.local_only_card_title_one)).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_FOOTER).assertIsDisplayed()
    }

    @Test
    fun severalBoards_saySoInThePlural() {
        setScreen(LocalOnly.Here(offerSignIn = true), boards = 3)

        composeRule.onNodeWithText(string(R.string.local_only_card_title_many)).assertIsDisplayed()
    }

    // The card is the part that asks for something. Once it has been answered
    // the fact stays and the question does not come back.
    @Test
    fun onceTheOfferIsAnswered_onlyTheFactRemains() {
        setScreen(LocalOnly.Here(offerSignIn = false), boards = 2)

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_FOOTER).assertIsDisplayed()
    }

    // "on this phone only" is a warning about being in one place. Once the
    // boards are kept it is simply false, so there is nothing to replace it
    // with -- a state that is fine does not need reporting.
    @Test
    fun signedIn_saysNothingAtAll() {
        setScreen(LocalOnly.Kept, boards = 2)

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_FOOTER).assertDoesNotExist()
    }

    // Before Firebase answers, neither state is known. Treating unknown as
    // "guest" flashed the line on every start for somebody who is signed in.
    @Test
    fun beforeTheAccountIsKnown_nothingIsClaimed() {
        setScreen(LocalOnly.Unknown, boards = 2)

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_FOOTER).assertDoesNotExist()
    }

    @Test
    fun signingInFromTheCard_reachesTheCaller() {
        var calls = 0
        setScreen(LocalOnly.Here(offerSignIn = true), boards = 1, onSignInClick = { calls++ })

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_SIGN_IN).performClick()

        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun notNow_reachesTheCaller() {
        var calls = 0
        setScreen(LocalOnly.Here(offerSignIn = true), boards = 1, onDismissSignInOffer = { calls++ })

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_DISMISS).performClick()

        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    // Two sets of buttons asking different questions is one too many, and the
    // offer can wait until the person has finished deleting things.
    @Test
    fun whilePickingBoardsToDelete_theOfferStaysOutOfTheWay() {
        setScreen(
            localOnly = LocalOnly.Here(offerSignIn = true),
            boards = 2,
            mode = HomeMode.Selecting(setOf(1L)),
        )

        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_CARD).assertDoesNotExist()
    }

    private fun setScreen(
        localOnly: LocalOnly,
        boards: Int,
        mode: HomeMode = HomeMode.Browsing,
        onSignInClick: () -> Unit = {},
        onDismissSignInOffer: () -> Unit = {},
    ) {
        val lists = List(boards) { index -> board(index + 1L, "Board ${index + 1}") }
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(
                    state = TierListsUiState.Success(
                        lists = lists,
                        totalListCount = lists.size,
                        rankedCount = lists.size,
                        mode = mode,
                        localOnly = localOnly,
                    ),
                    onTierListClick = {},
                    onSignInClick = onSignInClick,
                    onDismissSignInOffer = onDismissSignInOffer,
                )
            }
        }
    }

    private fun board(id: Long, title: String) = TierList(
        id = id,
        title = title,
        tiers = listOf(
            Tier(
                id = id * 10,
                label = "S",
                colorLight = "#B03A32",
                colorDark = "#F1948C",
                items = listOf(TierItem(id = id * 100, title = "Arrival", imageUrl = null)),
            ),
        ),
    )

    private fun string(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
