package com.artiuillab.tieryourlife

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// AppCompatActivity provides per-app locale support below API 33.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appPreferences: AppPreferences by lazy(LazyThreadSafetyMode.NONE) {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AppPreferencesEntryPoint::class.java,
        ).appPreferences()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyStoredLocale()
        super.onCreate(savedInstanceState)
        applyWindowBackground()
        enableEdgeToEdge()
        setContent {
            val containerSize = LocalWindowInfo.current.containerSize
            val compactBreakpointPx = with(LocalDensity.current) { COMPACT_BREAKPOINT.roundToPx() }
            val viewModel: AppViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Read from the window rather than from the device: a folding phone
            // is two different answers in one body, and only the window knows
            // which one is open.
            LaunchedEffect(containerSize, compactBreakpointPx) {
                requestedOrientation = orientationFor(
                    containerWidth = containerSize.width,
                    containerHeight = containerSize.height,
                    compactBreakpoint = compactBreakpointPx,
                )
            }

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
}

/** Material's compact window class: below this, one column is all there is. */
internal val COMPACT_BREAKPOINT = 600.dp

/**
 * Upright while the window is small, free once it is not.
 *
 * A board is a column of rows you drag things between. On a phone, turning it
 * sideways keeps the column and cuts the rows short, so there is nothing to
 * gain. A tablet is the other case entirely: the width is the point, and a
 * person holding one landscape should not be told to turn it.
 *
 * Either dimension being compact is enough, so a phone held sideways is still
 * asked to come back upright.
 */
internal fun orientationFor(
    containerWidth: Int,
    containerHeight: Int,
    compactBreakpoint: Int,
): Int =
    if (containerWidth < compactBreakpoint || containerHeight < compactBreakpoint) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
