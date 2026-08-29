package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.lifecycle.ViewModel
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class HiddenUiState(
    val lists: List<HiddenEntry> = emptyList(),
    val people: List<HiddenEntry> = emptyList(),
) {
    val isEmpty: Boolean get() = lists.isEmpty() && people.isEmpty()
}

@HiltViewModel
class HiddenViewModel @Inject constructor(
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(HiddenUiState())
    val state: StateFlow<HiddenUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = HiddenUiState(
            lists = preferences.hiddenLists(),
            people = preferences.hiddenAuthors(),
        )
    }

    fun showListAgain(publishedId: String) {
        preferences.unhideList(publishedId)
        load()
    }

    fun showAuthorAgain(authorUid: String) {
        preferences.unhideAuthor(authorUid)
        load()
    }
}
