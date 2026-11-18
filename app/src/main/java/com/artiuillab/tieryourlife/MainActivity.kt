package com.artiuillab.tieryourlife

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ReviewMoments
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import timber.log.Timber

// AppCompatActivity provides per-app locale support below API 33.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appPreferences: AppPreferences by lazy(LazyThreadSafetyMode.NONE) {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AppPreferencesEntryPoint::class.java,
        ).appPreferences()
    }

    private val reviewMoments: ReviewMoments by lazy(LazyThreadSafetyMode.NONE) {
        EntryPointAccessors.fromApplication(applicationContext, AppPreferencesEntryPoint::class.java).reviewMoments()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyStoredLocale()
        super.onCreate(savedInstanceState)
        applyWindowBackground()
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { reviewMoments.asks.collect { askForReview() } }
        }
        setContent {
            val viewModel: AppViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            val boardLayoutMemory = remember { PreferencesBoardLayoutMemory(appPreferences) }
            val tipMemory = remember { PreferencesTipMemory(appPreferences) }

            AppRoot(
                state = state,
                boardLayoutMemory = boardLayoutMemory,
                tipMemory = tipMemory,
                onThemeChoiceChange = viewModel::setThemeChoice,
                onLanguageTagChange = { tag ->
                    viewModel.setLanguageTag(tag)
                    applyLocale(tag)
                },
            )
        }
    }

    /** Play decides whether its card shows; nothing waits for it, and nothing is said when it does not. */
    private fun askForReview() {
        val manager = ReviewManagerFactory.create(this)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful && !isFinishing) {
                manager.launchReviewFlow(this, request.result)
            } else {
                Timber.i(request.exception, "Play's review card was not available")
            }
        }
    }

    // Do not clear a locale selected through Android settings when no value is stored.
    private fun applyStoredLocale() {
        appPreferences.languageTag()?.let(::applyLocale)
    }

    private fun applyLocale(tag: String?) {
        val locales = tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    // Prevents a flash when the in-app choice differs from the system theme.
    private fun applyWindowBackground() {
        val color = when (appPreferences.themeChoice()) {
            ThemeChoice.LIGHT -> ContextCompat.getColor(this, R.color.window_background_light)
            ThemeChoice.DARK -> ContextCompat.getColor(this, R.color.window_background_dark)
            ThemeChoice.SYSTEM -> ContextCompat.getColor(this, R.color.window_background)
        }
        window.setBackgroundDrawable(color.toDrawable())
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AppPreferencesEntryPoint {
    fun appPreferences(): AppPreferences

    fun reviewMoments(): ReviewMoments
}
