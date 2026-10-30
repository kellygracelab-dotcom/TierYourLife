package com.artiuillab.tieryourlife.core.settings

interface AppPreferences {

    fun themeChoice(): ThemeChoice

    fun setThemeChoice(choice: ThemeChoice)

    fun languageTag(): String?

    fun setLanguageTag(tag: String?)

    /** Shown while a fresh balance is fetched. The server stays the authority. */
    fun lastKnownCredits(): Int?

    fun setLastKnownCredits(credits: Int?)

    /**
     * Null for everyone the queue never answered for. Whether the row exists is
     * the server's answer; without this the settings screen grows a row while
     * being looked at.
     */
    fun lastKnownPendingReports(): Int?

    fun setLastKnownPendingReports(reports: Int?)

    /** What the trash held last time it was counted. */
    fun lastKnownTrashCount(): Int

    fun setLastKnownTrashCount(count: Int)

    /** Bytes of pictures the account held when this phone last measured: a row that opens with a number, not a blank. */
    fun lastKnownStoredBytes(): Long

    fun setLastKnownStoredBytes(bytes: Long)

    /** Local: nobody is told. Undoable, hence the name kept beside the id. */
    fun hiddenListIds(): Set<String>

    fun hiddenLists(): List<HiddenEntry>

    fun hideList(publishedId: String, title: String)

    fun unhideList(publishedId: String)

    fun hiddenAuthorUids(): Set<String>

    fun hiddenAuthors(): List<HiddenEntry>

    fun hideAuthor(authorUid: String, name: String)

    fun unhideAuthor(authorUid: String)

    /** On by default; consulted only once signed in. Off has to mean the copy is gone, or the switch is a lie. */
    fun backUpBoards(): Boolean

    fun setBackUpBoards(backUp: Boolean)

    /** "Not now" means never again: a card that comes back gets swatted unread. */
    fun signInOfferAnswered(): Boolean

    fun markSignInOfferAnswered()

    /** Pictures cost data allowance, so they wait for Wi-Fi by default; boards are text and go regardless. */
    fun picturesOnWifiOnly(): Boolean

    fun setPicturesOnWifiOnly(wifiOnly: Boolean)

    /** Rows or pictures. A property of the screen, remembered all the same: somebody who chose pictures meant it. */
    fun boardsAsPictures(): Boolean

    fun setBoardsAsPictures(asPictures: Boolean)

    /** When a sync run last got through, or null while none ever has. */
    fun lastSyncedAtMs(): Long?

    fun setLastSyncedAtMs(atMs: Long?)

    /** By uid rather than one flag: a second conflict months later is news again. */
    fun conflictsSeen(): Set<String>

    fun markConflictSeen(listUid: String)

    /**
     * The account each published id on this phone was last seen in, by id. A
     * board can carry an id another account published, and only that account
     * can say it is gone.
     */
    fun publishedOwners(): Map<String, String> = emptyMap()

    fun setPublishedOwners(owners: Map<String, String>) = Unit

    /** The account the last check of what is published ran for: a different one now means somebody switched. */
    fun lastPublishingAccount(): String? = null

    fun setLastPublishingAccount(account: String?) = Unit

    /**
     * Whether the person folded the pool's side column (true), opened it
     * (false), or left it to the board (null). Kept on this device only: it is
     * about this screen's room, not about the boards.
     */
    fun poolColumnCollapsed(): Boolean? = null

    fun setPoolColumnCollapsed(collapsed: Boolean?) = Unit

    /** Lines of cards the pool docked under the rows shows on a tablet held upright. */
    fun poolDockRows(): Int = DEFAULT_POOL_DOCK_ROWS

    fun setPoolDockRows(rows: Int) = Unit
}

const val DEFAULT_POOL_DOCK_ROWS = 2
