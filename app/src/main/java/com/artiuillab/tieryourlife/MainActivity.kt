package com.artiuillab.tieryourlife

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity on purpose: below API 33, AppCompatDelegate.setApplicationLocales
// works only through an AppCompat activity. Switching to ComponentActivity compiles and
// works on new phones — and silently kills in-app language switching on API 24–32.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before setContent: the first frame must already draw in the stored language.
        applyStoredLocale()
        applyWindowBackground()
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var themeChoice by rememberSaveable { mutableStateOf(appPreferences.themeChoice()) }
            val darkTheme = when (themeChoice) {
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
                ThemeChoice.SYSTEM -> systemDarkTheme
            }
            val onThemeChoiceChange: (ThemeChoice) -> Unit = { choice ->
                themeChoice = choice
                appPreferences.setThemeChoice(choice)
            }
            var languageTag by rememberSaveable { mutableStateOf(appPreferences.languageTag()) }
            val onLanguageTagChange: (String?) -> Unit = { tag ->
                languageTag = tag
                appPreferences.setLanguageTag(tag)
                applyLocale(tag)
            }
            val navController = rememberNavController()

            TierYourLifeTheme(darkTheme = darkTheme) {
                // Transitions are off deliberately: every destination fills the screen
                // with the same surface colour, so the default cross-fade has nothing to
                // show — it reads as a blink. Restoring the defaults brings back exactly
                // the flicker this replaced.
                NavHost(
                    navController = navController,
                    startDestination = Route.TierLists,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable<Route.TierLists> {
                        TierListsScreen(
                            onTierListClick = { id -> navController.navigate(Route.TierDetail(id)) },
                            onSettingsClick = { navController.navigate(Route.Settings) },
                            onNewListCreated = { id ->
                                navController.navigate(Route.TierDetail(id, startInTitleEdit = true))
                            },
                        )
                    }
                    composable<Route.TierDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<Route.TierDetail>()
                        TierDetailScreen(
                            onBack = { navController.popBackStack() },
                            startInTitleEdit = route.startInTitleEdit,
                        )
                    }
                    composable<Route.Settings> {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onTrashClick = { navController.navigate(Route.Trash) },
                            themeChoice = themeChoice,
                            onThemeChoiceChange = onThemeChoiceChange,
                            languageTag = languageTag,
                            onLanguageTagChange = onLanguageTagChange,
                        )
                    }
                    composable<Route.Trash> {
                        TrashScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
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
