package com.artiuillab.tieryourlife.feature.tier.data.preferences

import android.content.Context
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "tier_your_life_prefs"
private const val KEY_THEME_CHOICE = "theme_choice"
private const val KEY_LANGUAGE_TAG = "language_tag"

// SharedPreferences is memory-cached by the platform after its first read, so reading
// themeChoice() on every call is cheap — no extra caching layer needed here.
@Singleton
class SharedPreferencesAppPreferences @Inject constructor(
    @ApplicationContext context: Context,
) : AppPreferences {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun themeChoice(): ThemeChoice {
        val stored = prefs.getString(KEY_THEME_CHOICE, null) ?: return ThemeChoice.SYSTEM
        // An unknown or missing value reads back as SYSTEM — covers both "never set"
        // and "set by a future version of this app to a value this build doesn't know".
        return runCatching { ThemeChoice.valueOf(stored) }.getOrDefault(ThemeChoice.SYSTEM)
    }

    override fun setThemeChoice(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME_CHOICE, choice.name).apply()
    }

    // Absent means "never chosen" — read back as null, same as explicitly following the
    // system, and distinct from every real tag ("uk", "pt-BR", ...) an option offers.
    override fun languageTag(): String? = prefs.getString(KEY_LANGUAGE_TAG, null)

    override fun setLanguageTag(tag: String?) {
        prefs.edit().putString(KEY_LANGUAGE_TAG, tag).apply()
    }
}
