package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The board's bar (walkthrough 26.11.2026): the same three things always,
 * Publish or Share in one place, Play and the menu; folded, icons, so the
 * title fits; someone else's list has Play, Save and Share under its title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class OwnBarActionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var state: TopAppBarState
    private val play = PlayButton(cards = 12, saved = false, onClick = {})
    private val publish = PublishButton(Publishing.Publish, primary = false, narrow = true, busy = false, onClick = {})

    @Test
    fun aPublishedList_hasShareWhereImPublishWouldBe() {
        show(publish = null, share = {})

        // With its word: wider than an icon's 48.
        assertTrue(widthOf(TierDetailTestTags.SHARE_LINK) > 48f)
        assertEquals(0, count(TierDetailTestTags.PUBLISH))
        capture("own_bar_share")
    }

    @Test
    fun somethingToPublish_takesTheLinksPlace() {
        show(publish = publish, share = {})

        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertExists()
        assertEquals(0, count(TierDetailTestTags.SHARE_LINK))
    }

    @Test
    fun folded_theActionsAreIcons_andTheTitleIsUp() {
        show(publish = null, share = {})
        fold()

        // Up in the bar, beside the one folded away under it.
        assertEquals(2, composeRule.onAllNodes(hasText("Movies I’ve watched: my tier list")).fetchSemanticsNodes().size)
        assertEquals(48f, widthOf(TierDetailTestTags.SHARE_LINK), 0.5f)
        assertEquals(48f, widthOf(TierDetailTestTags.PLAY), 0.5f)
        capture("own_bar_folded")
    }

    @Test
    fun foldedWithSomethingToPublish_keepsPublishAsItsIcon() {
        show(publish = publish, share = null)
        fold()

        assertEquals(48f, widthOf(TierDetailTestTags.PUBLISH), 0.5f)
    }

    @Test
    fun someoneElsesList_hasPlaySaveAndShareUnderItsTitle() {
        show(publish = null, share = null, reader = true)

        val row = composeRule.onNodeWithTag(TierDetailTestTags.READER_ACTIONS)
        row.assertExists()
        assertEquals(1, count(TierDetailTestTags.PLAY))
        val playTop = composeRule.onNodeWithTag(TierDetailTestTags.PLAY).fetchSemanticsNode().positionInRoot.y
        val titleTop = composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).fetchSemanticsNode().positionInRoot.y
        assertTrue(playTop > titleTop)
        composeRule.onNodeWithText("Save my list").assertExists()
        capture("reader_bar_actions")
    }

    @Test
    fun someoneElsesList_folded_bringsPlayUpAsItsTriangle() {
        show(publish = null, share = null, reader = true)
        fold()

        // The triangle in the bar, and the worded one folded away with the row.
        assertEquals(2, count(TierDetailTestTags.PLAY))
    }

    private fun show(publish: PublishButton?, share: (() -> Unit)?, reader: Boolean = false) {
        composeRule.setContent {
            state = rememberTopAppBarState()
            TierYourLifeTheme(darkTheme = true) {
                OwnBoardTopBar(
                    title = "Movies I’ve watched: my tier list",
                    counts = "Film & TV · 31 cards",
                    state = state,
                    onBack = {},
                    onMoreClick = {},
                    onRenameList = null,
                    play = play,
                    publish = publish,
                    shareLink = share,
                    readerActions = if (reader) {
                        {
                            Text("Save my list")
                            Spacer(Modifier.width(8.dp))
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun fold() {
        composeRule.runOnIdle { state.heightOffset = state.heightOffsetLimit }
        composeRule.waitForIdle()
    }

    private fun widthOf(tag: String): Float = with(composeRule.density) {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().size.width.toDp().value
    }

    private fun count(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            "src/test/screenshots/$name.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F)),
        )
    }
}
