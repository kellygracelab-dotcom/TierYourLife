package com.artiuillab.tieryourlife.core.settings

import android.annotation.SuppressLint
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
private const val KEY_BOARDS_AS_PICTURES = "boards_as_pictures"
private const val KEY_SIGN_IN_OFFER_ANSWERED = "sign_in_offer_answered"
private const val KEY_PICTURES_ON_WIFI_ONLY = "pictures_on_wifi_only"
private const val KEY_COVER_IDS_LEARNED = "cover_ids_learned"
private const val KEY_LAST_SYNCED_AT = "last_synced_at"
private const val KEY_CONFLICTS_SEEN = "conflicts_seen"
private const val KEY_BOARD_REFUSAL = "board_refusal"
private const val KEY_ERASURE_UNDER_WAY = "erasure_under_way"
private const val KEY_LAST_SIGNED_IN = "last_signed_in"
private const val KEY_HELD_BOARDS = "held_boards"
private const val KEY_HELD_BOARDS_COPIED = "held_boards_copied"
private const val HELD_SEPARATOR = "\n"
private const val KEY_LAST_KNOWN_REPORTS = "last_known_reports"
private const val KEY_LAST_KNOWN_TRASH = "last_known_trash"
private const val KEY_LAST_KNOWN_STORED_BYTES = "last_known_stored_bytes"
private const val KEY_POOL_COLUMN_COLLAPSED = "poolColumnCollapsed"
private const val KEY_POOL_DOCK_ROWS = "poolDockRows"
private const val KEY_PUBLISHED_OWNERS = "published_owners"
private const val KEY_LAST_PUBLISHING_ACCOUNT = "last_publishing_account"
private const val KEY_ACCOUNT_ERASED_NOTICE = "account_erased_notice"

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

    override fun forgetThePerson() {
        prefs.edit {
            remove(KEY_HIDDEN_LISTS)
            remove(KEY_HIDDEN_AUTHORS)
            remove(KEY_LAST_SYNCED_AT)
            remove(KEY_LAST_KNOWN_STORED_BYTES)
            remove(KEY_LAST_KNOWN_CREDITS)
            remove(KEY_LAST_KNOWN_REPORTS)
            remove(KEY_LAST_KNOWN_TRASH)
            remove(KEY_CONFLICTS_SEEN)
            prefs.all.keys.filter { it.startsWith("$KEY_BOARD_REFUSAL:") }.forEach(::remove)
        }
    }

    override fun lastKnownCredits(): Int? =
        prefs.getInt(KEY_LAST_KNOWN_CREDITS, -1).takeIf { it >= 0 }

    override fun setLastKnownCredits(credits: Int?) {
        prefs.edit {
            if (credits == null) remove(KEY_LAST_KNOWN_CREDITS) else putInt(KEY_LAST_KNOWN_CREDITS, credits)
        }
    }

    override fun lastKnownPendingReports(): Int? =
        prefs.getInt(KEY_LAST_KNOWN_REPORTS, -1).takeIf { it >= 0 }

    override fun setLastKnownPendingReports(reports: Int?) {
        prefs.edit {
            if (reports == null) remove(KEY_LAST_KNOWN_REPORTS) else putInt(KEY_LAST_KNOWN_REPORTS, reports)
        }
    }

    override fun lastKnownTrashCount(): Int = prefs.getInt(KEY_LAST_KNOWN_TRASH, 0)

    override fun setLastKnownTrashCount(count: Int) {
        prefs.edit { putInt(KEY_LAST_KNOWN_TRASH, count) }
    }

    override fun lastKnownStoredBytes(): Long = prefs.getLong(KEY_LAST_KNOWN_STORED_BYTES, 0L)

    override fun setLastKnownStoredBytes(bytes: Long) {
        prefs.edit { putLong(KEY_LAST_KNOWN_STORED_BYTES, bytes) }
    }

    override fun poolColumnCollapsed(): Boolean? =
        if (prefs.contains(KEY_POOL_COLUMN_COLLAPSED)) prefs.getBoolean(KEY_POOL_COLUMN_COLLAPSED, false) else null

    override fun setPoolColumnCollapsed(collapsed: Boolean?) {
        prefs.edit { if (collapsed == null) remove(KEY_POOL_COLUMN_COLLAPSED) else putBoolean(KEY_POOL_COLUMN_COLLAPSED, collapsed) }
    }

    override fun publishedOwners(): Map<String, String> =
        entriesIn(KEY_PUBLISHED_OWNERS).associate { it.id to it.label }

    override fun setPublishedOwners(owners: Map<String, String>) {
        val records = owners.entries.fold(emptySet<String>()) { records, (id, account) -> HiddenRecords.with(records, id, account) }
        prefs.edit { putStringSet(KEY_PUBLISHED_OWNERS, records) }
    }

    override fun lastPublishingAccount(): String? = prefs.getString(KEY_LAST_PUBLISHING_ACCOUNT, null)

    override fun setLastPublishingAccount(account: String?) {
        prefs.edit { putString(KEY_LAST_PUBLISHING_ACCOUNT, account) }
    }

    override fun accountErasedNoticePending(): Boolean = prefs.getBoolean(KEY_ACCOUNT_ERASED_NOTICE, false)

    override fun setAccountErasedNoticePending(pending: Boolean) {
        prefs.edit { putBoolean(KEY_ACCOUNT_ERASED_NOTICE, pending) }
    }

    override fun poolDockRows(): Int = prefs.getInt(KEY_POOL_DOCK_ROWS, DEFAULT_POOL_DOCK_ROWS)

    override fun setPoolDockRows(rows: Int) {
        prefs.edit { putInt(KEY_POOL_DOCK_ROWS, rows) }
    }

    // Rows by default: they say how many cards are ranked and how many wait,
    // which pictures cannot, and that is the more useful thing to open on.
    override fun boardsAsPictures(): Boolean = prefs.getBoolean(KEY_BOARDS_AS_PICTURES, false)

    override fun setBoardsAsPictures(asPictures: Boolean) {
        prefs.edit { putBoolean(KEY_BOARDS_AS_PICTURES, asPictures) }
    }

    override fun backUpBoards(account: String): Boolean = prefs.getBoolean(backUpKey(account), true)

    override fun setBackUpBoards(account: String, backUp: Boolean) {
        prefs.edit { putBoolean(backUpKey(account), backUp) }
    }

    override fun settleBackUpChoice(account: String?) {
        if (!prefs.contains(KEY_BACK_UP_BOARDS)) return
        val choice = prefs.getBoolean(KEY_BACK_UP_BOARDS, true)
        prefs.edit {
            if (account != null && !prefs.contains(backUpKey(account))) putBoolean(backUpKey(account), choice)
            remove(KEY_BACK_UP_BOARDS)
        }
    }

    private fun backUpKey(account: String) = "$KEY_BACK_UP_BOARDS:$account"

    override fun coverIdsLearned(account: String): Boolean = prefs.getBoolean("$KEY_COVER_IDS_LEARNED:$account", false)

    override fun setCoverIdsLearned(account: String) {
        prefs.edit { putBoolean("$KEY_COVER_IDS_LEARNED:$account", true) }
    }

    override fun signInOfferAnswered(): Boolean = prefs.getBoolean(KEY_SIGN_IN_OFFER_ANSWERED, false)

    override fun markSignInOfferAnswered() {
        prefs.edit { putBoolean(KEY_SIGN_IN_OFFER_ANSWERED, true) }
    }

    override fun picturesOnWifiOnly(): Boolean = prefs.getBoolean(KEY_PICTURES_ON_WIFI_ONLY, true)

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) {
        prefs.edit { putBoolean(KEY_PICTURES_ON_WIFI_ONLY, wifiOnly) }
    }

    override fun lastSyncedAtMs(): Long? = prefs.getLong(KEY_LAST_SYNCED_AT, 0L).takeIf { it > 0L }

    override fun setLastSyncedAtMs(atMs: Long?) {
        prefs.edit {
            if (atMs == null) remove(KEY_LAST_SYNCED_AT) else putLong(KEY_LAST_SYNCED_AT, atMs)
        }
    }

    override fun boardRefusal(account: String): String? = prefs.getString("$KEY_BOARD_REFUSAL:$account", null)

    override fun setBoardRefusal(account: String, refusal: String?) {
        prefs.edit {
            if (refusal == null) remove("$KEY_BOARD_REFUSAL:$account") else putString("$KEY_BOARD_REFUSAL:$account", refusal)
        }
    }

    override fun erasureUnderWay(): String? = prefs.getString(KEY_ERASURE_UNDER_WAY, null)

    override fun setErasureUnderWay(mark: String?) {
        // Committed, not applied: the request this guards may be the last thing the process does.
        prefs.edit(commit = true) {
            if (mark == null) remove(KEY_ERASURE_UNDER_WAY) else putString(KEY_ERASURE_UNDER_WAY, mark)
        }
    }

    override fun lastSignedIn(): String? = prefs.getString(KEY_LAST_SIGNED_IN, null)

    override fun setLastSignedIn(account: String?) {
        // Committed: a sign-out by choice clears it just before the process may end.
        prefs.edit(commit = true) {
            if (account == null) remove(KEY_LAST_SIGNED_IN) else putString(KEY_LAST_SIGNED_IN, account)
        }
    }

    override fun heldBoards(): Map<String, Set<String>> = uidsByAccount(KEY_HELD_BOARDS)

    // Committed: written just before the boards are let go, and the only thing saying whose they were.
    override fun setHeldBoards(held: Map<String, Set<String>>): Boolean = commitUidsByAccount(KEY_HELD_BOARDS, held)

    override fun heldBoardsCopied(): Map<String, Set<String>> = uidsByAccount(KEY_HELD_BOARDS_COPIED)

    // Committed: written before the copies are made.
    override fun setHeldBoardsCopied(copied: Map<String, Set<String>>): Boolean = commitUidsByAccount(KEY_HELD_BOARDS_COPIED, copied)

    private fun uidsByAccount(key: String): Map<String, Set<String>> =
        stored(key)
            .mapNotNull { entry -> entry.split(HELD_SEPARATOR, limit = 2).takeIf { it.size == 2 } }
            .groupBy({ it[0] }, { it[1] })
            .mapValues { (_, uids) -> uids.toSet() }

    // The KTX edit {} drops what commit() answers, and here the answer decides whether boards are let go or copied.
    @SuppressLint("UseKtx")
    private fun commitUidsByAccount(key: String, uids: Map<String, Set<String>>): Boolean {
        val entries = uids.flatMapTo(mutableSetOf()) { (account, ofIt) -> ofIt.map { "$account$HELD_SEPARATOR$it" } }
        val editor = prefs.edit()
        if (entries.isEmpty()) editor.remove(key) else editor.putStringSet(key, entries)
        return editor.commit()
    }

    override fun conflictsSeen(): Set<String> = stored(KEY_CONFLICTS_SEEN)

    override fun markConflictSeen(listUid: String) {
        prefs.edit { putStringSet(KEY_CONFLICTS_SEEN, stored(KEY_CONFLICTS_SEEN) + listUid) }
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
