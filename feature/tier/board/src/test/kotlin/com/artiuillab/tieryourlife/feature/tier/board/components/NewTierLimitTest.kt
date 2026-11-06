package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The account keeps 20 tiers on a board, the pool among them; one more and the board never backed up again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class NewTierLimitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(tiers: Int) {
        val list = previewTierList.copy(
            tiers = List(tiers) { index ->
                Tier(id = index.toLong(), label = "T$index", colorLight = "#111111", colorDark = "#EEEEEE", items = emptyList(), isPool = index == tiers - 1)
            },
        )
        composeRule.setContent {
            TierYourLifeTheme {
                TierListSettingsScreenContent(list = list, onBack = {}, onAddTier = { _, _, _, _ -> }, onSetDisplayMode = {})
            }
        }
    }

    @Test
    fun aBoardWithRoom_offersANewTier() {
        show(tiers = TierList.MAX_TIERS - 1)

        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).assertHasClickAction()
        composeRule.onNodeWithText("18 tiers").assertExists()
    }

    @Test
    fun aFullBoard_saysWhyThereIsNoNewTier() {
        show(tiers = TierList.MAX_TIERS)

        composeRule.onNodeWithTag(TierDetailTestTags.NEW_TIER_ROW).assertIsNotEnabled()
        composeRule.onNodeWithText("20 tiers with the pool: the most a board holds").assertExists()
    }
}
