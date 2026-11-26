package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A list's name typed and not yet confirmed is kept when the app goes to the
 * background, where it may be closed (seen on a fresh list, 26.11.2026).
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ListTitleDraftTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val renames = mutableListOf<String>()
    private val owner = object : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun aTypedName_isKeptWhenTheAppGoesAway_andTheFieldStaysOpen() {
        show()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performTextReplacement("  Road trips ")

        stop()

        assertEquals(listOf("Road trips"), renames)
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).assertTextEquals("  Road trips ")
    }

    @Test
    fun nothingTyped_renamesNothing() {
        show()
        composeRule.onNodeWithTag(TierDetailTestTags.HEADER_TITLE).performClick()
        composeRule.waitForIdle()

        stop()

        assertEquals(emptyList<String>(), renames)
    }

    @Test
    fun notEditing_renamesNothing() {
        show()

        stop()

        assertEquals(emptyList<String>(), renames)
    }

    private fun show() {
        owner.registry.currentState = Lifecycle.State.RESUMED
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TierYourLifeTheme {
                    OwnBoardTopBar(
                        title = "Untitled list",
                        counts = "0 cards",
                        state = rememberTopAppBarState(),
                        onBack = {},
                        onMoreClick = {},
                        onRenameList = { renames += it },
                        play = null,
                        publish = null,
                    )
                }
            }
        }
    }

    private fun stop() {
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        composeRule.waitForIdle()
    }
}
