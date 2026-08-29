package com.artiuillab.tieryourlife.core.settings

interface AppPreferences {

    fun themeChoice(): ThemeChoice

    fun setThemeChoice(choice: ThemeChoice)

    fun languageTag(): String?

    fun setLanguageTag(tag: String?)

    /**
     * The balance the server last reported. Shown while a fresh one is fetched,
     * so the number does not appear a moment after the screen it belongs to.
     * The server stays the authority: nothing is spent against this.
     */
    fun lastKnownCredits(): Int?

    fun setLastKnownCredits(credits: Int?)
}
