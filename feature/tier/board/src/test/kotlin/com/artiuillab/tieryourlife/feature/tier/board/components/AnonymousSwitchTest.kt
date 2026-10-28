package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Publish anonymously" sits under Public, for anybody who can publish, and says what it does. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class AnonymousSwitchTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val asked = mutableListOf<Boolean>()

    private fun show(list: TierList = previewTierList, signedIn: Boolean = true) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierListSettingsScreenContent(
                    list = list,
                    onBack = {},
                    onAddTier = { _, _, _, _ -> },
                    onSetDisplayMode = {},
                    signedIn = signedIn,
                    onSetAnonymous = { asked += it },
                )
            }
        }
    }

    @Test
    fun theSwitch_isOffForANamedList_andAsksToGoAnonymous() {
        show()

        composeRule.onNodeWithText("Publish anonymously").assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.ANONYMOUS_SWITCH).assertIsOff().performClick()

        composeRule.runOnIdle { assertEquals(listOf(true), asked) }
    }

    @Test
    fun anAnonymousList_showsTheSwitchOn() {
        show(previewTierList.copy(anonymous = true))

        composeRule.onNodeWithTag(TierDetailTestTags.ANONYMOUS_SWITCH).assertIsOn()
    }

    @Test
    fun aGuest_cannotPublish_soIsNotAsked() {
        show(signedIn = false)

        composeRule.onNodeWithTag(TierDetailTestTags.ANONYMOUS_SWITCH).assertDoesNotExist()
    }

    @Test
    fun aRankingOfSomebodysList_isNotOfferedIt() {
        show(previewTierList.copy(rankingOf = "their-list", authorName = "Olena"))

        composeRule.onNodeWithTag(TierDetailTestTags.ANONYMOUS_SWITCH).assertDoesNotExist()
    }
}
