package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.core.ui.logFailures
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.tier.domain.export.TierListsExportStrings
import com.artiuillab.tieryourlife.feature.tier.domain.export.buildTierListsExport
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ExportedText(val text: String, val listCount: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TierRepository,
    private val accountRepository: AccountRepository,
    private val generationCredits: GenerationCredits,
    private val community: CommunityRepository,
    private val backup: BoardBackup,
    private val preferences: AppPreferences,
) : ViewModel() {

    val account: StateFlow<Account> = accountRepository.account
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Account.Unknown)

    // Null while generation is not offered: a balance nobody can spend is a puzzle.
    private val _credits = MutableStateFlow(
        generationCredits.lastKnown().takeIf { Features.GENERATION_OFFERED },
    )
    val credits: StateFlow<Int?> = _credits.asStateFlow()

    private val _trashCount = MutableStateFlow(preferences.lastKnownTrashCount())
    val trashCount: StateFlow<Int> = _trashCount.asStateFlow()

    /**
     * Null for everyone who is not the reader; being turned away is how the
     * app finds out. Opens with the last answer, because it decides whether
     * a row exists.
     */
    private val _pendingReports = MutableStateFlow(preferences.lastKnownPendingReports())
    val pendingReports: StateFlow<Int?> = _pendingReports.asStateFlow()

    private val _backupSettings = MutableStateFlow<BackupSettings?>(null)

    /** Null until it has been read, and always null for a guest: there is nothing to say. */
    val backupSettings: StateFlow<BackupSettings?> = _backupSettings.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    /**
     * Waits for the account rather than sampling it: on the way in it is
     * [Account.Unknown] for a frame, and asking then left the section out for
     * a signed-in person.
     */
    fun loadBackupSettings() {
        viewModelScope.launch {
            val settled = account.first { it !is Account.Unknown }
            _backupSettings.value = if (settled is Account.SignedIn) backup.settings() else null
        }
    }

    fun startBackingUp() {
        backup.start()
        loadBackupSettings()
    }

    /** Confirmed on the way off only: turning it off deletes the copy. */
    fun stopBackingUp() {
        viewModelScope.launch {
            backup.stopAndDelete()
            loadBackupSettings()
        }
    }

    fun setPicturesOnWifiOnly(wifiOnly: Boolean) {
        backup.setPicturesOnWifiOnly(wifiOnly)
        loadBackupSettings()
    }

    fun loadCredits() {
        if (!Features.GENERATION_OFFERED) return
        viewModelScope.launch {
            logFailures("Reading generation credits") {
                _credits.value = generationCredits.remaining()
            }
        }
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            // Being turned away is the ordinary answer for non-readers but looks
            // like the network being down, so the reason is logged and a
            // failure leaves the last answer standing.
            community.reports()
                .onFailure { Timber.i(it, "Not showing the report queue") }
                // Remembered before it is shown: the screen opens from what was remembered.
                .onSuccess { reports ->
                    preferences.setLastKnownPendingReports(reports.size)
                    _pendingReports.value = reports.size
                }
        }
    }

    fun loadTrashCount() {
        viewModelScope.launch {
            logFailures("Counting trashed entries") {
                val count = repository.getTrashEntries().size
                preferences.setLastKnownTrashCount(count)
                _trashCount.value = count
            }
        }
    }

    fun exportText(strings: TierListsExportStrings, onResult: (ExportedText) -> Unit) {
        viewModelScope.launch {
            var exported: ExportedText? = null
            messages.guard("Exporting lists") {
                val lists = repository.getAllTierLists()
                exported = ExportedText(
                    text = buildTierListsExport(lists, strings),
                    listCount = lists.size,
                )
            }
            exported?.let(onResult)
        }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
