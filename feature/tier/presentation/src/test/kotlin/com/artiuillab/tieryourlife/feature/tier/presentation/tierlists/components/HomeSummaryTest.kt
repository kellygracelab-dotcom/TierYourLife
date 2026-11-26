package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Walkthrough 26.11, item 6: Your lists said "private" with two of them out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class HomeSummaryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun someOut_saysHowMany() {
        composeRule.setContent { TierYourLifeTheme { HomeHeader(totalListCount = 6, rankedCount = 481, publicCount = 2) } }

        composeRule.onNodeWithTag(TierListsTestTags.SUMMARY).assertTextEquals("6 lists · 2 public")
    }

    @Test
    fun noneOut_staysPrivate() {
        composeRule.setContent { TierYourLifeTheme { HomeHeader(totalListCount = 6, rankedCount = 481, publicCount = 0) } }

        composeRule.onNodeWithTag(TierListsTestTags.SUMMARY).assertTextEquals("6 lists · 481 placed · private")
    }
}
