package com.artiuillab.tieryourlife.feature.tier.board.share

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Your list is live": a first publish ends on the link, not on a passing word. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class LiveSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pressed = mutableListOf<String>()

    private fun calls(open: Boolean = true) = LiveSheetCalls(
        onShare = { pressed += "share" },
        onCopy = { pressed += "copy" },
        onImage = { pressed += "image" },
        onOpen = if (open) ({ pressed += "open" }) else null,
        onDone = { pressed += "done" },
    )

    @Test
    fun itSaysWhereTheListIs_andEachWayToSendIt() {
        composeRule.setContent {
            TierYourLifeTheme { LiveSheet(section = "Games", link = "https://tieryourlife.com/l/pub-1", calls = calls()) }
        }

        composeRule.onNodeWithText("Your list is live").assertExists()
        composeRule.onNodeWithText("Anyone with the link can see it. It is also in Community → Games.").assertExists()
        composeRule.onNodeWithText("tieryourlife.com/l/pub-1").assertExists()
        listOf(TierDetailTestTags.LIVE_SHARE, TierDetailTestTags.LIVE_COPY, TierDetailTestTags.LIVE_IMAGE, TierDetailTestTags.LIVE_OPEN, TierDetailTestTags.LIVE_DONE)
            .forEach { composeRule.onNodeWithTag(it).performClick() }

        assertEquals(listOf("share", "copy", "image", "open", "done"), pressed)
    }

    // The id may reach the board a frame after the word that it went out.
    @Test
    fun beforeTheLinkArrives_nothingSendsANonLink() {
        composeRule.setContent { TierYourLifeTheme { LiveSheet(section = "Games", link = null, calls = calls()) } }

        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_SHARE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_COPY).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_OPEN).assertDoesNotExist()

        assertEquals(emptyList<String>(), pressed)
    }

    @Test
    fun aFirstPublish_opensIt_butAnUpdateKeepsItsSnackbar() {
        val messages = MutableSharedFlow<UserMessage>(extraBufferCapacity = 1)
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(previewTierList.copy(publishedId = "pub-1", category = "film_tv")),
                    actions = TierDetailActions(onPublish = {}),
                    userMessages = messages,
                )
            }
        }

        messages.tryEmit(UserMessage.Published(updated = true))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_SHEET).assertDoesNotExist()

        messages.tryEmit(UserMessage.Published(updated = false))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_SHEET).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_DONE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.LIVE_SHEET).assertDoesNotExist()
    }
}
