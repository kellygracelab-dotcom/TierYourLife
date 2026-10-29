package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Show my ranking to others" in the list's menu: once arranged, never on one's own list, and a guest signs in first. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class ShowMyRankingMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Every A24 film",
        tiers = listOf(
            Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Moonlight", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = emptyList()),
        ),
    )
    private val arranged = CommunityListUiState.Success(list = board, authorName = "Olena M.", authorUid = "olena", arranged = true, signedIn = true)

    private var shown = 0
    private var signIns = 0

    private fun openMenu(state: CommunityListUiState.Success, notice: RankingNotice? = null) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = state,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onShowMyRanking = { shown++ },
                    onSignIn = { signIns++ },
                    rankingNotices = notice?.let { flowOf(it) } ?: flowOf(),
                )
            }
        }
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
    }

    @Test
    fun arrangedMine_offersToShowIt_andTappingSendsIt() {
        openMenu(arranged)

        composeRule.onNodeWithText("Show my ranking to others").performClick()

        composeRule.runOnIdle { assertEquals(1, shown) }
    }

    @Test
    fun nothingArranged_offersNothing() {
        openMenu(arranged.copy(arranged = false))

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_SHOW_RANKING).assertDoesNotExist()
    }

    @Test
    fun aGuest_isOfferedToSignInInstead() {
        openMenu(arranged.copy(signedIn = false))

        composeRule.onNodeWithText("Sign in to show your ranking").performClick()

        composeRule.runOnIdle {
            assertEquals(1, signIns)
            assertEquals(0, shown)
        }
    }

    @Test
    fun theirOwnList_isNotOfferedIt() {
        openMenu(arranged.copy(authorUid = "me", own = true))

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_SHOW_RANKING).assertDoesNotExist()
    }

    @Test
    fun aRankingShown_saysWhereItIs() {
        openMenu(arranged, notice = RankingNotice.Shown)

        composeRule.onNodeWithText("Your ranking is in the list’s rankings").assertExists()
    }
}
