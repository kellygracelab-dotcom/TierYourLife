package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A published board leads to its list as others see it: the readers' rankings and the link live there. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class SeeInCommunityRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0

    private fun show(onSeeInCommunity: (() -> Unit)?, publicPending: Boolean? = null) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierListSettingsScreenContent(
                    list = previewTierList.copy(publishedId = "abc"),
                    onBack = {},
                    onAddTier = { _, _, _, _ -> },
                    onSetDisplayMode = {},
                    signedIn = true,
                    publicPending = publicPending,
                    onSeeInCommunity = onSeeInCommunity,
                )
            }
        }
    }

    @Test
    fun aPublishedBoard_opensItsListInCommunity() {
        show(onSeeInCommunity = { opened++ })

        composeRule.onNodeWithText("See it in Community").assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.SEE_IN_COMMUNITY_ROW).performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun aBoardBeingTakenDown_offersNoWayThere() {
        show(onSeeInCommunity = { opened++ }, publicPending = false)

        composeRule.onNodeWithTag(TierDetailTestTags.SEE_IN_COMMUNITY_ROW).assertDoesNotExist()
    }

    @Test
    fun aBoardThatIsNotOut_offersNoWayThere() {
        show(onSeeInCommunity = null)

        composeRule.onNodeWithTag(TierDetailTestTags.SEE_IN_COMMUNITY_ROW).assertDoesNotExist()
    }
}
