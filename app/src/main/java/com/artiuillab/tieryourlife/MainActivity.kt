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

// AppCompatActivity, not ComponentActivity: below API 33 (minSdk here is 24),
// AppCompatDelegate.setApplicationLocales is a backport of the framework per-app-language
// API, and with Compose that backport only applies through an AppCompat activity context
// (docs/design-spec-home.md, section 7). The manifest theme is still a bare no-action-bar
// AppCompat theme — TierYourLifeTheme does all real theming in Compose.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Not a ViewModel: AppPreferences is domain-layer, plain-Kotlin, synchronous —
    // exactly the shape that doesn't need one (docs/design-spec-home.md, section 7).
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Applied before enableEdgeToEdge()/setContent below, so the very first frame
        // already draws in the stored language rather than drawing once and flipping.
        applyStoredLocale()
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            // Seeded from the persisted choice once; every later change updates this
            // state and persists in the same place (onThemeChoiceChange below), so the
            // two never drift apart.
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
            // Same seed-once-then-track pattern as themeChoice above, kept as its own
            // piece of state rather than derived from AppCompatDelegate.getApplicationLocales()
            // so the Settings row can show it without re-reading appcompat's state on
            // every recomposition.
            var languageTag by rememberSaveable { mutableStateOf(appPreferences.languageTag()) }
            val onLanguageTagChange: (String?) -> Unit = { tag ->
                languageTag = tag
                appPreferences.setLanguageTag(tag)
                applyLocale(tag)
            }
            val navController = rememberNavController()

            TierYourLifeTheme(darkTheme = darkTheme) {
                // No transitions between destinations. Navigation Compose fades the
                // outgoing screen out and the incoming one in by default, and on this
                // app that reads as a blink rather than as movement: every destination
                // here fills the screen edge to edge with a surface of the same colour,
                // so a cross-fade between two of them has nothing to describe — it just
                // dims the screen and brings it back. It is most obvious on the create
                // button, where the whole point is that the new list is already open.
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

    private fun applyStoredLocale() = applyLocale(appPreferences.languageTag())

    // Null means "follow the system" — an empty LocaleListCompat tells AppCompatDelegate
    // to drop any override, which is exactly the framework's own definition of that
    // state and matches AppPreferences.languageTag()'s contract.
    private fun applyLocale(tag: String?) {
        val locales = tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
