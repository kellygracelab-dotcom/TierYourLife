package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.BackupSection
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.StopBackingUpDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.readableSize
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one place somebody can turn the copy of their boards off, and the one
 * line that admits it has stopped working.
 */
@RunWith(AndroidJUnit4::class)
class BackupSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<Boolean>()
    private val pictureChanges = mutableListOf<Boolean>()

    @Test
    fun onAndWorking_saysNothingAboutBeingFine() {
        setSection(settings(on = true), stuckSince = null)

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_SWITCH).assertIsDisplayed()
        // No "backed up · 2 minutes ago" anywhere: a working state that
        // announces itself teaches people to come and check it.
        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_STUCK).assertDoesNotExist()
    }

    // The reverse of the same rule. Silence while nothing has gone up for a
    // week is the silence that costs somebody their boards.
    @Test
    fun stuck_saysSoInError() {
        setSection(settings(on = true), stuckSince = "3 days ago")

        composeRule.onNodeWithText(string(R.string.settings_backup_stuck, "3 days ago")).assertIsDisplayed()
    }

    @Test
    fun theSizeIsShownUnderThePicturesSwitch() {
        setSection(settings(on = true, storedBytes = 142L * 1024 * 1024), stuckSince = null)

        composeRule.onNodeWithText(
            string(R.string.settings_backup_pictures_sub, "142 MB"),
        ).assertIsDisplayed()
    }

    // With the copy off there is no size and no Wi-Fi question: both are about
    // something that is not happening.
    @Test
    fun switchedOff_hidesTheQuestionsThatNoLongerApply() {
        setSection(settings(on = false), stuckSince = null)

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_PICTURES_SWITCH).assertDoesNotExist()
    }

    @Test
    fun turningItOff_reachesTheCaller() {
        setSection(settings(on = true), stuckSince = null)

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_SWITCH).performClick()

        composeRule.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test
    fun theWifiQuestion_reachesTheCaller() {
        setSection(settings(on = true, picturesOnWifiOnly = true), stuckSince = null)

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_PICTURES_SWITCH).performClick()

        composeRule.runOnIdle { assertEquals(listOf(false), pictureChanges) }
    }

    // "Off" has to mean the copy is gone, so the dialog says so rather than
    // asking "are you sure".
    @Test
    fun theConfirmation_saysWhatOffMeans() {
        composeRule.setContent {
            TierYourLifeTheme {
                StopBackingUpDialog(onConfirm = {}, onDismiss = {})
            }
        }

        composeRule.onNodeWithText(string(R.string.settings_backup_off_body)).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_OFF_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun sizesReadTheWayPeopleSayThem() {
        assertEquals("142 MB", readableSize(142L * 1024 * 1024))
        assertEquals("512 KB", readableSize(512L * 1024))
        assertEquals("0 KB", readableSize(0))
    }

    private fun settings(
        on: Boolean,
        picturesOnWifiOnly: Boolean = true,
        storedBytes: Long = 0,
    ) = BackupSettings(
        on = on,
        picturesOnWifiOnly = picturesOnWifiOnly,
        storedBytes = storedBytes,
        lastSyncedAtMs = null,
    )

    private fun setSection(settings: BackupSettings, stuckSince: String?) {
        changes.clear()
        pictureChanges.clear()
        composeRule.setContent {
            TierYourLifeTheme {
                BackupSection(
                    settings = settings,
                    stuckSince = stuckSince,
                    onBackUpChange = { changes += it },
                    onPicturesOnWifiOnlyChange = { pictureChanges += it },
                )
            }
        }
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)
}
