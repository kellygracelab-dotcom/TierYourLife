package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 26.11: the board's bar lost its camera; Add → Your photo is the one way to a card of one's own picture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h640dp")
class AddYourOwnTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun yourPhoto_opensTheOwnCardDialog() {
        var ownPhoto = 0
        composeRule.setContent {
            TierYourLifeTheme { AddYourOwn(fill = Color.Transparent, onPasteLink = {}, onOwnPhoto = { ownPhoto++ }, onJustAName = {}) }
        }

        composeRule.onNodeWithTag(CatalogueSearchTestTags.ITEM_SEARCH_OWN_PHOTO).performClick()

        composeRule.runOnIdle { assertEquals(1, ownPhoto) }
    }
}
