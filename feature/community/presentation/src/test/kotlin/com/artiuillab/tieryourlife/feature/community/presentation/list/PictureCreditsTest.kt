package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A published list names whom its pictures are by at the end, one line per credited card, and says nothing when none is. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class PictureCreditsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun board(vararg cards: TierItem) = TierList(
        id = 0,
        title = "Animals",
        tiers = listOf(
            Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = cards.toList()),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = emptyList()),
        ),
    )

    private fun show(list: TierList) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = CommunityListUiState.Success(list = list, authorName = "Olena M.", authorUid = "olena"),
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun creditedCards_doNotTakeSpaceUnderTheRows() {
        show(board(TierItem(1, "Red fox", null, credit = "© Jane Doe, CC BY"), TierItem(2, "Badger", null)))

        composeRule.onNodeWithTag(CommunityListTestTags.PICTURE_CREDITS).assertDoesNotExist()
        composeRule.onNodeWithText("Red fox — © Jane Doe, CC BY").assertDoesNotExist()
        composeRule.onNodeWithText("Badger —", substring = true).assertDoesNotExist()
    }

    @Test
    fun aListWithNoCreditedPicture_hasNoCreditsBlock() {
        show(board(TierItem(1, "Moonlight", null)))

        composeRule.onNodeWithTag(CommunityListTestTags.PICTURE_CREDITS).assertDoesNotExist()
    }
}
