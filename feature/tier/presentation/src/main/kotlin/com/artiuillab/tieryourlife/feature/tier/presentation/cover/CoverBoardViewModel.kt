package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** The one board open on the cover, read whole when it opens: the cover's lists are summaries without cards. */
@HiltViewModel
class CoverBoardViewModel @Inject constructor(private val repository: TierRepository) : ViewModel() {

    private val _board = MutableStateFlow<TierList?>(null)
    val board: StateFlow<TierList?> = _board.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            runSuspendCatching { repository.getTierListById(id) }
                .onSuccess { _board.value = it }
                .onFailure { Timber.w(it, "Reading the board open on the cover failed") }
        }
    }
}
