package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.ViewModel
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** The boards, by uid, whose link to their original was hidden on this device. */
@HiltViewModel
class OriginalLinkViewModel @Inject constructor(private val preferences: AppPreferences) : ViewModel() {

    private val _hidden = MutableStateFlow(preferences.originalLinksHidden())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    fun hide(boardUid: String) {
        preferences.hideOriginalLink(boardUid)
        _hidden.value = preferences.originalLinksHidden()
    }
}
