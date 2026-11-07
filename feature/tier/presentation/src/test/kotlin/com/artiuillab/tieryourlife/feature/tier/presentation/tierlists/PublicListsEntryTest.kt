package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class PublicListsEntryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun publicationsAreReachableEvenWithNoBoardsOnThisDevice() {
        var opened = 0
        compose.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(
                    state = TierListsUiState.Success(emptyList(), 0, 0, localOnly = LocalOnly.Kept),
                    onTierListClick = {}, onOpenPublished = { opened++ },
                )
            }
        }
        compose.onNodeWithTag(TierListsTestTags.PUBLIC_LISTS).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun aGuestIsNotOfferedAccountPublications() {
        compose.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(TierListsUiState.Success(emptyList(), 0, 0), onTierListClick = {})
            }
        }
        compose.onNodeWithTag(TierListsTestTags.PUBLIC_LISTS).assertDoesNotExist()
    }
}
