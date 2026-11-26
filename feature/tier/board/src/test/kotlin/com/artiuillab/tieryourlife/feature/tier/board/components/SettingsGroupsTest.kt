package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** List settings in three groups: Publication, List, and Careful apart at the end with Delete list (walkthrough 26.11.2026). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SettingsGroupsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var deleted = 0
    private var returned = 0

    @Test
    fun theGroups_runPublication_thenList_thenCareful() {
        show(previewTierList.copy(publishedId = "p"))

        // Where each row starts in the whole column, not clipped to the window: the last rows are below it.
        val top = { tag: String -> composeRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y }
        assertTrue(top(TierDetailTestTags.UPDATE_PUBLISHED) < top(TierDetailTestTags.NEW_TIER_ROW))
        assertTrue(top(TierDetailTestTags.NEW_TIER_ROW) < top(TierDetailTestTags.SETTINGS_RETURN_ALL))
        assertTrue(top(TierDetailTestTags.SETTINGS_RETURN_ALL) < top(TierDetailTestTags.SETTINGS_TAKE_DOWN))
        assertTrue(top(TierDetailTestTags.SETTINGS_TAKE_DOWN) < top(TierDetailTestTags.SETTINGS_DELETE_LIST))
        composeRule.onNodeWithText("CAREFUL").assertExists()
    }

    @Test
    fun deleteList_asksFirst_andNamesTheList() {
        show(previewTierList)

        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_DELETE_LIST).performScrollTo().performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.DELETE_LIST_DIALOG).assertExists()
        composeRule.onNodeWithText("“${previewTierList.title}” moves to the trash. You can restore it from there.").assertExists()
        assertEquals(0, deleted)
        composeRule.onNodeWithTag(TierDetailTestTags.DELETE_LIST_DIALOG).captureRoboImage(
            "src/test/screenshots/settings_delete_list_dialog.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F)),
        )

        composeRule.onNodeWithTag(TierDetailTestTags.DELETE_LIST_CONFIRM).performClick()
        assertEquals(1, deleted)
        composeRule.onNodeWithTag(TierDetailTestTags.DELETE_LIST_DIALOG).assertDoesNotExist()
    }

    @Test
    fun deletingAPublishedList_saysItComesDownToo() {
        show(previewTierList.copy(publishedId = "p"))

        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_DELETE_LIST).performScrollTo().performClick()
        composeRule.onNodeWithText(
            "“${previewTierList.title}” moves to the trash. You can restore it from there. Its copy in Community comes down too.",
        ).assertExists()
    }

    @Test
    fun cancel_keepsTheList() {
        show(previewTierList)

        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_DELETE_LIST).performScrollTo().performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(0, deleted)
        composeRule.onNodeWithTag(TierDetailTestTags.DELETE_LIST_DIALOG).assertDoesNotExist()
    }

    @Test
    fun nothingPlaced_hasNothingToPutBack() {
        show(previewTierList.copy(tiers = previewTierList.tiers.map { if (it.isPool) it else it.copy(items = emptyList()) }))

        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_RETURN_ALL).performScrollTo().assertIsNotEnabled()
        assertEquals(0, returned)
    }

    // Moved from the bar's Display menu (walkthrough 26.11.2026): Display lives here now.
    @Test
    fun namesOnCards_isASwitchUnderDisplay() {
        val memory = InMemoryBoardLayoutMemory()
        composeRule.setContent {
            CompositionLocalProvider(LocalBoardLayoutMemory provides memory) {
                TierYourLifeTheme {
                    TierListSettingsScreenContent(list = previewTierList, onBack = {}, onAddTier = { _, _, _, _ -> }, onSetDisplayMode = {})
                }
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.NAMES_ON_CARDS).performScrollTo().assertIsOn().performClick().assertIsOff()
        assertFalse(memory.namesOnCards)
    }

    @Test
    fun theCarefulGroup_inTheDark() {
        show(previewTierList.copy(publishedId = "p"), dark = true)
        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_DELETE_LIST).performScrollTo()
        capture("settings_careful_dark")
    }

    private fun show(list: TierList, dark: Boolean = false) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TierListSettingsScreenContent(
                        list = list,
                        onBack = {},
                        onAddTier = { _, _, _, _ -> },
                        onSetDisplayMode = {},
                        signedIn = true,
                        onPublish = {},
                        onSeeInCommunity = {},
                        onShareLink = {},
                        onReturnAllToPool = { returned++ },
                        onDeleteList = { deleted++ },
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            "src/test/screenshots/$name.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F)),
        )
    }
}
