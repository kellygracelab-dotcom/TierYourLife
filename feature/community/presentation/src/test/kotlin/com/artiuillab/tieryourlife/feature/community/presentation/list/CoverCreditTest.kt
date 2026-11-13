package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A house list's cover credit: a line in the list's menu, and the same "About the picture" a card has. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class CoverCreditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Wildlife",
        tiers = listOf(
            Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Red fox", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = emptyList()),
        ),
    )
    private val credit = CoverCredit(
        imageUrl = "https://example.org/fox.jpg",
        attribution = PictureAttribution(
            source = "web",
            author = "Jane Doe",
            license = "CC BY 4.0",
            licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
            link = "https://www.example.com/fox",
        ),
    )

    private fun show(cover: CoverCredit?) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = CommunityListUiState.Success(list = board, authorName = "TierYourLife", editorial = true, cover = cover),
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun aCreditedCover_isInTheMenu_andSaysWhoMadeIt() {
        show(credit)
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_ABOUT_COVER).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertDoesNotExist()
        composeRule.onNodeWithText("Jane Doe").assertExists()
        composeRule.onNodeWithText("CC BY 4.0").assertExists()
        composeRule.onNodeWithText("example.com").assertExists()

        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithText("Jane Doe").assertDoesNotExist()
    }

    @Test
    fun aCoverNobodyCredited_hasNoLine() {
        show(null)
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_ABOUT_COVER).assertDoesNotExist()
    }
}
