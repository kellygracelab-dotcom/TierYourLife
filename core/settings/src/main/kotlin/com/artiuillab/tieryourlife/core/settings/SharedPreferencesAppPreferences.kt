package com.artiuillab.tieryourlife.core.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "tier_your_life_prefs"
private const val KEY_THEME_CHOICE = "theme_choice"
private const val KEY_LANGUAGE_TAG = "language_tag"
private const val KEY_LAST_KNOWN_CREDITS = "last_known_credits"
private const val KEY_HIDDEN_LISTS = "hidden_list_ids"
private const val KEY_HIDDEN_AUTHORS = "hidden_author_uids"
private const val KEY_BACK_UP_BOARDS = "back_up_boards"
private const val KEY_SIGN_IN_OFFER_ANSWERED = "sign_in_offer_answered"

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
        prefs.edit { putString(KEY_THEME_CHOICE, choice.name) }
    }

    override fun languageTag(): String? = prefs.getString(KEY_LANGUAGE_TAG, null)

    override fun setLanguageTag(tag: String?) {
        prefs.edit { putString(KEY_LANGUAGE_TAG, tag) }
    }

    override fun lastKnownCredits(): Int? =
        prefs.getInt(KEY_LAST_KNOWN_CREDITS, -1).takeIf { it >= 0 }

    override fun setLastKnownCredits(credits: Int?) {
        prefs.edit {
            if (credits == null) remove(KEY_LAST_KNOWN_CREDITS) else putInt(KEY_LAST_KNOWN_CREDITS, credits)
        }
    }

    override fun backUpBoards(): Boolean = prefs.getBoolean(KEY_BACK_UP_BOARDS, true)

    override fun setBackUpBoards(backUp: Boolean) {
        prefs.edit { putBoolean(KEY_BACK_UP_BOARDS, backUp) }
    }

    override fun signInOfferAnswered(): Boolean = prefs.getBoolean(KEY_SIGN_IN_OFFER_ANSWERED, false)

    override fun markSignInOfferAnswered() {
        prefs.edit { putBoolean(KEY_SIGN_IN_OFFER_ANSWERED, true) }
    }

    override fun hiddenListIds(): Set<String> = idsIn(KEY_HIDDEN_LISTS)

    override fun hiddenLists(): List<HiddenEntry> = entriesIn(KEY_HIDDEN_LISTS)

    override fun hideList(publishedId: String, title: String) {
        hide(KEY_HIDDEN_LISTS, publishedId, title)
    }

    override fun unhideList(publishedId: String) {
        unhide(KEY_HIDDEN_LISTS, publishedId)
    }

    override fun hiddenAuthorUids(): Set<String> = idsIn(KEY_HIDDEN_AUTHORS)

    override fun hiddenAuthors(): List<HiddenEntry> = entriesIn(KEY_HIDDEN_AUTHORS)

    override fun hideAuthor(authorUid: String, name: String) {
        hide(KEY_HIDDEN_AUTHORS, authorUid, name)
    }

    override fun unhideAuthor(authorUid: String) {
        unhide(KEY_HIDDEN_AUTHORS, authorUid)
    }

    private fun stored(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty()

    private fun idsIn(key: String): Set<String> = HiddenRecords.ids(stored(key))

    private fun entriesIn(key: String): List<HiddenEntry> = HiddenRecords.entries(stored(key))

    private fun hide(key: String, id: String, label: String) {
        prefs.edit { putStringSet(key, HiddenRecords.with(stored(key), id, label)) }
    }

    private fun unhide(key: String, id: String) {
        prefs.edit { putStringSet(key, HiddenRecords.without(stored(key), id)) }
    }
}
