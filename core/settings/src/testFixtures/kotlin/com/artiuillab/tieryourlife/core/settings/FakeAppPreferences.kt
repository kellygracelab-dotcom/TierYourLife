package com.artiuillab.tieryourlife.core.settings

class FakeAppPreferences(
    private var backUpBoards: Boolean = true,
    private var offerAnswered: Boolean = false,
    private var pendingReports: Int? = null,
    private var trashCount: Int = 0,
) : AppPreferences {
    private val hiddenLists = mutableMapOf<String, String>()
    private val hiddenAuthors = mutableMapOf<String, String>()

    override fun forgetThePerson() {
        hiddenLists.clear()
        hiddenAuthors.clear()
        refusals.clear()
    }

    override fun themeChoice(): ThemeChoice = ThemeChoice.SYSTEM
    override fun setThemeChoice(choice: ThemeChoice) = Unit
    override fun languageTag(): String? = null
    override fun setLanguageTag(tag: String?) = Unit
    override fun lastKnownCredits(): Int? = null
    override fun setLastKnownCredits(credits: Int?) = Unit

    override fun lastKnownPendingReports(): Int? = pendingReports
    override fun setLastKnownPendingReports(reports: Int?) {
        pendingReports = reports
    }

    override fun lastKnownTrashCount(): Int = trashCount
    override fun setLastKnownTrashCount(count: Int) {
        trashCount = count
    }

    override fun hiddenListIds(): Set<String> = hiddenLists.keys
    override fun hiddenLists(): List<HiddenEntry> = hiddenLists.map { HiddenEntry(it.key, it.value) }
    override fun hideList(publishedId: String, title: String) {
        hiddenLists[publishedId] = title
    }

    override fun unhideList(publishedId: String) {
        hiddenLists -= publishedId
    }

    override fun hiddenAuthorUids(): Set<String> = hiddenAuthors.keys
    override fun hiddenAuthors(): List<HiddenEntry> = hiddenAuthors.map { HiddenEntry(it.key, it.value) }
    override fun hideAuthor(authorUid: String, name: String) {
        hiddenAuthors[authorUid] = name
    }

    override fun unhideAuthor(authorUid: String) {
        hiddenAuthors -= authorUid
    }

    private var asPictures = false

    override fun boardsAsPictures(): Boolean = asPictures

    override fun setBoardsAsPictures(asPictures: Boolean) {
        this.asPictures = asPictures
    }

    /** Per account, as the real one; [backUpBoards] is what an account nobody has set yet reads. */
    private val backUpByAccount = mutableMapOf<String, Boolean>()

    override fun backUpBoards(account: String): Boolean = backUpByAccount[account] ?: backUpBoards

    override fun setBackUpBoards(account: String, backUp: Boolean) {
        backUpByAccount[account] = backUp
    }

    override fun signInOfferAnswered(): Boolean = offerAnswered

    override fun markSignInOfferAnswered() {
        offerAnswered = true
    }

    override fun picturesOnWifiOnly(): Boolean = true

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) = Unit

    override fun lastKnownStoredBytes(): Long = 0L

    override fun setLastKnownStoredBytes(bytes: Long) = Unit

    override fun lastSyncedAtMs(): Long? = null

    override fun setLastSyncedAtMs(atMs: Long?) = Unit

    private val refusals = mutableMapOf<String, String>()

    override fun boardRefusal(account: String): String? = refusals[account]

    private var erasure: String? = null

    override fun erasureUnderWay(): String? = erasure

    override fun setErasureUnderWay(mark: String?) {
        erasure = mark
    }

    private var signedInLast: String? = null

    override fun lastSignedIn(): String? = signedInLast

    override fun setLastSignedIn(account: String?) {
        signedInLast = account
    }

    private var held: Map<String, Set<String>> = emptyMap()

    override fun heldBoards(): Map<String, Set<String>> = held

    override fun setHeldBoards(held: Map<String, Set<String>>) {
        this.held = held.filterValues { it.isNotEmpty() }
    }

    override fun setBoardRefusal(account: String, refusal: String?) {
        if (refusal == null) refusals.remove(account) else refusals[account] = refusal
    }

    override fun conflictsSeen(): Set<String> = emptySet()

    override fun markConflictSeen(listUid: String) = Unit

    private var owners = emptyMap<String, String>()
    private var publishingAccount: String? = null

    override fun publishedOwners(): Map<String, String> = owners

    override fun setPublishedOwners(owners: Map<String, String>) {
        this.owners = owners
    }

    override fun lastPublishingAccount(): String? = publishingAccount

    override fun setLastPublishingAccount(account: String?) {
        publishingAccount = account
    }
}
