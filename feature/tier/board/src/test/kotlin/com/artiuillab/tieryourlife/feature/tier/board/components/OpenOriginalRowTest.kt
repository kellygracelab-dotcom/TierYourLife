package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A copy's way to its original stays in the list settings, with the link under the title hidden or not. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class OpenOriginalRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = 0

    private fun show(onOpenOriginal: (() -> Unit)?) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierListSettingsScreenContent(
                    list = previewTierList.copy(takenFrom = "abc"),
                    onBack = {},
                    onAddTier = { _, _, _, _ -> },
                    onSetDisplayMode = {},
                    onOpenOriginal = onOpenOriginal,
                )
            }
        }
    }

    @Test
    fun aCopy_opensTheListItWasMadeFrom() {
        show(onOpenOriginal = { opened++ })

        composeRule.onNodeWithText("Open the original").assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.OPEN_ORIGINAL_ROW).performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun aBoardThatIsNobodysCopy_hasNoOriginal() {
        show(onOpenOriginal = null)

        composeRule.onNodeWithTag(TierDetailTestTags.OPEN_ORIGINAL_ROW).assertDoesNotExist()
    }
}
