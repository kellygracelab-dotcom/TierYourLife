package com.artiuillab.tieryourlife.feature.settings.presentation.components

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A board the account turns away is named with the reason, not blamed on the connection a day later. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class BackupRefusalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(settings: BackupSettings, stuckSince: String? = null) {
        composeRule.setContent {
            TierYourLifeTheme {
                BackupSection(settings = settings, stuckSince = stuckSince, onBackUpChange = {}, onPicturesOnWifiOnlyChange = {})
            }
        }
    }

    private fun settings(refusal: BackupRefusal?, on: Boolean = true) =
        BackupSettings(on = on, picturesOnWifiOnly = true, storedBytes = 0, lastSyncedAtMs = null, refusal = refusal)

    @Test
    fun aBoardTooLarge_isNamed_atOnce() {
        show(settings(BackupRefusal("Films", BackupRefusal.Reason.TooLarge)))

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_STUCK)
            .assertTextEquals("“Films” isn’t backed up: it’s bigger than the account keeps. Fewer tiers or cards will let it through.")
    }

    @Test
    fun aRefusal_isSaidInsteadOfBlamingTheConnection() {
        show(settings(BackupRefusal("Games", BackupRefusal.Reason.TooManyBoards)), stuckSince = "2 days ago")

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_STUCK)
            .assertTextEquals("“Games” isn’t backed up: the account keeps 200 boards at most. Delete the ones you no longer need.")
    }

    @Test
    fun withBackupOff_noRefusalIsShown() {
        show(settings(BackupRefusal("Films", BackupRefusal.Reason.NotAccepted), on = false))

        composeRule.onNodeWithTag(SettingsTestTags.BACKUP_STUCK).assertDoesNotExist()
    }
}
