package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.ViewModel
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** The boards, by uid, whose credit line was put away on this device. */
@HiltViewModel
class RankingCreditViewModel @Inject constructor(private val preferences: AppPreferences) : ViewModel() {

    private val _putAway = MutableStateFlow(preferences.creditsPutAway())
    val putAway: StateFlow<Set<String>> = _putAway.asStateFlow()

    fun putAway(boardUid: String) {
        preferences.putCreditAway(boardUid)
        _putAway.value = preferences.creditsPutAway()
    }
}
