package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.MoveItemSheetContent
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A card's menu names whom its picture is by, when the picture's license asks for it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class PhotoCreditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(item: TierItem) {
        val ranked = previewTierList.tiers.filterNot { it.isPool }
        composeRule.setContent {
            TierYourLifeTheme {
                MoveItemSheetContent(
                    item = item,
                    currentTierId = ranked.first().id,
                    rankedTiers = ranked,
                    pool = previewTierList.tiers.first { it.isPool },
                    onMoveToTier = { _, _ -> },
                    onRemove = {},
                )
            }
        }
    }

    @Test
    fun aCreditedCard_saysWhoThePhotoIsBy() {
        show(TierItem(id = 1, title = "Red fox", imageUrl = null, credit = "© Jane Doe, CC BY"))

        composeRule.onNodeWithText("Photo: © Jane Doe, CC BY").assertExists()
    }

    @Test
    fun aCardWithoutACredit_saysNothingAboutItsPhoto() {
        show(TierItem(id = 1, title = "Arrival", imageUrl = null))

        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_CREDIT).assertDoesNotExist()
    }
}
