package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Someone else's list has no Add in its pool, on either half: one's own cards go on one's own copy, as on the website. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class ReaderPoolAddTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Pizza styles",
        tiers = listOf(
            Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Neapolitan", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = listOf(TierItem(2, "Detroit", null))),
        ),
    )

    private fun show(showing: Showing) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = CommunityListUiState.Success(list = board, authorName = "Olena M.", showing = showing, arranged = showing == Showing.Mine),
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun mine_hasNoAdd() {
        show(Showing.Mine)

        composeRule.onNodeWithTag(ADD_CHIP).assertDoesNotExist()
    }

    @Test
    fun theirs_hasNoAdd() {
        show(Showing.Theirs)

        composeRule.onNodeWithTag(ADD_CHIP).assertDoesNotExist()
    }

    private companion object {
        const val ADD_CHIP = "tier_detail_add_chip"
    }
}
