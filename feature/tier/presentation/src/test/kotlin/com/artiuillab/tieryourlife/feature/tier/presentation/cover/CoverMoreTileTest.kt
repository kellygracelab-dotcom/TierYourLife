package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Coordinator, 26.11.2026: on a 720 x 748 cover at 300dpi "+53" dropped under seven cards; it stays on their line. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h399dp-300dpi")
class CoverMoreTileTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aBigTier_keepsItsMoreTileOnTheCardsLine() {
        val cards = (1..60).map { TierItem(id = 1_000L + it, title = "Card $it", imageUrl = null) }
        val list = TierList(id = 1, title = "QA Images 240", tiers = listOf(Tier(10, "A", "#C06A25", "#E9A867", cards)))
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides WindowShape.Cover) {
                    CoverListScreen(list = list, next = null, onBack = {}, onRankOnMain = {}, onCardOnMain = { _, _ -> }, onNext = {})
                }
            }
        }

        val first = composeRule.onNodeWithTag(CoverTestTags.card(cards.first().id)).fetchSemanticsNode().boundsInRoot
        val more = composeRule.onNodeWithTag(CoverTestTags.more(10)).fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, more.top)
    }
}
