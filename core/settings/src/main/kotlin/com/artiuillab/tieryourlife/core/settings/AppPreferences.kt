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
     * not an accusation. Both can be undone, which is why a name is kept
     * alongside the id.
     */
    fun hiddenListIds(): Set<String>

    fun hiddenLists(): List<HiddenEntry>

    fun hideList(publishedId: String, title: String)

    fun unhideList(publishedId: String)

    fun hiddenAuthorUids(): Set<String>

    fun hiddenAuthors(): List<HiddenEntry>

    fun hideAuthor(authorUid: String, name: String)

    fun unhideAuthor(authorUid: String)

    /**
     * Whether this account's boards are kept anywhere but this phone.
     *
     * On by default, and only ever consulted once somebody is signed in: a
     * guest has nowhere to keep them. Turning it off has to mean the copy is
     * gone, or the switch is a lie.
     */
    fun backUpBoards(): Boolean

    fun setBackUpBoards(backUp: Boolean)

    /**
     * Set once the offer to sign in has been made and answered. "Not now"
     * means never again rather than not this week -- a card that comes back is
     * a card somebody learns to swat, and the footer line says the same thing
     * quietly for as long as it stays true.
     */
    fun signInOfferAnswered(): Boolean

    fun markSignInOfferAnswered()

    /**
     * Pictures are the part of a board that costs somebody their data
     * allowance, so they wait for Wi-Fi by default. The boards themselves are
     * text and go whenever.
     */
    /**
     * Whether Your lists is drawn as pictures rather than as rows.
     *
     * A property of the screen rather than of the person, but remembered all
     * the same: somebody who chose pictures once meant it, and asking again on
     * every visit would be the app forgetting on purpose.
     */
    fun boardsAsPictures(): Boolean

    fun setBoardsAsPictures(asPictures: Boolean)

    fun picturesOnWifiOnly(): Boolean

    fun setPicturesOnWifiOnly(wifiOnly: Boolean)

    /** When a sync run last got through, or null while none ever has. */
    fun lastSyncedAtMs(): Long?

    fun setLastSyncedAtMs(atMs: Long?)

    /**
     * Boards whose "changed on two phones" notice has been read. Kept by uid
     * rather than as one flag, because a second conflict months later is news
     * again and deserves saying.
     */
    fun conflictsSeen(): Set<String>

    fun markConflictSeen(listUid: String)
}
