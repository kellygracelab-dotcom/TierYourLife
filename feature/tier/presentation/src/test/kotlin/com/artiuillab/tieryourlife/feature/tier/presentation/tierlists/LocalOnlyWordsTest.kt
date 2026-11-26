package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Coordinator, 26.11.2026: a window with a rail is a tablet or an opened fold, not a phone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w1280dp-h800dp")
class LocalOnlyWordsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWideWindow_saysDevice() {
        show(WindowShape.Wide, boards = 5)

        composeRule.onNodeWithText("5 lists, on this device only").assertExists()
        composeRule.onNodeWithText("Sign in to keep them when you change devices.").assertExists()
    }

    @Test
    fun aMediumWindow_withOneBoard_saysDevice() {
        show(WindowShape.Medium, boards = 1)

        composeRule.onNodeWithText("Sign in to keep it when you change devices.").assertExists()
    }

    @Test
    fun aNarrowWindow_stillSaysPhone() {
        show(WindowShape.Narrow, boards = 2)

        composeRule.onNodeWithText("2 lists, on this phone only").assertExists()
    }

    private fun show(shape: WindowShape, boards: Int) {
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides shape) {
                    TierListsScreenContent(
                        state = TierListsUiState.Success(
                            lists = (1..boards).map(::board),
                            totalListCount = boards,
                            rankedCount = 0,
                            localOnly = LocalOnly.Here(offerSignIn = true),
                        ),
                        onTierListClick = {},
                    )
                }
            }
        }
    }

    private fun board(id: Int) = TierListSummary(id = id.toLong(), title = "Board $id", tiers = emptyList(), editedAt = id.toLong())
}
