package com.artiuillab.tieryourlife.feature.community.presentation.feed

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityFeedList
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorialCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aListThatStandsWithoutAnAuthor_showsNoAuthor_besideOneThatHasOne() {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityFeedList(
                    feed = CommunityFeed.Ready(
                        listOf(
                            summary(id = "people", authorUid = "author-1"),
                            summary(id = "editorial", authorUid = "85RRieyLyUPCEqHjdYCdxVlWxHx2"),
                            summary(id = "flagged", authorUid = "author-2", anonymous = true),
                            summary(id = "blank", authorUid = ""),
                        ),
                    ),
                    category = null,
                    onSelectCategory = {},
                    onOpen = {},
                    onRetry = {},
                    onOpenAuthor = {},
                    // The category tiles would push the cards below a small emulator's fold.
                    showCategories = false,
                )
            }
        }

        // Every card was composed, so a missing author below is missing, not off the grid.
        listOf("people", "editorial", "flagged", "blank").forEach { id ->
            composeRule.onNodeWithTag(CommunityTestTags.communityCard(id)).assertExists()
        }
        composeRule.onNodeWithTag(CommunityTestTags.communityCardAuthor("people")).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.communityCardAuthor("editorial")).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.communityCardAuthor("flagged")).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.communityCardAuthor("blank")).assertDoesNotExist()
    }

    private fun summary(id: String, authorUid: String, anonymous: Boolean = false) = PublishedListSummary(
        id = id,
        title = "List $id",
        authorUid = authorUid,
        authorName = "Danylo",
        category = ListCategory.FilmTv,
        itemCount = 10,
        updatedAtMillis = 0,
        anonymous = anonymous,
    )
}
