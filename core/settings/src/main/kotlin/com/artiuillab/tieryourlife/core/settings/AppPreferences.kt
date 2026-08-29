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

    /**
     * Community lists this phone has hidden, and authors it has hidden
     * entirely. Both are local: nobody is told they were hidden, and hiding is
     * not an accusation.
     */
    fun hiddenListIds(): Set<String>

    fun hideList(publishedId: String)

    fun hiddenAuthorUids(): Set<String>

    fun hideAuthor(authorUid: String)
}
