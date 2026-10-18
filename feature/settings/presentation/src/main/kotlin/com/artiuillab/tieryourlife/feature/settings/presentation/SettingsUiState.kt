package com.artiuillab.tieryourlife.feature.settings.presentation

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings

data class SettingsUiState(
    val account: Account = Account.Unknown,
    val credits: Int? = null,
    val trashCount: Int = 0,
    /**
     * Null for everyone who is not the reader; being turned away is how the
     * app finds out. Opens with the last answer, because it decides whether
     * a row exists.
     */
    val pendingReports: Int? = null,
    /** Null until it has been read, and always null for a guest: there is nothing to say. */
    val backupSettings: BackupSettings? = null,
)
