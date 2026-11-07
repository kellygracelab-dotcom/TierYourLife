package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class CardCreditsTest {
    @get:Rule val compose = createComposeRule()

    private fun show(credit: String?, author: String = "Olena", editorial: Boolean = false, byNobody: Boolean = false) {
        val tier = Tier(1, "S", "B03A32", "F1948C", items = listOf(TierItem(10, "Red fox", null, credit = credit)))
        val pool = Tier(2, "Pool", "DAD7E0", "46464F", items = emptyList(), isPool = true)
        compose.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = CommunityListUiState.Success(
                        list = TierList(1, "Wildlife", listOf(tier, pool)),
                        authorName = author, authorUid = "olena", signedIn = false, editorial = editorial, byNobody = byNobody,
                    ),
                    onBack = {}, onMoveItem = { _, _, _ -> }, onSave = {}, onRetry = {},
                )
            }
        }
    }

    @Test
    fun aGuestCanOpenFullCreditsFromTheCardsAccessibleAction_withoutABoardFooter() {
        val credit = "© Jane Doe · CC BY 4.0 · https://example.org/source"
        show(credit)
        compose.onNodeWithTag(CommunityListTestTags.PICTURE_CREDITS).assertDoesNotExist()
        compose.onNodeWithTag(TierDetailTestTags.tile(10)).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_CREDIT).performClick()
        compose.onNodeWithText(credit).assertExists()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag(TierDetailTestTags.IMAGE_CREDITS_TEXT).assertDoesNotExist()
        compose.onNodeWithTag(TierDetailTestTags.MOVE_SHEET).assertExists()
    }

    @Test
    fun aCardWithoutCredits_hasNoCreditsAction() {
        show(null)
        compose.onNodeWithTag(TierDetailTestTags.tile(10)).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_CREDIT).assertDoesNotExist()
    }

    @Test
    fun aBlankCredit_hasNoCreditsAction() {
        show("  ")
        compose.onNodeWithTag(TierDetailTestTags.tile(10)).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag(TierDetailTestTags.MOVE_SHEET_CREDIT).assertDoesNotExist()
    }

    @Test
    fun anAnonymousList_hasNoEmptyAuthorLine() {
        show(null, author = "", editorial = true, byNobody = true)
        compose.onNodeWithText("Anonymous").assertDoesNotExist()
        compose.onNodeWithTag(CommunityListTestTags.ANONYMOUS_AUTHOR).assertDoesNotExist()
    }

    @Test
    fun aHouseList_hasNoAuthorPlaceholder() {
        show(null, author = "TierYourLife", editorial = true)
        compose.onNodeWithText("TierYourLife").assertDoesNotExist()
        compose.onNodeWithText("Anonymous").assertDoesNotExist()
    }

    @Test
    fun aNamedList_keepsItsRealAuthor() {
        show(null)
        compose.onNodeWithText("Olena", substring = true).assertExists()
    }
}
