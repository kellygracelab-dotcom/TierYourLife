package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ExploreTestTags
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCard
import com.artiuillab.tieryourlife.feature.community.presentation.list.BasedOnLine
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Whose a card is, at a glance: somebody else's anonymous list and the app's own show no author,
 * the reader's own says so with their face, and a named list keeps its author.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class AnonymousAuthorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun card(authorUid: String, authorName: String, anonymous: Boolean, mine: Boolean = false) = PublishedListSummary(
        id = "l-1",
        title = "Street food in Lviv",
        authorUid = authorUid,
        authorName = authorName,
        category = "food",
        itemCount = 12,
        updatedAtMillis = 0,
        anonymous = anonymous,
        mine = mine,
    )

    private fun show(content: @Composable () -> Unit) = composeRule.setContent { TierYourLifeTheme { content() } }

    @Test
    fun somebodysAnonymousList_hasNoAuthorRow() {
        show { FeedCard(card(authorUid = "", authorName = "", anonymous = true), onClick = {}, onLongClick = {}) }

        composeRule.onNodeWithTag(ExploreTestTags.cardAuthor("l-1")).assertDoesNotExist()
        composeRule.onNodeWithText("Anonymous").assertDoesNotExist()
    }

    @Test
    fun myAnonymousList_saysItIsMineAndAnonymous() {
        show {
            CompositionLocalProvider(LocalOwnFace provides OwnFace(name = "Danylo", photoUrl = null)) {
                FeedCard(card(authorUid = "", authorName = "", anonymous = true, mine = true), onClick = {}, onLongClick = {})
            }
        }

        composeRule.onNodeWithContentDescription("Your list, published anonymously").assertExists()
        composeRule.onNodeWithText("Your list · anonymous").assertDoesNotExist()
        composeRule.onNodeWithTag(ExploreTestTags.cardAuthor("l-1"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun myNamedList_saysItIsMine() {
        show { FeedCard(card(authorUid = "u-me", authorName = "Danylo", anonymous = false, mine = true), onClick = {}, onLongClick = {}) }

        composeRule.onNodeWithText("Your list").assertExists()
        composeRule.onNodeWithText("Danylo").assertDoesNotExist()
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
