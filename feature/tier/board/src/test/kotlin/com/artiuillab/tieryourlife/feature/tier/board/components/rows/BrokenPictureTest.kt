package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** QA 14.11: a picture its source took away leaves the card's name, as on the website, not an empty tile. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class BrokenPictureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPictureThatWillNotLoad_showsTheCardsName() {
        composeRule.setContent {
            TierYourLifeTheme {
                ItemTile(item = TierItem(1, "Spirited Away", "file:///gone/spirited-away.jpg"), width = 72.dp, height = 96.dp)
            }
        }

        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Spirited Away").fetchSemanticsNodes().isNotEmpty() }
    }
}
