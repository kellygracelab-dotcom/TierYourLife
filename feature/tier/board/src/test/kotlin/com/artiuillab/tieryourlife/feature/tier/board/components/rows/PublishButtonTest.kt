package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The bar's publish button: what it offers on each kind of board, and how it stands beside Play. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PublishButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val asked = mutableListOf<Publishing>()

    private val unplaced = previewTierList
    private val placed = previewTierList.copy(tiers = previewTierList.tiers.map { if (it.isPool) it.copy(items = emptyList()) else it })

    @Test
    fun whatIsOffered_followsTheBoard() {
        assertEquals(Publishing.Publish, publishingOf(unplaced, publishedIsBehind = false))
        assertEquals(Publishing.ShareRanking, publishingOf(unplaced.copy(takenFrom = "source"), publishedIsBehind = false))
        assertEquals(Publishing.Update, publishingOf(unplaced.copy(publishedId = "p"), publishedIsBehind = true))
        assertEquals(Publishing.Update, publishingOf(unplaced.copy(takenFrom = "source", rankingOf = "source"), publishedIsBehind = true))
        assertNull(publishingOf(unplaced.copy(publishedId = "p"), publishedIsBehind = false))
        assertNull(publishingOf(unplaced.copy(tiers = unplaced.tiers.map { it.copy(items = emptyList()) }), publishedIsBehind = false))
        assertFalse(everyCardPlaced(unplaced))
        assertTrue(everyCardPlaced(placed))
    }

    @Test
    fun ownUnpublished_withCardsLeft_isTonal_andSaysPublishEvenOnAPhone() {
        show(unplaced)
        wordShown("Publish")
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Publish").performClick()
        assertEquals(listOf(Publishing.Publish), asked)
        capture("board_publish_cards_left")
    }

    @Test
    fun ownUnpublished_everyCardPlaced_isTheFilledButton() {
        show(placed)
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Publish").performClick()
        assertEquals(listOf(Publishing.Publish), asked)
        capture("board_publish_placed")
    }

    @Test
    fun publishedAndChanged_offersUpdate() {
        show(placed.copy(publishedId = "p"), behind = true)
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Update").performClick()
        assertEquals(listOf(Publishing.Update), asked)
        capture("board_publish_update")
    }

    // Danylo 16.11: a long publish showed nothing. Now the button turns, says so, and takes no second tap.
    @Test
    fun whileItGoes_theButtonTurns_saysSo_andTakesNoSecondTap() {
        show(placed, busy = true)
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Publishing…").assertIsNotEnabled().performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH_PROGRESS, useUnmergedTree = true).assertExists()
        assertTrue(asked.isEmpty())
        capture("board_publish_busy")
    }

    @Test
    fun anUpdateGoing_saysUpdating() {
        show(placed.copy(publishedId = "p"), behind = true, busy = true)
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Updating…")
    }

    @Test
    fun publishedAndUnchanged_hasNoButton_andThePhoneKeepsItsCamera() {
        show(placed.copy(publishedId = "p"))
        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.PUBLISH)).fetchSemanticsNodes().size)
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ADD_BUTTON).assertExists()
    }

    @Test
    fun onAPhone_theCameraGivesPublishItsRoom() {
        show(placed)
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ADD_BUTTON).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w720dp-h820dp-xhdpi")
    fun anOpenFlip_keepsTheCameraBesidePublish() {
        show(placed)
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ADD_BUTTON).assertExists()
    }

    @Test
    fun aCopyOfSomeoneElsesList_offersToShareTheRanking() {
        show(placed.copy(takenFrom = "source", authorName = "Ann"))
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Show in Rankings").performClick()
        assertEquals(listOf(Publishing.ShareRanking), asked)
        capture("board_publish_ranking")
    }

    @Test
    @Config(qualifiers = "+ru")
    fun theLongestLabel_stillLeavesTheTitleARoom() {
        show(placed.copy(takenFrom = "source", authorName = "Ann"))
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals("Показать в рейтингах")
        capture("board_publish_ranking_ru")
    }

    @Test
    @Config(qualifiers = "+ru-w360dp-h780dp")
    fun aNarrowPhone_inRussian_saysBothWords() {
        show(unplaced)
        wordShown("Опубликовать")
        capture("board_publish_narrow_ru")
    }

    @Test
    @Config(qualifiers = "+ru-w360dp-h780dp")
    fun aNarrowPhone_inRussian_showInRankings() {
        show(placed.copy(takenFrom = "source", authorName = "Ann"))
        wordShown("Показать в рейтингах")
        capture("board_publish_narrow_ranking_ru")
    }

    @Test
    @Config(qualifiers = "+de-w360dp-h780dp")
    fun aNarrowPhone_inGerman() {
        show(placed)
        wordShown("Veröffentlichen")
        capture("board_publish_narrow_de")
    }

    @Test
    @Config(qualifiers = "+de")
    fun theLongestWordToPublish_fitsOnAPhone() {
        show(placed)
        capture("board_publish_placed_de")
    }

    @Test
    fun dark_everyCardPlaced() {
        show(placed, dark = true)
        capture("board_publish_placed_dark")
    }

    @Test
    @Config(qualifiers = "w720dp-h820dp-xhdpi")
    fun anOpenFlip_saysItInWords_evenWithCardsLeft() {
        show(unplaced)
        capture("board_publish_open_flip")
    }

    @Test
    @Config(qualifiers = "w720dp-h820dp-xhdpi")
    fun anOpenFlip_dark_everyCardPlaced() {
        show(placed, dark = true)
        capture("board_publish_open_flip_dark")
    }

    @Test
    fun aBoardThatIsNotOnesOwn_hasNoButton() {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(state = TierDetailUiState.Success(placed), actions = TierDetailActions(onPublish = { asked += it }), readOnly = true)
            }
        }
        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.PUBLISH)).fetchSemanticsNodes().size)
    }

    private fun show(list: TierList, behind: Boolean = false, dark: Boolean = false, busy: Boolean = false) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    actions = TierDetailActions(onPlay = {}, publishing = busy, publishedIsBehind = behind, onPublish = { asked += it }),
                )
            }
        }
    }

    /** The word is drawn beside the icon: the button is as wide as a word, and says it to those who listen. */
    private fun wordShown(word: String) {
        composeRule.onNodeWithTag(TierDetailTestTags.PUBLISH).assertContentDescriptionEquals(word).assertWidthIsAtLeast(80.dp)
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            "src/test/screenshots/$name.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F)),
        )
    }
}
