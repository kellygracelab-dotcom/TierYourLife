package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.FaceSheet
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** "Your face" (FACE-PHOTO-2026-11-19): Letter · Your photo · Upload · cards, the check on the face worn only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-port-xhdpi")
class FaceSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val taps = mutableListOf<String>()

    private fun open(current: String?, ownPhoto: String?, dark: Boolean = false) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                FaceSheet(
                    current = current,
                    ownPhoto = ownPhoto,
                    cardImages = listOf(CARD),
                    name = "Danylo",
                    onDismiss = {},
                    onChoose = { taps += "choose:$it" },
                    onPickPhoto = { taps += "upload" },
                    onWearOwnPhoto = { taps += "own" },
                    onRemoveOwnPhoto = { taps += "remove" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theLetterWorn_keepsThePhotosTile_unchecked_andATapWearsIt() {
        open(current = null, ownPhoto = OWN)

        composeRule.onNodeWithTag(AccountTestTags.FACE_LETTER).assertIsSelected()
        composeRule.onNodeWithTag(AccountTestTags.FACE_OWN_PHOTO).assertIsNotSelected()
        composeRule.onNodeWithTag(AccountTestTags.FACE_OWN_PHOTO).performClick()

        assertEquals(listOf("own"), taps)
    }

    @Test
    fun aCardWorn_isTheOnlyOneChecked() {
        open(current = CARD, ownPhoto = OWN)

        composeRule.onNodeWithTag(AccountTestTags.faceChoice(CARD)).assertIsSelected()
        composeRule.onNodeWithTag(AccountTestTags.FACE_LETTER).assertIsNotSelected()
        composeRule.onNodeWithTag(AccountTestTags.FACE_OWN_PHOTO).assertIsNotSelected()
    }

    @Test
    fun noPhotoYet_noTileForIt_andNothingToRemove() {
        open(current = null, ownPhoto = null)

        composeRule.onNodeWithTag(AccountTestTags.FACE_OWN_PHOTO).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountTestTags.FACE_REMOVE_PHOTO).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountTestTags.FACE_GALLERY).performClick()
        assertEquals(listOf("upload"), taps)
    }

    @Test
    fun removeYourPhoto_isOffered_whileThereIsOne() {
        open(current = OWN, ownPhoto = OWN)

        composeRule.onNodeWithTag(AccountTestTags.FACE_REMOVE_PHOTO).performClick()

        assertEquals(listOf("remove"), taps)
    }

    @Test
    fun sheet_photoWorn() = capture("face_sheet_photo_worn", current = OWN)

    @Test
    fun sheet_letterWorn_dark() = capture("face_sheet_letter_worn_dark", current = null, dark = true)

    @Test
    @Config(qualifiers = "en-w320dp-h640dp-port-mdpi")
    fun sheet_onTheSmallestPhone() = capture("face_sheet_320x640", current = null)

    @Test
    @Config(qualifiers = "ru-w320dp-h640dp-port-mdpi")
    fun sheet_russian_onTheSmallestPhone() = capture("face_sheet_ru_320x640", current = null)

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(name: String, current: String?, dark: Boolean = false) {
        open(current = current, ownPhoto = OWN, dark = dark)
        captureScreenRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private companion object {
        const val OWN = "https://example.test/avatars/own.jpg"
        const val CARD = "https://image.tmdb.org/t/p/w500/poster.jpg"
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
