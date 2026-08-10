package com.artiuillab.tieryourlife.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "tier_your_life_prefs"
private const val KEY_THEME_CHOICE = "theme_choice"
private const val KEY_LANGUAGE_TAG = "language_tag"

@Singleton
class SharedPreferencesAppPreferences @Inject constructor(
    @ApplicationContext context: Context,
) : AppPreferences {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun themeChoice(): ThemeChoice {
        val stored = prefs.getString(KEY_THEME_CHOICE, null) ?: return ThemeChoice.SYSTEM
        return runCatching { ThemeChoice.valueOf(stored) }.getOrDefault(ThemeChoice.SYSTEM)
    }

    override fun setThemeChoice(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME_CHOICE, choice.name).apply()
    }

    override fun languageTag(): String? = prefs.getString(KEY_LANGUAGE_TAG, null)

    override fun setLanguageTag(tag: String?) {
        prefs.edit().putString(KEY_LANGUAGE_TAG, tag).apply()
    }
}
