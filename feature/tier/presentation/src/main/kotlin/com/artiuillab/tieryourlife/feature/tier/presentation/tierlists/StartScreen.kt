package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Someone with no lists of their own yet: Your lists would greet them with nothing, so the app opens on the community's lists. */
@HiltViewModel
class StartScreenViewModel @Inject constructor(repository: TierRepository) : ViewModel() {
    val newcomer: StateFlow<Boolean?> = flow {
        emit(runSuspendCatching { repository.getTierListSummaries().isEmpty() }.getOrDefault(false))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * Whether the app starts on the community rather than Your lists, decided once
 * as it opens and kept while it stays open; null for the moment it takes to ask.
 */
@Composable
fun rememberStartOnCommunity(viewModel: StartScreenViewModel = hiltViewModel()): Boolean? {
    val newcomer by viewModel.newcomer.collectAsStateWithLifecycle()
    var decided by rememberSaveable { mutableStateOf<Boolean?>(null) }
    if (decided == null && newcomer != null) decided = newcomer
    return decided
}
