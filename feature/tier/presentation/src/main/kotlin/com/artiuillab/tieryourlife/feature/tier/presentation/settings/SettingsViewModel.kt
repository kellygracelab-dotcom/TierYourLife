package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.export.TierListsExportStrings
import com.artiuillab.tieryourlife.feature.tier.domain.export.buildTierListsExport
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportedText(val text: String, val listCount: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TierRepository,
) : ViewModel() {

    private val _trashCount = MutableStateFlow(0)
    val trashCount: StateFlow<Int> = _trashCount.asStateFlow()

    // Reload on every resume (see the Settings screen's own OnResumeEffect) so a trip to
    // Trash and back always shows the up-to-date count — the two screens don't share a
    // ViewModel, so this is the only way this row ever learns something changed there.
    fun loadTrashCount() {
        viewModelScope.launch {
            _trashCount.value = repository.getTrashEntries().size
        }
    }

    fun exportText(strings: TierListsExportStrings, onResult: (ExportedText) -> Unit) {
        viewModelScope.launch {
            val overviews = repository.getAllTierLists()
            val lists = overviews.map { overview -> repository.getTierListById(overview.id) ?: overview }
            onResult(ExportedText(text = buildTierListsExport(lists, strings), listCount = lists.size))
        }
    }
}
