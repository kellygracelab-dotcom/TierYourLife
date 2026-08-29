package com.artiuillab.tieryourlife.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "tier_your_life_prefs"
private const val KEY_THEME_CHOICE = "theme_choice"
private const val KEY_LANGUAGE_TAG = "language_tag"
private const val KEY_LAST_KNOWN_CREDITS = "last_known_credits"
private const val KEY_HIDDEN_LISTS = "hidden_list_ids"
private const val KEY_HIDDEN_AUTHORS = "hidden_author_uids"

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

    override fun lastKnownCredits(): Int? =
        prefs.getInt(KEY_LAST_KNOWN_CREDITS, -1).takeIf { it >= 0 }

    override fun setLastKnownCredits(credits: Int?) {
        prefs.edit().apply {
            if (credits == null) remove(KEY_LAST_KNOWN_CREDITS) else putInt(KEY_LAST_KNOWN_CREDITS, credits)
        }.apply()
    }

    override fun hiddenListIds(): Set<String> = prefs.getStringSet(KEY_HIDDEN_LISTS, emptySet()).orEmpty()

    override fun hideList(publishedId: String) {
        prefs.edit().putStringSet(KEY_HIDDEN_LISTS, hiddenListIds() + publishedId).apply()
    }

    override fun hiddenAuthorUids(): Set<String> = prefs.getStringSet(KEY_HIDDEN_AUTHORS, emptySet()).orEmpty()

    override fun hideAuthor(authorUid: String) {
        prefs.edit().putStringSet(KEY_HIDDEN_AUTHORS, hiddenAuthorUids() + authorUid).apply()
    }
}
