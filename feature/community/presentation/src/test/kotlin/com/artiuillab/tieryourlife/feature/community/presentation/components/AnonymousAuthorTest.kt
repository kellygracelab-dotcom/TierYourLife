package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCard
import com.artiuillab.tieryourlife.feature.community.presentation.list.BasedOnLine
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A list out without its author's name says "Anonymous" where the author would be; the app's own lists still say nothing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class AnonymousAuthorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun card(authorUid: String, authorName: String, anonymous: Boolean) = PublishedListSummary(
        id = "l-1",
        title = "Street food in Lviv",
        authorUid = authorUid,
        authorName = authorName,
        category = "food",
        itemCount = 12,
        updatedAtMillis = 0,
        anonymous = anonymous,
    )

    private fun show(content: @Composable () -> Unit) = composeRule.setContent { TierYourLifeTheme { content() } }

    @Test
    fun somebodysAnonymousList_saysAnonymous_onItsCard() {
        show { FeedCard(card(authorUid = "", authorName = "", anonymous = true), onClick = {}, onLongClick = {}) }

        composeRule.onNodeWithText("Anonymous").assertExists()
    }

    @Test
    fun theAppsOwnList_staysWithoutAnAuthorRow() {
        show { FeedCard(card(authorUid = "85RRieyLyUPCEqHjdYCdxVlWxHx2", authorName = "TierYourLife", anonymous = false), onClick = {}, onLongClick = {}) }

        composeRule.onNodeWithText("Anonymous").assertDoesNotExist()
        composeRule.onNodeWithText("TierYourLife").assertDoesNotExist()
    }

    @Test
    fun aNamedList_keepsItsAuthor() {
        show { FeedCard(card(authorUid = "u-1", authorName = "olena", anonymous = false), onClick = {}, onLongClick = {}) }

        composeRule.onNodeWithText("olena").assertExists()
        composeRule.onNodeWithText("Anonymous").assertDoesNotExist()
    }

    @Test
    fun theAnonymousAuthorLine_offersNothingToFollow() {
        show { AnonymousAuthorLine() }

        composeRule.onNodeWithTag(CommunityListTestTags.ANONYMOUS_AUTHOR).assertExists()
        composeRule.onNodeWithText("Anonymous").assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.FOLLOW).assertDoesNotExist()
    }

    @Test
    fun aCopyOfAnAnonymousList_namesTheListAndNobodyBehindIt() {
        show { BasedOnLine(BasedOn(id = "src", title = "Films", authorName = ""), onOpen = {}) }

        composeRule.onNodeWithText("Based on “Films”").assertExists()
    }

    @Test
    fun aCopyOfANamedList_stillSaysWhose() {
        show { BasedOnLine(BasedOn(id = "src", title = "Films", authorName = "Olena", authorUid = "u-7"), onOpen = {}) }

        composeRule.onNodeWithText("Based on “Films” by Olena").assertExists()
    }
}
