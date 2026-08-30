package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    val account: StateFlow<Account> = accountRepository.account
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Account.Unknown)

    private val _credits = MutableStateFlow(generationCredits.lastKnown())
    val credits: StateFlow<Int?> = _credits.asStateFlow()

    private val _trashCount = MutableStateFlow(0)
    val trashCount: StateFlow<Int> = _trashCount.asStateFlow()

    /**
     * How many complaints are waiting, or null for everyone who is not the
     * person who reads them. Being turned away is how the app finds out.
     */
    private val _pendingReports = MutableStateFlow<Int?>(null)
    val pendingReports: StateFlow<Int?> = _pendingReports.asStateFlow()

    private val _backupSettings = MutableStateFlow<BackupSettings?>(null)

    /** Null until it has been read, and always null for a guest: there is nothing to say. */
    val backupSettings: StateFlow<BackupSettings?> = _backupSettings.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    fun loadBackupSettings() {
        viewModelScope.launch {
            _backupSettings.value = if (account.value is Account.SignedIn) backup.settings() else null
        }
    }

    fun startBackingUp() {
        backup.start()
        loadBackupSettings()
    }

    /**
     * Confirmed on the way off, never on the way on. Turning it on has nothing
     * to warn about; turning it off deletes the copy, and that is the promise
     * the word "off" is making.
     */
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
        viewModelScope.launch {
            logFailures("Reading generation credits") {
                _credits.value = generationCredits.remaining()
            }
        }
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            // Being turned away is the ordinary answer for everyone who does not
            // read reports, but it looks the same as the network being down, so
            // the reason is written down rather than swallowed.
            _pendingReports.value = community.reports()
                .onFailure { Timber.i(it, "Not showing the report queue") }
                .getOrNull()
                ?.size
        }
    }

    fun loadTrashCount() {
        viewModelScope.launch {
            logFailures("Counting trashed entries") {
                _trashCount.value = repository.getTrashEntries().size
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
