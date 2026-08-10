package com.artiuillab.tieryourlife

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity on purpose: below API 33, AppCompatDelegate.setApplicationLocales
// works only through an AppCompat activity. Switching to ComponentActivity compiles and
// works on new phones — and silently kills in-app language switching on API 24–32.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Read here, before the first frame, for the two things that cannot wait for
    // composition: the locale and the window background. Everything after setContent
    // reads the same values through AppViewModel.
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before setContent: the first frame must already draw in the stored language.
        applyStoredLocale()
        applyWindowBackground()
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            AppRoot(
                state = state,
                onThemeChoiceChange = viewModel::setThemeChoice,
                onLanguageTagChange = { tag ->
                    viewModel.setLanguageTag(tag)
                    applyLocale(tag)
                },
            )
        }
    }

    // Not simply applyLocale(languageTag()): with nothing stored, applying null would clear
    // an override the user may have set in Android's own per-app language settings.
    // Picking "Default" in our Settings still clears it — at the moment it is picked.
    private fun applyStoredLocale() {
        appPreferences.languageTag()?.let(::applyLocale)
    }

    private fun applyLocale(tag: String?) {
        val locales = tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    // Looks redundant next to themes.xml, but themes.xml follows the system night setting —
    // wrong once the user overrides the theme: a light phone running Dark would flash a
    // white window before Compose draws. The stored choice wins, before the first frame.
    private fun applyWindowBackground() {
        val color = when (appPreferences.themeChoice()) {
            ThemeChoice.LIGHT -> ContextCompat.getColor(this, R.color.window_background_light)
            ThemeChoice.DARK -> ContextCompat.getColor(this, R.color.window_background_dark)
            ThemeChoice.SYSTEM -> ContextCompat.getColor(this, R.color.window_background)
        }
        window.setBackgroundDrawable(color.toDrawable())
    }
}
