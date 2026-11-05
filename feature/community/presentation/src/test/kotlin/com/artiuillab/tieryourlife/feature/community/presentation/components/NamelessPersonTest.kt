package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.author.AuthorScreenContent
import com.artiuillab.tieryourlife.feature.community.presentation.author.AuthorTestTags
import com.artiuillab.tieryourlife.feature.community.presentation.author.AuthorUiState
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A person who never gave a name is "Someone", on a card and on their own page, and never a blank. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class NamelessPersonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aCardByANamelessPerson_saysSomeone() {
        val summary = PublishedListSummary(
            id = "l-1",
            title = "Street food in Lviv",
            authorUid = "u-1",
            authorName = "",
            category = "food",
            itemCount = 12,
            updatedAtMillis = 0,
        )
        composeRule.setContent { TierYourLifeTheme { FeedCard(summary, onClick = {}, onLongClick = {}) } }

        composeRule.onNodeWithText("Someone").assertExists()
    }

    @Test
    fun theirPage_isHeadedSomeone_andSoIsItsMenu() {
        composeRule.setContent {
            TierYourLifeTheme {
                AuthorScreenContent(
                    state = AuthorUiState.Ready(name = "", photoUrl = null, lists = emptyList()),
                    onBack = {},
                    onOpenList = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(AuthorTestTags.NAME).assertTextEquals("Someone")
        composeRule.onNodeWithTag(AuthorTestTags.MORE).performClick()
        composeRule.onNodeWithText("Hide everything from Someone").assertExists()
    }
}
