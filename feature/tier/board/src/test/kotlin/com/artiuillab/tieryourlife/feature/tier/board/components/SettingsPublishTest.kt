package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.Publishing
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings publish with the bar's button and words: one tap, then a status with Take down beside it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SettingsPublishTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val published = mutableListOf<Publishing>()
    private val setPublic = mutableListOf<Boolean>()

    @Test
    fun ownUnpublished_theButtonPublishesAtOnce() {
        show(previewTierList)
        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_PUBLISH).performClick()
        assertEquals(listOf(Publishing.Publish), published)
        capture("settings_publish_own")
    }

    @Test
    fun aCopy_offersToShowItInRankings() {
        show(previewTierList.copy(takenFrom = "source", authorName = "Ann"))
        composeRule.onNodeWithText("Show in Rankings").performClick()
        assertEquals(listOf(Publishing.ShareRanking), published)
        capture("settings_publish_ranking")
    }

    @Test
    fun published_saysSo_andTakeDownIsBesideIt() {
        show(previewTierList.copy(publishedId = "p"))
        composeRule.onNodeWithText("Published ✓").assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_PUBLISH).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.SETTINGS_TAKE_DOWN).performClick()
        assertEquals(listOf(false), setPublic)
        capture("settings_published")
    }

    @Test
    fun publishedAndChanged_offersUpdate_theSameAsTheBar() {
        show(previewTierList.copy(publishedId = "p"), behind = true, dark = true)
        composeRule.onNodeWithTag(TierDetailTestTags.UPDATE_PUBLISHED).performClick()
        assertEquals(listOf(Publishing.Update), published)
        capture("settings_published_update_dark")
    }

    @Test
    @Config(qualifiers = "+ru")
    fun inRankings_inRussian() {
        show(previewTierList.copy(takenFrom = "source", rankingOf = "source", publishedId = "p", authorName = "Ann"))
        composeRule.onNodeWithText("В рейтингах ✓").assertExists()
        capture("settings_ranked_ru")
    }

    private fun show(list: TierList, behind: Boolean = false, dark: Boolean = false) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TierListSettingsScreenContent(
                    list = list,
                    onBack = {},
                    onAddTier = { _, _, _, _ -> },
                    onSetDisplayMode = {},
                    signedIn = true,
                    publishedIsBehind = behind,
                    onSetPublic = { setPublic += it },
                    onPublish = { published += it },
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
