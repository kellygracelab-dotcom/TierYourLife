package com.artiuillab.tieryourlife.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.core.theme.messages.logFailures
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.lists.TierListsExportStrings
import com.artiuillab.tieryourlife.feature.tier.domain.lists.buildTierListsExport
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private val _state = MutableStateFlow(
        SettingsUiState(
            // Null while generation is not offered: a balance nobody can spend is a puzzle.
            credits = generationCredits.lastKnown().takeIf { Features.GENERATION_OFFERED },
            trashCount = preferences.lastKnownTrashCount(),
            pendingReports = preferences.lastKnownPendingReports(),
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var credits: Int?
        get() = _state.value.credits
        set(value) = _state.update { it.copy(credits = value) }
    private var trashCount: Int
        get() = _state.value.trashCount
        set(value) = _state.update { it.copy(trashCount = value) }
    private var pendingReports: Int?
        get() = _state.value.pendingReports
        set(value) = _state.update { it.copy(pendingReports = value) }
    private var backupSettings: BackupSettings?
        get() = _state.value.backupSettings
        set(value) = _state.update { it.copy(backupSettings = value) }

    init {
        viewModelScope.launch {
            accountRepository.account.collect { account -> _state.update { it.copy(account = account) } }
        }
    }

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    /**
     * Waits for the account rather than sampling it: on the way in it is
     * [Account.Unknown] for a frame, and asking then left the section out for
     * a signed-in person.
     */
    fun loadBackupSettings() {
        viewModelScope.launch {
            val settled = _state.map { it.account }.first { it !is Account.Unknown }
            backupSettings = if (settled is Account.SignedIn) backup.settings() else null
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
                credits = generationCredits.remaining()
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
                    pendingReports = reports.size
                }
        }
    }

    fun loadTrashCount() {
        viewModelScope.launch {
            logFailures("Counting trashed entries") {
                val count = repository.getTrashEntries().size
                preferences.setLastKnownTrashCount(count)
                trashCount = count
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
