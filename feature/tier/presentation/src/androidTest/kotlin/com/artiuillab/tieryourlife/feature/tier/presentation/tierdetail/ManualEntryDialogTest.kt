package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ManualEntryDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ManualEntryDialogContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import androidx.compose.ui.test.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ManualEntryDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingWithATitleOnly_invokesOnSaveWithTitleAndNoPhoto() {
        var savedTitle: String? = null
        var savedPhoto: String? = "not yet called"
        composeRule.setContent {
            TierYourLifeTheme {
                ManualEntryDialog(
                    onDismiss = {},
                    onSave = { title, photoUris ->
                        savedTitle = title
                        savedPhoto = photoUris.firstOrNull()
                    },
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_NAME_FIELD).performTextInput("Dune")
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("Dune", savedTitle)
            assertNull(savedPhoto)
        }
    }

    @Test
    fun emptyTitleAndNoPhoto_saveIsUnavailable() {
        composeRule.setContent {
            TierYourLifeTheme {
                ManualEntryDialog(onDismiss = {}, onSave = { _, _ -> })
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_SAVE).assertIsNotEnabled()
    }

    @Test
    fun emptyTitle_withAPhotoChosen_saveIsAvailable_andSavesWithABlankTitle() {
        var savedTitle: String? = "not yet called"
        var savedPhoto: String? = null
        composeRule.setContent {
            var photoUris by remember { mutableStateOf(emptyList<String>()) }
            TierYourLifeTheme {
                ManualEntryDialogContent(
                    title = "",
                    onTitleChange = {},
                    photoUris = photoUris,
                    onChoosePhotoClick = { photoUris = listOf("content://gallery/42") },
                    onRemovePhotos = { photoUris = emptyList() },
                    onDismiss = {},
                    onSave = {
                        savedTitle = ""
                        savedPhoto = photoUris.firstOrNull()
                    },
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_SAVE).assertIsNotEnabled()

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_CHOOSE_PHOTO).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_SAVE).assertIsEnabled()
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_SAVE).performClick()

        composeRule.runOnIdle {
            assertEquals("", savedTitle)
            assertEquals("content://gallery/42", savedPhoto)
        }
    }

    @Test
    fun photoPreview_isVisibleBeforeSaving_andCanBeRemoved() {
        composeRule.setContent {
            var photoUris by remember { mutableStateOf(emptyList<String>()) }
            TierYourLifeTheme {
                ManualEntryDialogContent(
                    title = "Dune",
                    onTitleChange = {},
                    photoUris = photoUris,
                    onChoosePhotoClick = { photoUris = listOf("content://gallery/42") },
                    onRemovePhotos = { photoUris = emptyList() },
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_REMOVE_PHOTO).assertDoesNotExist()

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_CHOOSE_PHOTO).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_PHOTO_FRAME).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_REMOVE_PHOTO).assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_REMOVE_PHOTO).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_REMOVE_PHOTO).assertDoesNotExist()
    }

    @Test
    fun severalPhotos_replaceTheNameFieldWithACount() {
        composeRule.setContent {
            TierYourLifeTheme {
                ManualEntryDialogContent(
                    title = "",
                    onTitleChange = {},
                    photoUris = listOf("content://gallery/1", "content://gallery/2"),
                    onChoosePhotoClick = {},
                    onRemovePhotos = {},
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ENTRY_NAME_FIELD).assertDoesNotExist()
        composeRule.onNodeWithText(
            plural(R.plurals.manual_dialog_photos_chosen, 2),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(plural(R.plurals.manual_dialog_add_many, 2)).assertIsDisplayed()
    }

    private fun plural(resourceId: Int, count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(resourceId, count, count)
}
