package com.artiuillab.tieryourlife

import androidx.lifecycle.ViewModel
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ColourChoice
import com.artiuillab.tieryourlife.core.settings.ColourSetting
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AppUiState(
    val themeChoice: ThemeChoice,
    val languageTag: String?,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    colourChoice: ColourChoice,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppUiState(
            themeChoice = appPreferences.themeChoice(),
            languageTag = appPreferences.languageTag(),
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    /** Null for the default, lavender. */
    val colour: StateFlow<ColourSetting?> = colourChoice.current

    fun setThemeChoice(choice: ThemeChoice) {
        appPreferences.setThemeChoice(choice)
        _state.update { it.copy(themeChoice = choice) }
    }

    // Applying the locale remains an AppCompat side effect owned by MainActivity.
    fun setLanguageTag(tag: String?) {
        appPreferences.setLanguageTag(tag)
        _state.update { it.copy(languageTag = tag) }
    }
}
